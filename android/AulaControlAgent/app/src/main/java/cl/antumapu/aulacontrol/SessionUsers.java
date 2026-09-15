package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.*;
import java.util.*;

final class SessionUsers {
    static final String AFFILIATION="aulacontrol-managed-guest-v1";
    private SessionUsers(){}

    static boolean createAndSwitch(Context c,String role,String name,String course){
        if(!Managed.owner(c))return false;
        try{
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

            int flags=DevicePolicyManager.SKIP_SETUP_WIZARD|DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;
            if(Build.VERSION.SDK_INT>=28)flags|=DevicePolicyManager.MAKE_USER_EPHEMERAL;

            String userLabel=Core.ROLE_TEACHER.equals(role)?"Profesor Temporal":"Estudiante Temporal";
            UserHandle u=d.createAndManageUser(Managed.admin(c),userLabel,Managed.admin(c),ex,flags);
            if(u==null)return false;
            return d.switchUser(Managed.admin(c),u);
        }catch(Exception e){return false;}
    }

    static boolean logoutGuest(Context c){
        if(!Managed.profileOwner(c))return false;
        try{
            DevicePolicyManager d=Managed.dpm(c);
            if(Build.VERSION.SDK_INT>=28){
                int result=d.logoutUser(Managed.admin(c));
                return result==UserManager.USER_OPERATION_SUCCESS;
            }

            // En Android 8 el profile owner del usuario secundario puede borrar
            // únicamente su propio usuario mediante wipeData(). Nunca ejecutamos
            // esta ruta desde el Device Owner del usuario principal.
            d.wipeData(0);
            return true;
        }catch(Exception ignored){return false;}
    }

    static void cleanupSecondaryUsers(Context c){
        if(!Managed.owner(c)||Build.VERSION.SDK_INT<28)return;
        try{
            DevicePolicyManager d=Managed.dpm(c);
            try{d.switchUser(Managed.admin(c),null);}catch(Exception ignored){}
            for(UserHandle u:d.getSecondaryUsers(Managed.admin(c))){
                try{d.stopUser(Managed.admin(c),u);}catch(Exception ignored){}
                try{d.removeUser(Managed.admin(c),u);}catch(Exception ignored){}
            }
        }catch(Exception ignored){}
    }
}
