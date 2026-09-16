package cl.antumapu.aulacontrol;

import android.Manifest;import android.app.*;import android.app.admin.*;import android.content.*;import android.content.pm.*;import android.os.*;import java.util.*;

final class Managed{
 private Managed(){}
 static ComponentName admin(Context c){return new ComponentName(c,AdminReceiver.class);} static DevicePolicyManager dpm(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);}
 static boolean owner(Context c){try{return dpm(c).isDeviceOwnerApp(c.getPackageName());}catch(Exception e){return false;}} static boolean profileOwner(Context c){try{return dpm(c).isProfileOwnerApp(c.getPackageName());}catch(Exception e){return false;}} static boolean managed(Context c){return owner(c)||profileOwner(c);}
 static void restriction(DevicePolicyManager d,ComponentName a,String key,boolean on){try{if(on)d.addUserRestriction(a,key);else d.clearUserRestriction(a,key);}catch(Exception ignored){}}
 static void grant(DevicePolicyManager d,ComponentName a,String pkg,String permission){try{d.setPermissionGrantState(a,pkg,permission,DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);}catch(Exception ignored){}}
 static void grantManagedPermissions(Context c,DevicePolicyManager d,ComponentName a,String self){
  grant(d,a,self,Manifest.permission.ACCESS_COARSE_LOCATION);
  grant(d,a,self,Manifest.permission.ACCESS_FINE_LOCATION);
  if(Build.VERSION.SDK_INT>=29)grant(d,a,self,Manifest.permission.ACCESS_BACKGROUND_LOCATION);
  if(Build.VERSION.SDK_INT>=33)grant(d,a,self,Manifest.permission.POST_NOTIFICATIONS);
 }
 static void apply(Context c){if(owner(c)){if(Core.adminMode(c))adminUnlock(c);else applyOwner(c);}else if(profileOwner(c))applyGuest(c);}

 static void applyOwner(Context c){
  if(Core.adminMode(c)){adminUnlock(c);return;}
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
   preferTabletSchoolHome(c,d,a);
  }catch(Exception ignored){}
 }

 static void applyGuest(Context c){
  try{
   DevicePolicyManager d=dpm(c);ComponentName a=admin(c);String self=c.getPackageName();
   d.setAffiliationIds(a,Collections.singleton(SessionUsers.AFFILIATION));
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
   grantManagedPermissions(c,d,a,self);

   if(RecoveryPrefs.lost(c))preferTabletSchoolHome(c,d,a);
   else if(Core.supervisionStarted(c)&&ScreenCaptureService.active)preferOriginalHome(c,d,a);
   else preferTabletSchoolHome(c,d,a);
  }catch(Exception ignored){}
 }

 static IntentFilter homeFilter(){
  IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);f.addCategory(Intent.CATEGORY_HOME);f.addCategory(Intent.CATEGORY_DEFAULT);return f;
 }

 static List<ComponentName> homes(Context c){
  ArrayList<ComponentName> out=new ArrayList<>();
  try{
   Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
   for(ResolveInfo r:c.getPackageManager().queryIntentActivities(q,PackageManager.MATCH_ALL)){
    if(r.activityInfo!=null)out.add(new ComponentName(r.activityInfo.packageName,r.activityInfo.name));
   }
  }catch(Exception ignored){}
  return out;
 }

 static ComponentName originalHome(Context c){
  String self=c.getPackageName();
  for(ComponentName n:homes(c))if(!self.equals(n.getPackageName()))return n;
  return null;
 }

 static void clearHomePreferences(Context c,DevicePolicyManager d,ComponentName a){
  LinkedHashSet<String> pkgs=new LinkedHashSet<>();pkgs.add(c.getPackageName());
  for(ComponentName n:homes(c))pkgs.add(n.getPackageName());
  for(String pkg:pkgs)try{d.clearPackagePersistentPreferredActivities(a,pkg);}catch(Exception ignored){}
 }

 static void preferTabletSchoolHome(Context c,DevicePolicyManager d,ComponentName a){
  try{
   clearHomePreferences(c,d,a);
   d.addPersistentPreferredActivity(a,homeFilter(),new ComponentName(c,PrivacyGateActivity.class));
  }catch(Exception ignored){}
 }

 static void preferOriginalHome(Context c,DevicePolicyManager d,ComponentName a){
  try{
   ComponentName n=originalHome(c);if(n==null)return;
   clearHomePreferences(c,d,a);
   d.addPersistentPreferredActivity(a,homeFilter(),n);
  }catch(Exception ignored){}
 }

 static void enableGuestLauncher(Context c){
  if(!profileOwner(c)||RecoveryPrefs.lost(c)||!Core.supervisionStarted(c)||!ScreenCaptureService.active)return;
  try{preferOriginalHome(c,dpm(c),admin(c));}catch(Exception ignored){}
 }

 static boolean openOriginalHome(Context c){
  try{
   ComponentName n=originalHome(c);if(n==null)return false;
   Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(n).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
   c.startActivity(i);return true;
  }catch(Exception ignored){return false;}
 }

 static void enterGate(Activity a){if(owner(a)&&!Core.adminMode(a)){applyOwner(a);try{a.startLockTask();}catch(Exception ignored){}}}

 static void adminUnlock(Context c){
  if(!owner(c))return;
  try{
   DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
   restriction(d,a,UserManager.DISALLOW_UNINSTALL_APPS,false);
   restriction(d,a,UserManager.DISALLOW_INSTALL_APPS,false);
   restriction(d,a,UserManager.DISALLOW_APPS_CONTROL,false);
   restriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,false);
   restriction(d,a,UserManager.DISALLOW_DEBUGGING_FEATURES,false);
   restriction(d,a,UserManager.DISALLOW_FACTORY_RESET,false);
   d.setUninstallBlocked(a,c.getPackageName(),false);
   try{d.setLockTaskPackages(a,new String[]{});}catch(Exception ignored){}
   try{d.setStatusBarDisabled(a,false);}catch(Exception ignored){}
  }catch(Exception ignored){}
 }

 static void lockNow(Context c){try{if(managed(c))dpm(c).lockNow();}catch(Exception ignored){}}
 static void restoreProtection(Context c){Core.adminMode(c,false);apply(c);}
 static void exitGate(Activity a){try{a.stopLockTask();}catch(Exception ignored){}}
 static void enter(Activity a){enterGate(a);} static void exit(Activity a){exitGate(a);if(owner(a))adminUnlock(a);}

 @SuppressWarnings("deprecation") static boolean release(Context c){
  if(!owner(c))return false;
  try{
   adminUnlock(c);DevicePolicyManager d=dpm(c);ComponentName a=admin(c);
   try{d.setLockTaskPackages(a,new String[]{});}catch(Exception ignored){}
   clearHomePreferences(c,d,a);
   d.clearDeviceOwnerApp(c.getPackageName());
   return true;
  }catch(Exception e){return false;}
 }
}
