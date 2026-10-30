package p201a6;

import java.nio.charset.StandardCharsets;

/** Minimal string decoder needed by the recovered Vocalizer JNI wrapper. */
public abstract class AbstractC1638g {
    public static String m3250f(byte[] value, byte[] key) {
        for (int i = 0; i < value.length; i++) value[i] = (byte) (value[i] ^ key[i % key.length]);
        return new String(value, StandardCharsets.UTF_8);
    }
}
