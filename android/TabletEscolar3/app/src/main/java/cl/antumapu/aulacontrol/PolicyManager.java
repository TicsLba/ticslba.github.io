package cl.antumapu.aulacontrol;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import java.util.Collections;

final class PolicyManager {
    static final String AFFILIATION="tablet-escolar-v3-managed";
    private PolicyManager(){}

    static ComponentName admin(Context c){return new ComponentName(c,AdminReceiver.class);}
    static DevicePolicyManager dpm(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);}
    static boolean owner(Context c){try{return dpm(c).isDeviceOwnerApp(c.getPackageName());}catch(Exception e){return false;}}
    static boolean profileOwner(Context c){try{return dpm(c).isProfileOwnerApp(c.getPackageName());}catch(Exception e){return false;}}
    static boolean managed(Context c){return owner(c)||profileOwner(c);}

    private static void restriction(DevicePolicyManager d,ComponentName a,String key,boolean on){try{if(on)d.addUserRestriction(a,key);else d.clearUserRestriction(a,key);}catch(Exception ignored){}}
    private static void grant(DevicePolicyManager d,ComponentName a,String pkg,String permission){try{d.setPermissionGrantState(a,pkg,permission,DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);}catch(Exception ignored){}}
    private static void grantManagedPermissions(Context c,DevicePolicyManager d,ComponentName a){String p=c.getPackageName();grant(d,a,p,Manifest.permission.ACCESS_COARSE_LOCATION);grant(d,a,p,Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=29)grant(d,a,p,Manifest.permission.ACCESS_BACKGROUND_LOCATION);if(Build.VERSION.SDK_INT>=33)grant(d,a,p,Manifest.permission.POST_NOTIFICATIONS);}

    static void applyOwnerGate(Context c){
        if(!owner(c)||!Store.configured(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
            d.setAffiliationIds(a,Collections.singleton(AFFILIATION));
            d.setUninstallBlocked(a,self,true);
            try{d.setLockTaskPackages(a,new String[]{self});}catch(Exception ignored){}
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,true);
            restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,true);
            restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
            grantManagedPermissions(c,d,a);
        }catch(Exception ignored){}
    }

    static void applyGuestOnce(Context c){
        if(!profileOwner(c)||Store.guestPolicyApplied(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
            d.setAffiliationIds(a,Collections.singleton(AFFILIATION));
            d.setUninstallBlocked(a,self,true);
            try{d.setLockTaskPackages(a,new String[]{self});}catch(Exception ignored){}
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
            restriction(d,a,UserManager.DISALLOW_ADD_USER,true);
            restriction(d,a,UserManager.DISALLOW_USER_SWITCH,true);
            restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,true);
            restriction(d,a,UserManager.DISALLOW_MODIFY_ACCOUNTS,false);
            restriction(d,a,UserManager.DISALLOW_CONFIG_WIFI,false);
            restriction(d,a,UserManager.DISALLOW_BLUETOOTH,false);
            grantManagedPermissions(c,d,a);
            Store.guestPolicyApplied(c,true);
        }catch(Exception ignored){}
    }

    static void enterMaintenance(Context c){
        if(!owner(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,false);restriction(d,a,UserManager.DISALLOW_INSTALL_APPS,false);restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,false);restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,false);restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,false);restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,false);
            d.setUninstallBlocked(a,c.getPackageName(),false);try{d.setLockTaskPackages(a,new String[]{});}catch(Exception ignored){}
        }catch(Exception ignored){}
    }

    static void startGateLock(Activity a){if(!owner(a))return;applyOwnerGate(a);try{a.startLockTask();}catch(Exception ignored){}}
    static void startSessionLock(Activity a){if(!profileOwner(a))return;try{dpm(a).setLockTaskPackages(admin(a),new String[]{a.getPackageName()});a.startLockTask();}catch(Exception ignored){}}
    static void stopLock(Activity a){try{a.stopLockTask();}catch(Exception ignored){}}
    static void lockNow(Context c){try{if(managed(c))dpm(c).lockNow();}catch(Exception ignored){}}

    @SuppressWarnings("deprecation") static boolean releaseOwner(Context c){
        if(!owner(c))return false;try{enterMaintenance(c);dpm(c).clearDeviceOwnerApp(c.getPackageName());return true;}catch(Exception e){return false;}
    }
}
