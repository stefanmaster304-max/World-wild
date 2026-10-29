package com.stefan.smartvoice;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class LicenseVault {
    private static final String PREFS = "voice_store";
    private static final String KEY = "microsoft_model_license_encrypted";
    private static final String KEY_ALIAS = "smartvoice.microsoft.embedded.license";
    private final SharedPreferences prefs;

    LicenseVault(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void save(String value) throws Exception {
        write(KEY, value);
    }

    synchronized String read() { return read(KEY); }

    synchronized void saveSecret(String itemKey, String value) throws Exception { write("voice_param_" + itemKey, value); }
    synchronized String readSecret(String itemKey) { return read("voice_param_" + itemKey); }

    private void write(String preferenceKey, String value) throws Exception {
        if (value == null || value.isEmpty()) { prefs.edit().remove(preferenceKey).apply(); return; }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        ByteBuffer packed = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        packed.put((byte) iv.length).put(iv).put(encrypted);
        prefs.edit().putString(preferenceKey, Base64.encodeToString(packed.array(), Base64.NO_WRAP)).apply();
    }

    private String read(String preferenceKey) {
        String stored = prefs.getString(preferenceKey, "");
        if (stored.isEmpty()) return "";
        try {
            byte[] packed = Base64.decode(stored, Base64.NO_WRAP);
            ByteBuffer input = ByteBuffer.wrap(packed);
            int ivLength = input.get() & 0xff;
            if (ivLength < 12 || ivLength > 16 || input.remaining() <= ivLength) return "";
            byte[] iv = new byte[ivLength]; input.get(iv);
            byte[] encrypted = new byte[input.remaining()]; input.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
