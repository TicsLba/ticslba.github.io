package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import java.security.SecureRandom;

final class Store {
    static final String PREF="tablet_escolar_v3";
    static final String ROLE_STUDENT="student", ROLE_TEACHER="teacher";
    static final String STATE_GATE="GATE", STATE_CREATING="CREATING_USER", STATE_SETUP="SESSION_SETUP", STATE_WAITING="WAITING_CAPTURE", STATE_ACTIVE="ACTIVE", STATE_CLOSING="CLOSING";
    static final String X_ROLE="role",X_NAME="name",X_COURSE="course",X_DEVICE_NAME="deviceName",X_DEVICE_ID="deviceId",X_KEY="technicalKey",X_AFFILIATION="affiliation",X_RELAY="relay";

    private Store(){}
    static SharedPreferences sp(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    static boolean configured(Context c){return !deviceName(c).isEmpty()&&!technicalKey(c).isEmpty()&&!sp(c).getString("pw_salt","").isEmpty()&&!sp(c).getString("pw_hash","").isEmpty();}
    static void configure(Context c,String deviceName,String key,String password){
        String salt=Crypto.salt();
        sp(c).edit().putString("device_name",deviceName.trim()).putString("technical_key",Crypto.encryptLocal(key.trim())).putString("pw_salt",salt).putString("pw_hash",Crypto.passwordHash(password,salt)).putString("state",STATE_GATE).putBoolean("guest",false).apply();
        deviceId(c);
    }
    static boolean checkPassword(Context c,String pw){String s=sp(c).getString("pw_salt",""),h=sp(c).getString("pw_hash","");return !s.isEmpty()&&!h.isEmpty()&&Crypto.equal(h,Crypto.passwordHash(pw,s));}
    static String deviceName(Context c){return sp(c).getString("device_name","");}
    static String technicalKey(Context c){return Crypto.decryptLocal(sp(c).getString("technical_key",""));}
    static String deviceId(Context c){String v=sp(c).getString("device_id","");if(!v.isEmpty())return v;String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";SecureRandom r=new SecureRandom();StringBuilder b=new StringBuilder("TAB-");for(int i=0;i<8;i++)b.append(chars.charAt(r.nextInt(chars.length())));v=b.toString();sp(c).edit().putString("device_id",v).apply();return v;}
    static String state(Context c){return sp(c).getString("state",STATE_GATE);}
    static void state(Context c,String s){sp(c).edit().putString("state",s).apply();}
    static boolean guest(Context c){return sp(c).getBoolean("guest",false);}
    static String role(Context c){return sp(c).getString("role","");}
    static String user(Context c){return Crypto.decryptLocal(sp(c).getString("user","");}
    static String course(Context c){return Crypto.decryptLocal(sp(c).getString("course","");}
    static String roleLabel(Context c){return ROLE_TEACHER.equals(role(c))?"Profesor":ROLE_STUDENT.equals(role(c))?"Estudiante":"";}
    static long idleLimit(Context c){return ROLE_TEACHER.equals(role(c))?30L*60_000L:10L*60_000L;}
    static long lastActivity(Context c){return sp(c).getLong("last_activity",System.currentTimeMillis());}
    static void touch(Context c){sp(c).edit().putLong("last_activity",System.currentTimeMillis()).apply();}
    static boolean guestPolicyApplied(Context c){return sp(c).getBoolean("guest_policy_applied",false);}
    static void guestPolicyApplied(Context c,boolean b){sp(c).edit().putBoolean("guest_policy_applied",b).apply();}
    static boolean supervisionStarted(Context c){return sp(c).getBoolean("supervision_started",false);}
    static void supervisionStarted(Context c,boolean b){sp(c).edit().putBoolean("supervision_started",b).apply();}
    static boolean lost(Context c){return sp(c).getBoolean("lost",false);}
    static void lost(Context c,boolean b){sp(c).edit().putBoolean("lost",b).apply();}
    static String relay(Context c){return sp(c).getString("relay","");}
    static void relay(Context c,String u){sp(c).edit().putString("relay",u==null?"":u.trim()).apply();}

    static PersistableBundle provisioning(Context c,String role,String name,String course){
        PersistableBundle b=new PersistableBundle();b.putString(X_ROLE,role);b.putString(X_NAME,name);b.putString(X_COURSE,course==null?"":course);b.putString(X_DEVICE_NAME,deviceName(c));b.putString(X_DEVICE_ID,deviceId(c));b.putString(X_KEY,technicalKey(c));b.putString(X_AFFILIATION,PolicyManager.AFFILIATION);b.putString(X_RELAY,relay(c));return b;
    }
    static void setupGuest(Context c,Intent intent){
        PersistableBundle b=null;try{b=intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);}catch(Exception ignored){}
        if(b==null)return;setupGuest(c,b);
    }
    static void setupGuest(Context c,PersistableBundle b){
        if(b==null)return;String role=b.getString(X_ROLE,""),name=b.getString(X_NAME,""),course=b.getString(X_COURSE,""),dn=b.getString(X_DEVICE_NAME,""),id=b.getString(X_DEVICE_ID,""),key=b.getString(X_KEY,"");
        if((!ROLE_STUDENT.equals(role)&&!ROLE_TEACHER.equals(role))||name.isEmpty()||dn.isEmpty()||id.isEmpty()||key.isEmpty())return;
        sp(c).edit().putBoolean("guest",true).putString("role",role).putString("user",Crypto.encryptLocal(name)).putString("course",Crypto.encryptLocal(course)).putString("device_name",dn).putString("device_id",id).putString("technical_key",Crypto.encryptLocal(key)).putString("relay",b.getString(X_RELAY,"")).putString("state",STATE_SETUP).putLong("last_activity",System.currentTimeMillis()).putBoolean("guest_policy_applied",false).putBoolean("supervision_started",false).apply();
    }
    static void clearGuestIdentity(Context c){sp(c).edit().remove("role").remove("user").remove("course").remove("last_activity").remove("supervision_started").putString("state",STATE_CLOSING).apply();}
}
