package cl.antumapu.aulacontrol;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class Crypto {
    private static final String KS="AndroidKeyStore", ALIAS="TabletEscolar3.Local";
    private Crypto(){}

    static String encryptLocal(String plain){
        try{
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,localKey());
            byte[] iv=c.getIV(), ct=c.doFinal((plain==null?"":plain).getBytes(StandardCharsets.UTF_8));
            byte[] out=new byte[iv.length+ct.length];
            System.arraycopy(iv,0,out,0,iv.length);System.arraycopy(ct,0,out,iv.length,ct.length);
            return Base64.encodeToString(out,Base64.NO_WRAP);
        }catch(Exception e){return "";}
    }

    static String decryptLocal(String enc){
        try{
            byte[] all=Base64.decode(enc,Base64.NO_WRAP);if(all.length<29)return "";
            byte[] iv=new byte[12],ct=new byte[all.length-12];System.arraycopy(all,0,iv,0,12);System.arraycopy(all,12,ct,0,ct.length);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,localKey(),new GCMParameterSpec(128,iv));
            return new String(c.doFinal(ct),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }

    private static SecretKey localKey() throws Exception{
        KeyStore s=KeyStore.getInstance(KS);s.load(null);if(s.containsAlias(ALIAS))return (SecretKey)s.getKey(ALIAS,null);
        KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,KS);
        g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return g.generateKey();
    }

    static String salt(){byte[] b=new byte[16];new SecureRandom().nextBytes(b);return Base64.encodeToString(b,Base64.NO_WRAP);}
    static String passwordHash(String pw,String salt){
        try{PBEKeySpec s=new PBEKeySpec(pw.toCharArray(),Base64.decode(salt,Base64.NO_WRAP),180000,256);return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(s).getEncoded(),Base64.NO_WRAP);}catch(Exception e){return "";}
    }
    static boolean equal(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    static String sha256(String text){try{return hex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
    static String hmac(String key,String text){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return hex(m.doFinal(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return "";}}
    static byte[] seal(String key,byte[] plain){
        try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)),"AES"),new GCMParameterSpec(128,nonce));byte[] ct=c.doFinal(plain);byte[] out=new byte[nonce.length+ct.length];System.arraycopy(nonce,0,out,0,nonce.length);System.arraycopy(ct,0,out,nonce.length,ct.length);return out;}catch(Exception e){return new byte[0];}
    }
    static byte[] open(String key,byte[] all){
        try{if(all==null||all.length<29)return new byte[0];byte[] nonce=new byte[12],ct=new byte[all.length-12];System.arraycopy(all,0,nonce,0,12);System.arraycopy(all,12,ct,0,ct.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)),"AES"),new GCMParameterSpec(128,nonce));return c.doFinal(ct);}catch(Exception e){return new byte[0];}
    }
    static String sealText(String key,String text){return Base64.encodeToString(seal(key,(text==null?"":text).getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
    static String openText(String key,String text){try{return new String(open(key,Base64.decode(text,Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}
}
