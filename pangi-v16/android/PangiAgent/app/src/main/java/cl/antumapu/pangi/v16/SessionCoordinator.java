package cl.antumapu.pangi.v16;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SessionCoordinator {
    static final class Result {
        final boolean ok;
        final String message;
        Result(boolean ok,String message){this.ok=ok;this.message=message;}
        static Result ok(){return new Result(true,"");}
        static Result fail(String m){return new Result(false,m);}
    }

    private SessionCoordinator(){}

    static Result createAndSwitch(Context c,String role,String name,String course){
        if(!PangiPolicy.isOwner(c))return Result.fail("PANGI no es Device Owner.");
        DevicePolicyManager d=PangiPolicy.dpm(c);
        ComponentName admin=PangiPolicy.admin(c);
        UserHandle user=null;
        try{
            cleanupSecondaryUsers(c);
            d.setAffiliationIds(admin,Collections.singleton(PangiPolicy.AFFILIATION));

            PersistableBundle extras=new PersistableBundle();
            extras.putString(PangiStore.K_ROLE,role);
            extras.putString(PangiStore.K_NAME,name);
            extras.putString(PangiStore.K_COURSE,course==null?"":course);
            extras.putString(PangiStore.K_TOKEN,UUID.randomUUID().toString());

            int flags=DevicePolicyManager.SKIP_SETUP_WIZARD
                    |DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED
                    |DevicePolicyManager.MAKE_USER_EPHEMERAL;

            String label=PangiStore.ROLE_TEACHER.equals(role)
                    ?"PANGI · Profesor":"PANGI · Estudiante";

            user=d.createAndManageUser(admin,label,admin,extras,flags);
            if(user==null)return Result.fail("Android no creó el usuario temporal.");

            int start=d.startUserInBackground(admin,user);
            if(start!=UserManager.USER_OPERATION_SUCCESS
                    && start!=UserManager.USER_OPERATION_ERROR_MAX_RUNNING_USERS){
                safeRemove(c,user);
                return Result.fail("Android no pudo preparar el usuario en segundo plano ("+start+").");
            }

            // A diferencia del proyecto anterior, no usamos un sleep fijo.
            // La sesión NO se muestra hasta que el Profile Owner del usuario nuevo
            // confirma mediante un bind DPM entre usuarios afiliados que HOME,
            // políticas e IME quedaron realmente listos.
            if(!awaitGuestReady(c,user,20000)){
                safeRemove(c,user);
                return Result.fail("El usuario temporal no confirmó HOME y teclado dentro del tiempo de seguridad.");
            }

            if(!d.switchUser(admin,user)){
                safeRemove(c,user);
                return Result.fail("Android preparó la sesión, pero rechazó el cambio de usuario.");
            }
            return Result.ok();
        }catch(Exception e){
            if(user!=null)safeRemove(c,user);
            String m=e.getMessage();
            return Result.fail("Fallo al preparar la sesión"+(m==null?"":": "+m));
        }
    }

    static void cleanupSecondaryUsers(Context c){
        if(!PangiPolicy.isOwner(c))return;
        try{
            DevicePolicyManager d=PangiPolicy.dpm(c);
            ComponentName a=PangiPolicy.admin(c);
            for(UserHandle u:d.getSecondaryUsers(a)){
                try{d.stopUser(a,u);}catch(Exception ignored){}
                try{d.removeUser(a,u);}catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }

    private static boolean awaitGuestReady(Context c,UserHandle user,long timeoutMs){
        long end=System.currentTimeMillis()+timeoutMs;
        while(System.currentTimeMillis()<end){
            CountDownLatch latch=new CountDownLatch(1);
            AtomicBoolean connected=new AtomicBoolean(false);
            ServiceConnection conn=new ServiceConnection(){
                @Override public void onServiceConnected(ComponentName name,IBinder service){
                    connected.set(service!=null);
                    latch.countDown();
                }
                @Override public void onServiceDisconnected(ComponentName name){ latch.countDown(); }
                @Override public void onNullBinding(ComponentName name){ latch.countDown(); }
                @Override public void onBindingDied(ComponentName name){ latch.countDown(); }
            };
            boolean requested=false;
            try{
                Intent i=new Intent().setComponent(new ComponentName(c,PangiReadyService.class));
                requested=PangiPolicy.dpm(c).bindDeviceAdminServiceAsUser(
                        PangiPolicy.admin(c),i,conn,Context.BIND_AUTO_CREATE,user);
                if(requested)latch.await(900,TimeUnit.MILLISECONDS);
            }catch(Exception ignored){}
            finally{
                if(requested)try{c.unbindService(conn);}catch(Exception ignored){}
            }
            if(connected.get())return true;
            try{Thread.sleep(350);}catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static void safeRemove(Context c,UserHandle u){
        try{
            DevicePolicyManager d=PangiPolicy.dpm(c);
            ComponentName a=PangiPolicy.admin(c);
            try{d.stopUser(a,u);}catch(Exception ignored){}
            try{d.removeUser(a,u);}catch(Exception ignored){}
        }catch(Exception ignored){}
    }
}
