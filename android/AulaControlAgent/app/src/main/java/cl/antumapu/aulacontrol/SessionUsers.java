package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;import android.content.*;import android.os.*;import java.util.*;

final class SessionUsers{
 static final String AFFILIATION="aulacontrol-managed-guest-v1";private SessionUsers(){}
 static boolean createAndSwitch(Context c,String role,String name,String course){
  if(!Managed.owner(c))return false;
  try{
   DevicePolicyManager d=Managed.dpm(c);d.setAffiliationIds(Managed.admin(c),Collections.singleton(AFFILIATION));
   PersistableBundle ex=new PersistableBundle();ex.putString("role",role);ex.putString("name",name);ex.putString("course",course==null?"":course);ex.putString("deviceName",Core.dn(c));ex.putString("deviceId",Core.id(c));ex.putString("key",Core.key(c));ex.putString("adminSalt",Core.adminSalt(c));ex.putString("adminHash",Core.adminHash(c));
   int flags=DevicePolicyManager.SKIP_SETUP_WIZARD|DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED;if(Build.VERSION.SDK_INT>=28)flags|=DevicePolicyManager.MAKE_USER_EPHEMERAL;
   UserHandle u=d.createAndManageUser(Managed.admin(c),role.equals(Core.ROLE_TEACHER)?"Profesor temporal":"Estudiante temporal",Managed.admin(c),ex,flags);if(u==null)return false;return d.switchUser(Managed.admin(c),u);
  }catch(Exception e){return false;}
 }
 static void logoutGuest(Context c){
  try{DevicePolicyManager d=Managed.dpm(c);if(Build.VERSION.SDK_INT>=28){d.logoutUser(Managed.admin(c));}else{d.wipeData(0);}}catch(Exception ignored){}
 }
 static void cleanupSecondaryUsers(Context c){if(!Managed.owner(c)||Build.VERSION.SDK_INT<28)return;try{DevicePolicyManager d=Managed.dpm(c);for(UserHandle u:d.getSecondaryUsers(Managed.admin(c)))try{d.removeUser(Managed.admin(c),u);}catch(Exception ignored){}}catch(Exception ignored){}}
}
