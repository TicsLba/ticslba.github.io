package cl.antumapu.aulacontrol;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.app.usage.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.util.Base64;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

final class Core {
    static final String P="aulacontrol";
    static final String ROLE_STUDENT="student", ROLE_TEACHER="teacher", ROLE_ADMIN="admin";
    static final double[] ALLOWED_FPS={0.5,1,2,4,6};
    static final String K_ROLE="role", K_NAME="name", K_COURSE="course", K_DEVICE_NAME="deviceName",
            K_DEVICE_ID="deviceId", K_KEY="key", K_ADMIN_SALT="adminSalt", K_ADMIN_HASH="adminHash",
            K_AFFILIATION="affiliation";
    private Core(){}

    static android.content.SharedPreferences sp(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}

    static String id(Context c){
        var s=sp(c);String v=s.getString("id","");
        if(!v.isEmpty())return v;
        String a="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var r=new SecureRandom();var b=new StringBuilder("TAB-");
        for(int i=0;i<8;i++)b.append(a.charAt(r.nextInt(a.length())));
        v=b.toString();s.edit().putString("id",v).apply();return v;
    }

    static String dn(Context c){return sp(c).getString("dn","");}
    static String dec(Context c,String keyName){String enc=sp(c).getString(keyName+"_enc","");return enc.isEmpty()?"":Secrets.decrypt(enc);}
    static void encPut(Context c,String keyName,String value){sp(c).edit().putString(keyName+"_enc",Secrets.encrypt(value==null?"":value)).remove(keyName).apply();}
    static String key(Context c){String v=dec(c,"key");if(!v.isEmpty())return v;String legacy=sp(c).getString("key","");if(!legacy.isEmpty())encPut(c,"key",legacy);return legacy;}
    static String user(Context c){return dec(c,"user");}
    static String course(Context c){return dec(c,"course");}
    static String role(Context c){String r=sp(c).getString("role","");return r==null?"":r;}
    static boolean guest(Context c){return sp(c).getBoolean("guest_session",false);}
    static boolean adminMode(Context c){return sp(c).getBoolean("admin_mode",false);}
    static boolean privacyAccepted(Context c){return sp(c).getBoolean("privacy_ack",false);}
    static void privacyAccepted(Context c,boolean v){sp(c).edit().putBoolean("privacy_ack",v).apply();}
    static boolean supervisionStarted(Context c){return sp(c).getBoolean("supervision_started",false);}
    static void supervisionStarted(Context c,boolean v){sp(c).edit().putBoolean("supervision_started",v).apply();}

    static boolean ready(Context c){
        if(guest(c)){
            String r=role(c);
            return !dn(c).isEmpty()&&!key(c).isEmpty()&&!user(c).isEmpty()&&
                    (ROLE_STUDENT.equals(r)||ROLE_TEACHER.equals(r));
        }
        var s=sp(c);
        return !dn(c).isEmpty()&&!key(c).isEmpty()&&!s.getString("salt","").isEmpty()&&!s.getString("hash","").isEmpty();
    }

    static void setup(Context c,String dn,String key,String pw){
        String salt=salt(),hash=hash(pw,salt);
        sp(c).edit().putBoolean("guest_session",false).putBoolean("supervision_started",false)
                .putString("dn",dn.trim()).putString("key_enc",Secrets.encrypt(key.trim())).remove("key")
                .putString("salt",salt).putString("hash",hash).putBoolean("privacy_ack",true).apply();
    }

    static void setupGuest(Context c, Intent intent){
        if(intent==null)return;
        PersistableBundle b=null;
        try{b=intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);}catch(Exception ignored){}
        if(b==null){
            b=new PersistableBundle();
            copy(intent,b,K_ROLE);copy(intent,b,K_NAME);copy(intent,b,K_COURSE);copy(intent,b,K_DEVICE_NAME);
            copy(intent,b,K_DEVICE_ID);copy(intent,b,K_KEY);copy(intent,b,K_AFFILIATION);
        }
        setupGuest(c,b);
    }

    private static void copy(Intent i,PersistableBundle b,String key){
        try{String v=i.getStringExtra(key);if(v!=null)b.putString(key,v);}catch(Exception ignored){}
    }

    static void setupGuest(Context c,PersistableBundle b){
        if(b==null)return;
        String technical=b.getString(K_KEY,"");
        String deviceName=b.getString(K_DEVICE_NAME,"");
        String deviceId=b.getString(K_DEVICE_ID,"");
        String role=b.getString(K_ROLE,ROLE_STUDENT);
        String name=b.getString(K_NAME,"");
        String course=b.getString(K_COURSE,"");
        if(technical.isEmpty()||deviceName.isEmpty()||deviceId.isEmpty()||name.isEmpty())return;
        if(!ROLE_STUDENT.equals(role)&&!ROLE_TEACHER.equals(role))return;

        sp(c).edit()
                .putBoolean("guest_session",true)
                .putBoolean("privacy_ack",true)
                .putBoolean("supervision_started",false)
                .putString("dn",deviceName)
                .putString("id",deviceId)
                .putString("key_enc",Secrets.encrypt(technical))
                .remove("salt").remove("hash")
                .putString("role",role)
                .putString("user_enc",Secrets.encrypt(name))
                .putString("course_enc",Secrets.encrypt(course))
                .putLong("last",System.currentTimeMillis())
                .remove("admin_mode")
                .apply();
    }

    static void config(Context c,String dn,String key,String pw){
        var e=sp(c).edit().putString("dn",dn.trim()).putString("key_enc",Secrets.encrypt(key.trim())).remove("key");
        if(!pw.isEmpty()){String s=salt();e.putString("salt",s).putString("hash",hash(pw,s));}
        e.apply();
    }

    static String adminSalt(Context c){return sp(c).getString("salt","");}
    static String adminHash(Context c){return sp(c).getString("hash","");}
    static boolean checkPw(Context c,String pw){
        var s=sp(c);String salt=s.getString("salt",""),stored=s.getString("hash","");
        if(salt.isEmpty()||stored.isEmpty())return false;
        return eq(stored,hash(pw,salt));
    }

    static void login(Context c,String u,String co,String role){
        sp(c).edit().putString("user_enc",Secrets.encrypt(u.trim())).putString("course_enc",Secrets.encrypt(co==null?"":co.trim()))
                .putString("role",role).putLong("last",System.currentTimeMillis()).apply();
    }

    static void clearIdentity(Context c){
        sp(c).edit().remove("user_enc").remove("course_enc").remove("user").remove("course").remove("role")
                .remove("last").remove("admin_mode").remove("supervision_started").apply();
    }

    static void touch(Context c){sp(c).edit().putLong("last",System.currentTimeMillis()).apply();}
    static long last(Context c){return sp(c).getLong("last",System.currentTimeMillis());}
    static long idleLimit(Context c){return ROLE_TEACHER.equals(role(c))?30*60*1000L:10*60*1000L;}
    static double fps(Context c){double v=Double.longBitsToDouble(sp(c).getLong("screen_fps_bits",Double.doubleToRawLongBits(2.0)));return allowedFps(v)?v:2.0;}
    static boolean allowedFps(double v){for(double x:ALLOWED_FPS)if(Math.abs(x-v)<0.01)return true;return false;}
    static boolean fps(Context c,double v){if(!allowedFps(v))return false;sp(c).edit().putLong("screen_fps_bits",Double.doubleToRawLongBits(v)).apply();return true;}
    static long frameIntervalMs(Context c){return Math.max(160L,Math.round(1000.0/fps(c)));}
    static String roleLabel(Context c){return ROLE_TEACHER.equals(role(c))?"Profesor":ROLE_STUDENT.equals(role(c))?"Estudiante":"";}
    static void adminMode(Context c,boolean v){sp(c).edit().putBoolean("admin_mode",v).apply();}

    static String salt(){byte[] b=new byte[16];new SecureRandom().nextBytes(b);return Base64.encodeToString(b,Base64.NO_WRAP);}
    static String hash(String pw,String salt){try{var spec=new PBEKeySpec(pw.toCharArray(),Base64.decode(salt,Base64.NO_WRAP),180000,256);byte[] b=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return Base64.encodeToString(b,Base64.NO_WRAP);}catch(Exception e){return "";}}
    static String hmac(String key,String data){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return hex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
    static String sha(String x){try{return hex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
    static byte[] netKey(String key)throws Exception{return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));}
    static byte[] seal(String key,byte[] plain){try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(netKey(key),"AES"),new GCMParameterSpec(128,nonce));byte[] ct=c.doFinal(plain);byte[] out=new byte[nonce.length+ct.length];System.arraycopy(nonce,0,out,0,nonce.length);System.arraycopy(ct,0,out,nonce.length,ct.length);return out;}catch(Exception e){return new byte[0];}}
    static byte[] open(String key,byte[] sealed){try{if(sealed==null||sealed.length<28)return new byte[0];byte[] nonce=Arrays.copyOfRange(sealed,0,12),ct=Arrays.copyOfRange(sealed,12,sealed.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(netKey(key),"AES"),new GCMParameterSpec(128,nonce));return c.doFinal(ct);}catch(Exception e){return new byte[0];}}
    static String sealText(String key,String plain){return Base64.encodeToString(seal(key,(plain==null?"":plain).getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
    static String openText(String key,String enc){try{return new String(open(key,Base64.decode(enc,Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
    static boolean eq(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    static String hex(byte[] b){var s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}

    static String ip(){try{var n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){var ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;var a=ni.getInetAddresses();while(a.hasMoreElements()){var x=a.nextElement();if(x instanceof Inet4Address&&x.isSiteLocalAddress())return x.getHostAddress();}}}catch(Exception ignored){}return "0.0.0.0";}
    static java.util.List<InetAddress> broadcasts(){var o=new LinkedHashSet<InetAddress>();try{o.add(InetAddress.getByName("255.255.255.255"));}catch(Exception ignored){}try{var n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){var ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;for(var ia:ni.getInterfaceAddresses())if(ia.getBroadcast()!=null)o.add(ia.getBroadcast());}}catch(Exception ignored){}return new ArrayList<>(o);}

    static boolean usageAccess(Context c){
        try{
            AppOpsManager a=(AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);
            if(a==null)return false;
            int mode=a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName());
            return mode==AppOpsManager.MODE_ALLOWED;
        }catch(Exception ignored){return false;}
    }

    // Sólo se consulta el timestamp del último evento de interacción. No se conserva
    // historial de aplicaciones, sitios, texto ni contenido personal.
    static long activity(Context c){
        long latest=last(c);
        try{
            var m=(UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE);
            long now=System.currentTimeMillis();
            var es=m.queryEvents(Math.max(0,now-35*60*1000L),now);
            var e=new UsageEvents.Event();
            while(es!=null&&es.hasNextEvent()){
                es.getNextEvent(e);
                int t=e.getEventType();
                if(t==UsageEvents.Event.USER_INTERACTION||t==UsageEvents.Event.ACTIVITY_RESUMED||t==UsageEvents.Event.SCREEN_INTERACTIVE||t==UsageEvents.Event.KEYGUARD_HIDDEN)
                    latest=Math.max(latest,e.getTimeStamp());
            }
        }catch(Exception ignored){}
        return latest;
    }

    static boolean overlay(Context c){return Settings.canDrawOverlays(c);}
}
