package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.UserHandle;
import java.util.Collections;

final class SessionUsers {
    private SessionUsers(){}

    static boolean createAndSwitch(Context c,String role,String name,String course){
        if(!PolicyManager.owner(c))return false;
        try{
            cleanupSecondaryUsers(c);
            DevicePolicyManager d=PolicyManager.dpm(c);
            d.setAffiliationIds(PolicyManager.admin(c),Collections.singleton(PolicyManager.AFFILIATION));
            PersistableBundle extras=Store.provisioning(c,role,name,course);
            int flags=DevicePolicyManager.SKIP_SETUP_WIZARD|DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;
            if(Build.VERSION.SDK_INT>=28)flags|=DevicePolicyManager.MAKE_USER_EPHEMERAL;
            String label=Store.ROLE_TEACHER.equals(role)?"Tablet Escolar · Profesor":"Tablet Escolar · Estudiante";
            UserHandle u=d.createAndManageUser(PolicyManager.admin(c),label,PolicyManager.admin(c),extras,flags);
            if(u==null)return false;
            Store.state(c,Store.STATE_CREATING);
            return d.switchUser(PolicyManager.admin(c),u);
        }catch(Exception e){return false;}
    }

    static boolean logoutGuest(Context c){
        if(!PolicyManager.profileOwner(c))return false;
        try{
            Store.state(c,Store.STATE_CLOSING);
            DevicePolicyManager d=PolicyManager.dpm(c);
            if(Build.VERSION.SDK_INT>=28)return d.logoutUser(PolicyManager.admin(c))==android.os.UserManager.USER_OPERATION_SUCCESS;
            d.wipeData(0);
            return true;
        }catch(Exception e){return false;}
    }

    static void cleanupSecondaryUsers(Context c){
        if(!PolicyManager.owner(c)||Build.VERSION.SDK_INT<28)return;
        try{
            DevicePolicyManager d=PolicyManager.dpm(c);
            for(UserHandle u:d.getSecondaryUsers(PolicyManager.admin(c))){
                try{d.stopUser(PolicyManager.admin(c),u);}catch(Exception ignored){}
                try{d.removeUser(PolicyManager.admin(c),u);}catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }
}
