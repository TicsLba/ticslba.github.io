package cl.antumapu.aulacontrol;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class Secrets {
    private static final String ALIAS = "AulaControl.LocalSecrets.v1";
    private Secrets() {}

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return kg.generateKey();
    }

    static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            String iv = Base64.encodeToString(c.getIV(), Base64.NO_WRAP);
            String data = Base64.encodeToString(c.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8)), Base64.NO_WRAP);
            return iv + ":" + data;
        } catch (Exception e) { return ""; }
    }

    static String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return "";
        try {
            String[] p = encoded.split(":", 2);
            if (p.length != 2) return "";
            byte[] iv = Base64.decode(p[0], Base64.NO_WRAP);
            byte[] data = Base64.decode(p[1], Base64.NO_WRAP);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(c.doFinal(data), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
}
