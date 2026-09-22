package cl.antumapu.pangi.v16;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PangiStore {
    static final String ROLE_STUDENT = "student";
    static final String ROLE_TEACHER = "teacher";

    static final String K_ROLE = "pangi.role";
    static final String K_NAME = "pangi.name";
    static final String K_COURSE = "pangi.course";
    static final String K_TOKEN = "pangi.token";

    static final String STATE_PREPARED = "PREPARED";
    static final String STATE_ACTIVE = "ACTIVE";
    static final String STATE_CLOSING = "CLOSING";

    private static final String PREF = "pangi_v16";
    private static final String CONFIGURED = "configured";
    private static final String DEVICE_NAME = "device_name";
    private static final String TECH_KEY = "technical_key";
    private static final String PW_SALT = "pw_salt";
    private static final String PW_HASH = "pw_hash";
    private static final String ADMIN_MODE = "admin_mode";
    private static final String GUEST_READY = "guest_ready";
    private static final String SESSION_STATE = "session_state";

    private PangiStore(){}

    static SharedPreferences sp(Context c){
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static boolean configured(Context c){ return sp(c).getBoolean(CONFIGURED,false); }
    static String deviceName(Context c){ return sp(c).getString(DEVICE_NAME,"PANGI"); }
    static String technicalKey(Context c){ return sp(c).getString(TECH_KEY,""); }

    static void configureOwner(Context c,String device,String technical,String password){
        byte[] salt=new byte[16];
        new SecureRandom().nextBytes(salt);
        String salt64=Base64.encodeToString(salt,Base64.NO_WRAP);
        String hash=hashPassword(password,salt);
        sp(c).edit()
                .putBoolean(CONFIGURED,true)
                .putString(DEVICE_NAME,device)
                .putString(TECH_KEY,technical)
                .putString(PW_SALT,salt64)
                .putString(PW_HASH,hash)
                .putBoolean(ADMIN_MODE,false)
                .apply();
    }

    static boolean checkAdmin(Context c,String password){
        try{
            byte[] salt=Base64.decode(sp(c).getString(PW_SALT,""),Base64.NO_WRAP);
            String expected=sp(c).getString(PW_HASH,"");
            return !expected.isEmpty() && constantTime(expected,hashPassword(password,salt));
        }catch(Exception e){ return false; }
    }

    static void adminMode(Context c,boolean on){ sp(c).edit().putBoolean(ADMIN_MODE,on).apply(); }
    static boolean adminMode(Context c){ return sp(c).getBoolean(ADMIN_MODE,false); }

    static void bootstrapGuest(Context c, Intent intent){
        PersistableBundle b=null;
        try{
            b=intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
        }catch(Exception ignored){}
        if(b==null)b=new PersistableBundle();
        sp(c).edit()
                .putString(K_ROLE,b.getString(K_ROLE,""))
                .putString(K_NAME,b.getString(K_NAME,""))
                .putString(K_COURSE,b.getString(K_COURSE,""))
                .putString(K_TOKEN,b.getString(K_TOKEN,""))
                .putString(SESSION_STATE,STATE_PREPARED)
                .putBoolean(GUEST_READY,false)
                .apply();
    }

    static String role(Context c){ return sp(c).getString(K_ROLE,""); }
    static String userName(Context c){ return sp(c).getString(K_NAME,""); }
    static String course(Context c){ return sp(c).getString(K_COURSE,""); }

    static void guestReady(Context c,boolean ready){ sp(c).edit().putBoolean(GUEST_READY,ready).apply(); }
    static boolean guestReady(Context c){ return sp(c).getBoolean(GUEST_READY,false); }

    static void state(Context c,String state){ sp(c).edit().putString(SESSION_STATE,state).apply(); }
    static String state(Context c){ return sp(c).getString(SESSION_STATE,STATE_PREPARED); }

    private static String hashPassword(String password,byte[] salt){
        try{
            PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,120000,256);
            byte[] out=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.encodeToString(out,Base64.NO_WRAP);
        }catch(Exception e){
            return Base64.encodeToString((password+new String(salt,StandardCharsets.ISO_8859_1))
                    .getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
        }
    }

    private static boolean constantTime(String a,String b){
        if(a.length()!=b.length())return false;
        int d=0;
        for(int i=0;i<a.length();i++)d|=a.charAt(i)^b.charAt(i);
        return d==0;
    }
}
