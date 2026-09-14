package cl.antumapu.aulacontrol;

import android.app.*;import android.app.usage.*;import android.content.*;import android.net.*;import android.os.*;import android.provider.Settings;import android.util.Base64;
import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import javax.crypto.*;import javax.crypto.spec.*;

final class Core {
 static final String P="aulacontrol"; private Core(){}
 static android.content.SharedPreferences sp(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);} 
 static String id(Context c){var s=sp(c);String v=s.getString("id","");if(!v.isEmpty())return v;String a="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var r=new SecureRandom();var b=new StringBuilder("TAB-");for(int i=0;i<8;i++)b.append(a.charAt(r.nextInt(a.length())));v=b.toString();s.edit().putString("id",v).apply();return v;}
 static String dn(Context c){return sp(c).getString("dn","");}
 static String key(Context c){var s=sp(c);String enc=s.getString("key_enc","");if(!enc.isEmpty()){String d=Secrets.decrypt(enc);if(!d.isEmpty())return d;}String legacy=s.getString("key","");if(!legacy.isEmpty()){String e=Secrets.encrypt(legacy);if(!e.isEmpty())s.edit().putString("key_enc",e).remove("key").apply();return legacy;}return "";}
 static String user(Context c){return sp(c).getString("user","");} static String course(Context c){return sp(c).getString("course","");}
 static boolean ready(Context c){var s=sp(c);return !dn(c).isEmpty()&&!key(c).isEmpty()&&!s.getString("salt","").isEmpty()&&!s.getString("hash","").isEmpty();}
 static void setup(Context c,String dn,String key,String pw){String salt=salt(),hash=hash(pw,salt),enc=Secrets.encrypt(key.trim());sp(c).edit().putString("dn",dn.trim()).putString("key_enc",enc).remove("key").putString("salt",salt).putString("hash",hash).apply();}
 static void config(Context c,String dn,String key,String pw){String enc=Secrets.encrypt(key.trim());var e=sp(c).edit().putString("dn",dn.trim()).putString("key_enc",enc).remove("key");if(!pw.isEmpty()){String s=salt();e.putString("salt",s).putString("hash",hash(pw,s));}e.apply();}
 static boolean checkPw(Context c,String pw){var s=sp(c);return eq(s.getString("hash",""),hash(pw,s.getString("salt","")));}
 static void login(Context c,String u,String co){sp(c).edit().putString("user",u.trim()).putString("course",co.trim()).putLong("last",System.currentTimeMillis()).apply();}
 static void touch(Context c){sp(c).edit().putLong("last",System.currentTimeMillis()).apply();} static long last(Context c){return sp(c).getLong("last",System.currentTimeMillis());}
 static void logout(Context c){sp(c).edit().remove("user").remove("course").remove("last").apply();GuestSession.begin(c);}
 static String salt(){byte[] b=new byte[16];new SecureRandom().nextBytes(b);return Base64.encodeToString(b,Base64.NO_WRAP);} 
 static String hash(String pw,String salt){try{var spec=new PBEKeySpec(pw.toCharArray(),Base64.decode(salt,Base64.NO_WRAP),120000,256);byte[] b=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return Base64.encodeToString(b,Base64.NO_WRAP);}catch(Exception e){return "";}}
 static String hmac(String key,String data){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return hex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
 static String sha(String x){try{return hex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
 static byte[] netKey(String key)throws Exception{return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));}
 static byte[] seal(String key,byte[] plain){try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(netKey(key),"AES"),new GCMParameterSpec(128,nonce));byte[] ct=c.doFinal(plain);byte[] out=new byte[nonce.length+ct.length];System.arraycopy(nonce,0,out,0,nonce.length);System.arraycopy(ct,0,out,nonce.length,ct.length);return out;}catch(Exception e){return new byte[0];}}
 static byte[] open(String key,byte[] sealed){try{if(sealed==null||sealed.length<28)return new byte[0];byte[] nonce=Arrays.copyOfRange(sealed,0,12),ct=Arrays.copyOfRange(sealed,12,sealed.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(netKey(key),"AES"),new GCMParameterSpec(128,nonce));return c.doFinal(ct);}catch(Exception e){return new byte[0];}}
 static String sealText(String key,String plain){byte[] b=seal(key,(plain==null?"":plain).getBytes(StandardCharsets.UTF_8));return Base64.encodeToString(b,Base64.NO_WRAP);}
 static String openText(String key,String enc){try{return new String(open(key,Base64.decode(enc,Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
 static boolean eq(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
 static String hex(byte[] b){var s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}
 static String ip(){try{var n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){var ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;var a=ni.getInetAddresses();while(a.hasMoreElements()){var x=a.nextElement();if(x instanceof Inet4Address&&x.isSiteLocalAddress())return x.getHostAddress();}}}catch(Exception ignored){}return "0.0.0.0";}
 static java.util.List<InetAddress> broadcasts(){var o=new LinkedHashSet<InetAddress>();try{o.add(InetAddress.getByName("255.255.255.255"));}catch(Exception ignored){}try{var n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){var ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;for(var ia:ni.getInterfaceAddresses())if(ia.getBroadcast()!=null)o.add(ia.getBroadcast());}}catch(Exception ignored){}return new ArrayList<>(o);}
 static boolean usage(Context c){try{var a=(AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}catch(Exception e){return false;}}
 static long activity(Context c){long latest=last(c);if(!usage(c))return latest;try{var m=(UsageStatsManager)c.getSystemService(Context.USAGE_STATS_SERVICE);long now=System.currentTimeMillis();var es=m.queryEvents(Math.max(0,now-15*60*1000L),now);var e=new UsageEvents.Event();while(es!=null&&es.hasNextEvent()){es.getNextEvent(e);int t=e.getEventType();if(t==UsageEvents.Event.USER_INTERACTION||t==UsageEvents.Event.ACTIVITY_RESUMED||t==UsageEvents.Event.SCREEN_INTERACTIVE||t==UsageEvents.Event.KEYGUARD_HIDDEN)latest=Math.max(latest,e.getTimeStamp());}}catch(Exception ignored){}return latest;}
 static boolean overlay(Context c){return Settings.canDrawOverlays(c);} 
}
