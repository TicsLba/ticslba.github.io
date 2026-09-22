package cl.antumapu.pangi.v205;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SessionUsers {
    static final String AFFILIATION="pangi-v20-5-managed-session";
    private SessionUsers(){}

    static boolean createAndSwitch(Context c,String role,String name,String course){
        if(!Managed.owner(c)||Build.VERSION.SDK_INT<28)return false;
        UserHandle u=null;
        try{
            cleanupSecondaryUsers(c);
            DevicePolicyManager d=Managed.dpm(c);
            ComponentName a=Managed.admin(c);
            d.setAffiliationIds(a,Collections.singleton(AFFILIATION));

            PersistableBundle ex=new PersistableBundle();
            ex.putString(Core.K_ROLE,role);
            ex.putString(Core.K_NAME,name);
            ex.putString(Core.K_COURSE,course==null?"":course);
            ex.putString(Core.K_DEVICE_NAME,Core.dn(c));
            ex.putString(Core.K_DEVICE_ID,Core.id(c));
            ex.putString(Core.K_KEY,Core.key(c));
            ex.putString(Core.K_AFFILIATION,AFFILIATION);
            ex.putInt(Core.K_IDLE_STUDENT,Core.idleMinutes(c,Core.ROLE_STUDENT));
            ex.putInt(Core.K_IDLE_TEACHER,Core.idleMinutes(c,Core.ROLE_TEACHER));
            ex.putString(Core.K_APPS,AppPolicy.appsForRole(c,role));
            ex.putString(RelayPrefs.EXTRA,RelayPrefs.url(c));

            int flags=DevicePolicyManager.SKIP_SETUP_WIZARD
                    |DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED
                    |DevicePolicyManager.MAKE_USER_EPHEMERAL;

            String label=Core.ROLE_TEACHER.equals(role)?"PANGI · Profesor":"PANGI · Estudiante";
            u=d.createAndManageUser(a,label,a,ex,flags);
            if(u==null)return false;

            int start=d.startUserInBackground(a,u);
            if(start!=UserManager.USER_OPERATION_SUCCESS
                    && start!=UserManager.USER_OPERATION_ERROR_MAX_RUNNING_USERS){
                safeRemove(c,u);
                return false;
            }

            // PANGI no cambia al usuario hasta que el Profile Owner del usuario
            // temporal confirme HOME, políticas, apps e IME.
            if(!awaitReady(c,u,25_000L)){
                safeRemove(c,u);
                return false;
            }

            SessionState.ownerGate(c);
            if(!d.switchUser(a,u)){
                safeRemove(c,u);
                return false;
            }
            return true;
        }catch(Exception e){
            if(u!=null)safeRemove(c,u);
            return false;
        }
    }

    static boolean logoutGuest(Context c){
        if(!Managed.profileOwner(c))return false;
        try{
            SessionState.set(c,SessionState.State.CLOSING);
            int result=Managed.dpm(c).logoutUser(Managed.admin(c));
            return result==UserManager.USER_OPERATION_SUCCESS;
        }catch(Exception ignored){return false;}
    }

    static void cleanupSecondaryUsers(Context c){
        if(!Managed.owner(c)||Build.VERSION.SDK_INT<28)return;
        try{
            DevicePolicyManager d=Managed.dpm(c);
            for(UserHandle u:d.getSecondaryUsers(Managed.admin(c))){
                try{d.stopUser(Managed.admin(c),u);}catch(Exception ignored){}
                try{d.removeUser(Managed.admin(c),u);}catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }

    private static boolean awaitReady(Context c,UserHandle user,long timeoutMs){
        long end=System.currentTimeMillis()+timeoutMs;

        while(System.currentTimeMillis()<end){
            CountDownLatch latch=new CountDownLatch(1);
            AtomicBoolean ready=new AtomicBoolean(false);

            ServiceConnection conn=new ServiceConnection(){
                @Override public void onServiceConnected(ComponentName name,IBinder binder){
                    ready.set(binder!=null);
                    latch.countDown();
                }
                @Override public void onServiceDisconnected(ComponentName name){latch.countDown();}
                @Override public void onNullBinding(ComponentName name){latch.countDown();}
                @Override public void onBindingDied(ComponentName name){latch.countDown();}
            };

            boolean bound=false;
            try{
                Intent i=new Intent().setComponent(new ComponentName(c,PangiReadyService.class));
                bound=Managed.dpm(c).bindDeviceAdminServiceAsUser(
                        Managed.admin(c),i,conn,Context.BIND_AUTO_CREATE,user);
                if(bound)latch.await(1100,TimeUnit.MILLISECONDS);
            }catch(Exception ignored){
            }finally{
                if(bound)try{c.unbindService(conn);}catch(Exception ignored){}
            }

            if(ready.get())return true;

            try{Thread.sleep(300);}
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static void safeRemove(Context c,UserHandle u){
        try{
            DevicePolicyManager d=Managed.dpm(c);
            ComponentName a=Managed.admin(c);
            try{d.stopUser(a,u);}catch(Exception ignored){}
            try{d.removeUser(a,u);}catch(Exception ignored){}
        }catch(Exception ignored){}
    }
}
