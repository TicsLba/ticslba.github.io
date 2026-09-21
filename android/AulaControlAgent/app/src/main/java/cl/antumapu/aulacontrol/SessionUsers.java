package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.Collections;

final class SessionUsers {
    static final String AFFILIATION="tablet-escolar-managed-session-v3";
    private SessionUsers(){}

    static boolean createAndSwitch(Context c,String role,String name,String course){
        if(!Managed.owner(c))return false;
        try{
            cleanupSecondaryUsers(c);
            DevicePolicyManager d=Managed.dpm(c);
            d.setAffiliationIds(Managed.admin(c),Collections.singleton(AFFILIATION));

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

            int flags=DevicePolicyManager.SKIP_SETUP_WIZARD|DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;
            if(Build.VERSION.SDK_INT>=28)flags|=DevicePolicyManager.MAKE_USER_EPHEMERAL;

            String label=Core.ROLE_TEACHER.equals(role)?"Aula Móvil · Profesor":"Aula Móvil · Estudiante";
            UserHandle u=d.createAndManageUser(Managed.admin(c),label,Managed.admin(c),ex,flags);
            if(u==null)return false;

            // Start the managed user out of view first. This gives Android time to
            // finish profile-owner provisioning and lets AdminReceiver install the
            // temporary HOME gate before the user ever becomes visible.
            if(Build.VERSION.SDK_INT>=28){
                try{d.startUserInBackground(Managed.admin(c),u);}catch(Exception ignored){}
                try{Thread.sleep(1200);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
            }else{
                try{Thread.sleep(700);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
            }
            SessionState.ownerGate(c);
            return d.switchUser(Managed.admin(c),u);
        }catch(Exception e){return false;}
    }

    static boolean logoutGuest(Context c){
        if(!Managed.profileOwner(c))return false;
        try{
            SessionState.set(c,SessionState.State.CLOSING);
            DevicePolicyManager d=Managed.dpm(c);
            if(Build.VERSION.SDK_INT>=28){
                int result=d.logoutUser(Managed.admin(c));
                return result==UserManager.USER_OPERATION_SUCCESS;
            }
            // Android 8.x has no logoutUser for an affiliated managed user. Wiping from
            // its profile owner removes the temporary user's local data and user container.
            d.wipeData(0);
            return true;
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
}
