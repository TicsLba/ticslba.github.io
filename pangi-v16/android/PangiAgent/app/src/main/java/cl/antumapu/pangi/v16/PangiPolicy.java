package cl.antumapu.pangi.v16;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.os.UserManager;

import java.util.Collections;
import java.util.List;

final class PangiPolicy {
    static final String AFFILIATION = "pangi-v16-managed";
    private PangiPolicy(){}

    static DevicePolicyManager dpm(Context c){
        return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    static ComponentName admin(Context c){ return new ComponentName(c,PangiAdminReceiver.class); }
    static ComponentName home(Context c){ return new ComponentName(c,PangiHomeActivity.class); }

    static boolean isOwner(Context c){
        try{return dpm(c)!=null&&dpm(c).isDeviceOwnerApp(c.getPackageName());}
        catch(Exception e){return false;}
    }

    static boolean isProfileOwner(Context c){
        try{return dpm(c)!=null&&dpm(c).isProfileOwnerApp(c.getPackageName());}
        catch(Exception e){return false;}
    }

    static boolean isManaged(Context c){ return isOwner(c)||isProfileOwner(c); }

    static IntentFilter homeFilter(){
        IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);
        f.addCategory(Intent.CATEGORY_HOME);
        f.addCategory(Intent.CATEGORY_DEFAULT);
        return f;
    }

    static void pinHome(Context c){
        if(!isManaged(c))return;
        try{
            PackageManager pm=c.getPackageManager();
            pm.setComponentEnabledSetting(home(c),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            DevicePolicyManager d=dpm(c);
            ComponentName a=admin(c);
            try{d.clearPackagePersistentPreferredActivities(a,c.getPackageName());}catch(Exception ignored){}
            d.addPersistentPreferredActivity(a,homeFilter(),home(c));
            d.setLockTaskPackages(a,new String[]{c.getPackageName()});
        }catch(Exception ignored){}
    }

    static void releaseHome(Context c){
        if(!isManaged(c))return;
        try{
            DevicePolicyManager d=dpm(c);
            d.clearPackagePersistentPreferredActivities(admin(c),c.getPackageName());
            c.getPackageManager().setComponentEnabledSetting(home(c),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
        }catch(Exception ignored){}
    }

    static void startGate(Activity a){
        pinHome(a);
        try{ if(dpm(a).isLockTaskPermitted(a.getPackageName())) a.startLockTask(); }
        catch(Exception ignored){}
    }

    static void stopGate(Activity a){
        try{a.stopLockTask();}catch(Exception ignored){}
    }

    static void prepareOwnerGate(Context c){
        if(!isOwner(c))return;
        try{
            DevicePolicyManager d=dpm(c);
            ComponentName a=admin(c);
            d.setAffiliationIds(a,Collections.singleton(AFFILIATION));
            d.setUninstallBlocked(a,c.getPackageName(),true);
            restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,true);
            restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,true);
            restriction(d,a,UserManager.DISALLOW_ADD_USER,true);
            restriction(d,a,UserManager.DISALLOW_USER_SWITCH,true);
            pinHome(c);
        }catch(Exception ignored){}
    }

    static boolean prepareGuest(Context c){
        if(!isProfileOwner(c))return false;
        try{
            DevicePolicyManager d=dpm(c);
            ComponentName a=admin(c);
            d.setAffiliationIds(a,Collections.singleton(AFFILIATION));
            d.setUninstallBlocked(a,c.getPackageName(),true);
            d.setLockTaskPackages(a,new String[]{c.getPackageName()});
            restriction(d,a,UserManager.DISALLOW_INSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
            restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
            restriction(d,a,UserManager.DISALLOW_ADD_USER,true);
            restriction(d,a,UserManager.DISALLOW_USER_SWITCH,true);
            restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,true);
            restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,true);
            restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
            restriction(d,a,UserManager.DISALLOW_CONFIG_VPN,true);
            restriction(d,a,UserManager.DISALLOW_USB_FILE_TRANSFER,true);
            restriction(d,a,UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,true);

            // Las cuentas personales, Wi‑Fi, Bluetooth y el teclado permanecen utilizables.
            restriction(d,a,UserManager.DISALLOW_MODIFY_ACCOUNTS,false);
            restriction(d,a,UserManager.DISALLOW_CONFIG_WIFI,false);
            restriction(d,a,UserManager.DISALLOW_BLUETOOTH,false);

            // Las tiendas quedan fuera de los perfiles temporales.
            hideIfPresent(c,d,a,"com.android.vending");
            hideIfPresent(c,d,a,"com.sec.android.app.samsungapps");
            hideIfPresent(c,d,a,"com.huawei.appmarket");
            hideIfPresent(c,d,a,"com.xiaomi.mipicks");

            pinHome(c);
            return ensureIme(c);
        }catch(Exception e){return false;}
    }

    static boolean ensureIme(Context c){
        try{
            InputMethodManager imm=(InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm==null)return false;
            List<InputMethodInfo> enabled=imm.getEnabledInputMethodList();
            List<InputMethodInfo> all=imm.getInputMethodList();

            InputMethodInfo candidate=null;
            if(enabled!=null&&!enabled.isEmpty())candidate=enabled.get(0);
            if(candidate==null&&all!=null&&!all.isEmpty())candidate=all.get(0);
            if(candidate==null)return false;

            String pkg=candidate.getPackageName();
            try{dpm(c).setApplicationHidden(admin(c),pkg,false);}catch(Exception ignored){}
            try{dpm(c).setPackagesSuspended(admin(c),new String[]{pkg},false);}catch(Exception ignored){}

            String def=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            if(def==null||def.trim().isEmpty()){
                try{dpm(c).setSecureSetting(admin(c),Settings.Secure.DEFAULT_INPUT_METHOD,candidate.getId());}
                catch(Exception ignored){}
            }

            enabled=imm.getEnabledInputMethodList();
            return enabled!=null&&!enabled.isEmpty();
        }catch(Exception e){return false;}
    }

    static String imeSummary(Context c){
        try{
            InputMethodManager imm=(InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabled=imm==null?null:imm.getEnabledInputMethodList();
            String def=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            return "IME: "+(def==null||def.isEmpty()?"sin predeterminado":def)
                    +" · habilitados "+(enabled==null?0:enabled.size());
        }catch(Exception e){return "IME: diagnóstico no disponible";}
    }

    private static void hideIfPresent(Context c,DevicePolicyManager d,ComponentName a,String pkg){
        try{
            c.getPackageManager().getPackageInfo(pkg,0);
            try{d.setPackagesSuspended(a,new String[]{pkg},true);}catch(Exception ignored){}
            try{d.setApplicationHidden(a,pkg,true);}catch(Exception ignored){}
        }catch(Exception ignored){}
    }

    private static void restriction(DevicePolicyManager d,ComponentName a,String key,boolean on){
        try{if(on)d.addUserRestriction(a,key);else d.clearUserRestriction(a,key);}
        catch(Exception ignored){}
    }
}
