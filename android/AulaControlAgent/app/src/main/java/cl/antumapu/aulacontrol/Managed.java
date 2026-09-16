package cl.antumapu.aulacontrol;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import java.util.Collections;

final class Managed {
    private Managed(){}

    static ComponentName admin(Context c){return new ComponentName(c,AdminReceiver.class);}
    static ComponentName homeGuard(Context c){return new ComponentName(c,HomeGateActivity.class);}
    static DevicePolicyManager dpm(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);}
    static boolean owner(Context c){try{return dpm(c)!=null&&dpm(c).isDeviceOwnerApp(c.getPackageName());}catch(Exception e){return false;}}
    static boolean profileOwner(Context c){try{return dpm(c)!=null&&dpm(c).isProfileOwnerApp(c.getPackageName());}catch(Exception e){return false;}}
    static boolean managed(Context c){return owner(c)||profileOwner(c);}

    static void restriction(DevicePolicyManager d,ComponentName a,String key,boolean on){try{if(on)d.addUserRestriction(a,key);else d.clearUserRestriction(a,key);}catch(Exception ignored){}}
    static void grant(DevicePolicyManager d,ComponentName a,String pkg,String permission){try{d.setPermissionGrantState(a,pkg,permission,DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);}catch(Exception ignored){}}
    static void grantManagedPermissions(Context c,DevicePolicyManager d,ComponentName a,String self){
        grant(d,a,self,Manifest.permission.ACCESS_COARSE_LOCATION);
        grant(d,a,self,Manifest.permission.ACCESS_FINE_LOCATION);
        try{grant(d,a,self,Manifest.permission.PACKAGE_USAGE_STATS);}catch(Exception ignored){}
        if(Build.VERSION.SDK_INT>=29)grant(d,a,self,Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if(Build.VERSION.SDK_INT>=33)grant(d,a,self,Manifest.permission.POST_NOTIFICATIONS);
    }

    static IntentFilter homeFilter(){
        IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);
        f.addCategory(Intent.CATEGORY_HOME);
        f.addCategory(Intent.CATEGORY_DEFAULT);
        return f;
    }

    static void homeGuardOn(Context c){
        if(!owner(c)||Core.adminMode(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);ComponentName h=homeGuard(c);
            c.getPackageManager().setComponentEnabledSetting(h,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            try{d.clearPackagePersistentPreferredActivities(a,c.getPackageName());}catch(Exception ignored){}
            d.addPersistentPreferredActivity(a,homeFilter(),h);
        }catch(Exception ignored){}
    }

    static void homeGuardOff(Context c){
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
            if(d!=null&&managed(c))try{d.clearPackagePersistentPreferredActivities(a,c.getPackageName());}catch(Exception ignored){}
            c.getPackageManager().setComponentEnabledSetting(homeGuard(c),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
        }catch(Exception ignored){}
    }

    static void apply(Context c){if(owner(c)){if(Core.adminMode(c))adminUnlock(c);else applyOwner(c);}else if(profileOwner(c))applyGuest(c);}

    static void applyOwner(Context c){
        if(!owner(c)||Core.adminMode(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
            d.setAffiliationIds(a,Collections.singleton(SessionUsers.AFFILIATION));
            d.setUninstallBlocked(a,self,true);
            try{d.setLockTaskPackages(a,new String[]{self});}catch(Exception ignored){}
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,true);
            restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,true);
            restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
            if(Build.VERSION.SDK_INT>=30)try{d.setUserControlDisabledPackages(a,Collections.singletonList(self));}catch(Exception ignored){}
            grantManagedPermissions(c,d,a,self);
            homeGuardOn(c);
        }catch(Exception ignored){}
    }

    static void applyGuest(Context c){
        if(!profileOwner(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
            // The temporary user must always use the manufacturer's ordinary launcher.
            homeGuardOff(c);
            d.setAffiliationIds(a,Collections.singleton(SessionUsers.AFFILIATION));
            d.setUninstallBlocked(a,self,true);
            try{d.setLockTaskPackages(a,new String[]{self});}catch(Exception ignored){}
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
            restriction(d,a,UserManager.DISALLOW_ADD_USER,true);
            restriction(d,a,UserManager.DISALLOW_USER_SWITCH,true);
            restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,true);
            restriction(d,a,UserManager.DISALLOW_MODIFY_ACCOUNTS,false);
            restriction(d,a,UserManager.DISALLOW_CONFIG_WIFI,false);
            restriction(d,a,UserManager.DISALLOW_BLUETOOTH,false);
            grantManagedPermissions(c,d,a,self);
        }catch(Exception ignored){}
    }

    static void enterGate(Activity a){
        if(!managed(a)||Core.adminMode(a))return;
        try{
            DevicePolicyManager d=dpm(a);ComponentName c=admin(a);
            try{d.setLockTaskPackages(c,new String[]{a.getPackageName()});}catch(Exception ignored){}
            if(!d.isLockTaskPermitted(a.getPackageName()))return;
            a.startLockTask();
        }catch(Exception ignored){}
    }

    static void exitGate(Activity a){try{a.stopLockTask();}catch(Exception ignored){}}

    static void adminUnlock(Context c){
        if(!owner(c))return;
        try{
            DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
            homeGuardOff(c);
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,false);
            restriction(d,a,UserManager.DISALLOW_INSTALL_APPS,false);
            restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,false);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,false);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,false);
            restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,false);
            restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,false);
            d.setUninstallBlocked(a,c.getPackageName(),false);
            try{d.setLockTaskPackages(a,new String[]{});}catch(Exception ignored){}
            try{d.setStatusBarDisabled(a,false);}catch(Exception ignored){}
        }catch(Exception ignored){}
    }

    static void restoreProtection(Context c){Core.adminMode(c,false);applyOwner(c);}
    static void lockNow(Context c){try{if(managed(c))dpm(c).lockNow();}catch(Exception ignored){}}
    static void enter(Activity a){enterGate(a);}
    static void exit(Activity a){exitGate(a);if(owner(a))adminUnlock(a);}

    @SuppressWarnings("deprecation") static boolean release(Context c){
        if(!owner(c))return false;
        try{
            adminUnlock(c);DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
            try{d.setLockTaskPackages(a,new String[]{});}catch(Exception ignored){}
            try{d.clearPackagePersistentPreferredActivities(a,c.getPackageName());}catch(Exception ignored){}
            try{c.getPackageManager().setComponentEnabledSetting(homeGuard(c),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);}catch(Exception ignored){}
            d.clearDeviceOwnerApp(c.getPackageName());
            return true;
        }catch(Exception e){return false;}
    }
}
