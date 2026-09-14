package cl.antumapu.aulacontrol;

import android.Manifest;
import android.app.*;
import android.app.admin.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.provider.Settings;
import java.util.*;

final class Managed{
 private Managed(){}
 static ComponentName admin(Context c){return new ComponentName(c,AdminReceiver.class);} 
 static DevicePolicyManager dpm(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);} 
 static boolean owner(Context c){try{return dpm(c).isDeviceOwnerApp(c.getPackageName());}catch(Exception e){return false;}}
 static String[] allowed(Context c){LinkedHashSet<String> p=new LinkedHashSet<>();p.add(c.getPackageName());try{Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);for(ResolveInfo r:c.getPackageManager().queryIntentActivities(q,PackageManager.MATCH_ALL)){String x=r.activityInfo.packageName;if(x==null)continue;if(x.equals(c.getPackageName())||x.equals("com.android.settings")||x.equals("com.android.vending")||x.contains("packageinstaller")||x.contains("permissioncontroller"))continue;p.add(x);}}catch(Exception ignored){}return p.toArray(new String[0]);}
 static void apply(Context c){if(!owner(c))return;try{DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
   d.setUninstallBlocked(a,self,true);
   restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,true);
   restriction(d,a,UserManager.DISALLOW_ADD_USER,true);
   restriction(d,a,UserManager.DISALLOW_ADD_MANAGED_PROFILE,true);
   restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,true);
   restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,true);
   restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,true);
   restriction(d,a,UserManager.DISALLOW_MODIFY_ACCOUNTS,true);
   restriction(d,a,UserManager.DISALLOW_CONFIG_CREDENTIALS,true);
   restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,true);
   restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,true);
   d.setLockTaskPackages(a,allowed(c));
   if(Build.VERSION.SDK_INT>=28)d.setLockTaskFeatures(a,DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
   if(Build.VERSION.SDK_INT>=23)try{d.setStatusBarDisabled(a,true);}catch(Exception ignored){}
   if(Build.VERSION.SDK_INT>=30)try{d.setUserControlDisabledPackages(a,Collections.singletonList(self));}catch(Exception ignored){}
   if(Build.VERSION.SDK_INT>=33)try{d.setPermissionGrantState(a,self,Manifest.permission.POST_NOTIFICATIONS,DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);}catch(Exception ignored){}
   IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);f.addCategory(Intent.CATEGORY_HOME);f.addCategory(Intent.CATEGORY_DEFAULT);d.addPersistentPreferredActivity(a,f,new ComponentName(c,PrivacyGateActivity.class));
 }catch(Exception ignored){}}
 static void restriction(DevicePolicyManager d,ComponentName a,String key,boolean enable){try{if(enable)d.addUserRestriction(a,key);else d.clearUserRestriction(a,key);}catch(Exception ignored){}}
 static boolean isDefaultHome(Context c){try{Intent h=new Intent(Intent.ACTION_MAIN);h.addCategory(Intent.CATEGORY_HOME);ResolveInfo r=c.getPackageManager().resolveActivity(h,PackageManager.MATCH_DEFAULT_ONLY);return r!=null&&r.activityInfo!=null&&c.getPackageName().equals(r.activityInfo.packageName);}catch(Exception e){return false;}}
 static void requestDefaultHome(Activity a,int req){if(owner(a)){apply(a);return;}try{if(Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)a.getSystemService(Context.ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)&&!rm.isRoleHeld(RoleManager.ROLE_HOME)){a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),req);return;}}a.startActivityForResult(new Intent(Settings.ACTION_HOME_SETTINGS),req);}catch(Exception ignored){try{Intent h=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);a.startActivityForResult(h,req);}catch(Exception ignored2){}}}
 static void enter(Activity a){if(owner(a)){apply(a);try{a.startLockTask();}catch(Exception ignored){}return;}if(!isDefaultHome(a))requestDefaultHome(a,46);}
 static void exit(Activity a){if(!owner(a))return;try{a.stopLockTask();}catch(Exception ignored){}try{DevicePolicyManager d=dpm(a);ComponentName c=admin(a);d.setStatusBarDisabled(c,false);restriction(d,c,UserManager.DISALLOW_DEBUGGING_FEATURES,false);restriction(d,c,UserManager.DISALLOW_APPS_CONTROL,false);restriction(d,c,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,false);}catch(Exception ignored){}}
 @SuppressWarnings("deprecation") static boolean release(Context c){if(!owner(c))return false;try{DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();d.setUninstallBlocked(a,self,false);try{d.setStatusBarDisabled(a,false);}catch(Exception ignored){}if(Build.VERSION.SDK_INT>=30)try{d.setUserControlDisabledPackages(a,Collections.emptyList());}catch(Exception ignored){}
   restriction(d,a,UserManager.DISALLOW_SAFE_BOOT,false);restriction(d,a,UserManager.DISALLOW_ADD_USER,false);restriction(d,a,UserManager.DISALLOW_ADD_MANAGED_PROFILE,false);restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,false);restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,false);restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,false);restriction(d,a,UserManager.DISALLOW_MODIFY_ACCOUNTS,false);restriction(d,a,UserManager.DISALLOW_CONFIG_CREDENTIALS,false);restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,false);restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,false);
   d.clearPackagePersistentPreferredActivities(a,self);d.clearDeviceOwnerApp(self);return true;}catch(Exception e){return false;}}
}
