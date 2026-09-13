package cl.antumapu.aulacontrol;

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
 static void apply(Context c){if(!owner(c))return;try{DevicePolicyManager d=dpm(c);ComponentName a=admin(c);d.setUninstallBlocked(a,c.getPackageName(),true);d.addUserRestriction(a,UserManager.DISALLOW_SAFE_BOOT);d.addUserRestriction(a,UserManager.DISALLOW_ADD_USER);d.addUserRestriction(a,UserManager.DISALLOW_ADD_MANAGED_PROFILE);d.setLockTaskPackages(a,allowed(c));if(Build.VERSION.SDK_INT>=28)d.setLockTaskFeatures(a,DevicePolicyManager.LOCK_TASK_FEATURE_NONE);IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);f.addCategory(Intent.CATEGORY_HOME);f.addCategory(Intent.CATEGORY_DEFAULT);d.addPersistentPreferredActivity(a,f,new ComponentName(c,MainActivity.class));}catch(Exception ignored){}}
 static boolean isDefaultHome(Context c){try{Intent h=new Intent(Intent.ACTION_MAIN);h.addCategory(Intent.CATEGORY_HOME);ResolveInfo r=c.getPackageManager().resolveActivity(h,PackageManager.MATCH_DEFAULT_ONLY);return r!=null&&r.activityInfo!=null&&c.getPackageName().equals(r.activityInfo.packageName);}catch(Exception e){return false;}}
 static void requestDefaultHome(Activity a,int req){if(owner(a)){apply(a);return;}try{if(Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)a.getSystemService(Context.ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)&&!rm.isRoleHeld(RoleManager.ROLE_HOME)){a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),req);return;}}a.startActivityForResult(new Intent(Settings.ACTION_HOME_SETTINGS),req);}catch(Exception ignored){try{Intent h=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);a.startActivityForResult(h,req);}catch(Exception ignored2){}}}
 static void enter(Activity a){if(owner(a)){apply(a);try{a.startLockTask();}catch(Exception ignored){}return;}if(!isDefaultHome(a))requestDefaultHome(a,46);}
 static void exit(Activity a){if(!owner(a))return;try{a.stopLockTask();}catch(Exception ignored){}}
 @SuppressWarnings("deprecation") static boolean release(Context c){if(!owner(c))return false;try{DevicePolicyManager d=dpm(c);ComponentName a=admin(c);d.setUninstallBlocked(a,c.getPackageName(),false);d.clearPackagePersistentPreferredActivities(a,c.getPackageName());try{d.clearUserRestriction(a,UserManager.DISALLOW_SAFE_BOOT);d.clearUserRestriction(a,UserManager.DISALLOW_ADD_USER);d.clearUserRestriction(a,UserManager.DISALLOW_ADD_MANAGED_PROFILE);}catch(Exception ignored){}d.clearDeviceOwnerApp(c.getPackageName());return true;}catch(Exception e){return false;}}
}
