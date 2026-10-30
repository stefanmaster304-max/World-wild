package es.codefactory.vocalizertts.voices;

import com.google.common.base.Ascii;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import p201a6.AbstractC1638g;
import p211b7.AbstractC1880e;
import p252d5.C3283a;

/* JADX INFO: loaded from: classes2.dex */
public class VocalizerEngine {

    /* JADX INFO: renamed from: g */
    public static final String f11478g = AbstractC1638g.m3250f(new byte[]{-39, 11, -15, -11, 90, -99, 74, -16, -31, 3, -5, -34, 105, -72}, new byte[]{-113, 100, -110, -80, Ascii.FF, -14, 41, -75});

    /* JADX INFO: renamed from: a */
    public final String f11479a;

    /* JADX INFO: renamed from: b */
    public final C3283a f11480b;

    /* JADX INFO: renamed from: c */
    public VocalizerVoice[] f11481c = null;

    /* JADX INFO: renamed from: d */
    public VocalizerVoice f11482d = null;
    private long mNativeEnginePtr = 0;

    /* JADX INFO: renamed from: e */
    public boolean f11483e = false;

    /* JADX INFO: renamed from: f */
    public boolean f11484f = false;

    public VocalizerEngine(String str, C3283a c3283a) {
        this.f11480b = c3283a;
        this.f11479a = str;
        System.loadLibrary(AbstractC1638g.m3250f(new byte[]{94, -64, Ascii.CAN, Ascii.SYN, -52, -106, -91, 60, 115, -44, Ascii.NAK, 17, -43, -106, -127}, new byte[]{Ascii.DLE, -75, 121, 120, -81, -13, -13, 83}));
        m6349g();
    }

    private boolean audioSamplesReceived(short[] samples) {
        p252d5.C3283a target = this.f11480b;
        return target != null && !target.f11291e && target.onSamples(samples);
    }

    /* JADX INFO: renamed from: b */
    private native void m6345b();

    /* JADX INFO: renamed from: c */
    private native void m6346c(int i);

    /* JADX INFO: renamed from: d */
    private native int m6347d();

    /* JADX INFO: renamed from: f */
    private native void m6348f(int i);

    /* JADX INFO: renamed from: g */
    private native void m6349g();

    /* JADX INFO: renamed from: h */
    private native void m6350h(int i);

    /* JADX INFO: renamed from: j */
    private native boolean m6351j(VocalizerVoice vocalizerVoice);

    /* JADX INFO: renamed from: l */
    private native void m6352l(int i);

    /* JADX INFO: renamed from: n */
    private native void m6353n();

    /* JADX INFO: renamed from: o */
    private native boolean m6354o(int i);

    /* JADX INFO: renamed from: pr */
    private native void m6355pr(int i);

    /* JADX INFO: renamed from: q */
    private native String m6356q();

    /* JADX INFO: renamed from: s */
    private native void m6357s(int i);

    /* JADX INFO: renamed from: t */
    private native void m6358t(String str);

    /* JADX INFO: renamed from: w */
    private native int m6359w();

    /* JADX INFO: renamed from: x */
    private native int m6360x();

    /* JADX INFO: renamed from: y */
    private native int m6361y();

    /* JADX INFO: renamed from: z */
    private native int m6362z();

    /* JADX INFO: renamed from: A */
    public final void m6363A() {
        if (m6371i()) {
            m6346c(1);
        }
    }

    /* JADX INFO: renamed from: B */
    public final void m6364B(int i) {
        if (m6371i()) {
            m6352l(i);
        }
    }

    /* JADX INFO: renamed from: C */
    public final void m6365C() {
        if (m6371i()) {
            m6355pr(0);
        }
    }

    /* JADX INFO: renamed from: D */
    public final void m6366D(String str) {
        if (m6371i() && this.f11482d != null && m6354o(4096)) {
            m6358t(str);
        }
    }

    /* JADX INFO: renamed from: E */
    public final void m6367E() {
        if (!m6371i() || this.f11482d == null) {
            return;
        }
        m6345b();
    }

    /* JADX INFO: renamed from: a */
    public native VocalizerVoice[] m6368a();

    /* JADX INFO: renamed from: e */
    public final void m6369e() {
        File[] fileArrListFiles;
        String str;
        if (this.f11484f) {
            m6374p(false);
            return;
        }
        AbstractC1638g.m3250f(new byte[]{43, 5, -109, 82, 65, -73, -38, 41}, new byte[]{79, 106, -38, 60, 40, -61, -32, 9});
        File file = new File(this.f11479a);
        String string = "";
        if (file.exists() && file.isDirectory() && (fileArrListFiles = collectPackageFiles(file)) != null) {
            Locale locale = Locale.ROOT;
            StringBuilder sb = new StringBuilder();
            int length = fileArrListFiles.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    string = sb.toString();
                    break;
                }
                File file2 = fileArrListFiles[i];
                if (this.f11484f) {
                    break;
                }
                if (!file2.isDirectory() && (file2.getName().toLowerCase(locale).endsWith(".xml") || file2.getName().toLowerCase(locale).endsWith(".hdr"))) {
                    int length2 = (int) file2.length();
                    byte[] bArr = new byte[length2];
                    try {
                        FileInputStream fileInputStream = new FileInputStream(file2);
                        try {
                            fileInputStream.read(bArr);
                            Charset charset = StandardCharsets.UTF_8;
                            str = new String(bArr, charset);
                            if (!str.toLowerCase(Locale.ROOT).contains(AbstractC1638g.m3250f(new byte[]{79, -127, -73, -95, -73, -115, 40, 67}, new byte[]{115, -17, -62, -64, -39, -18, 77, 125}))) {
                                byte[] bytes = AbstractC1638g.m3250f(new byte[]{97, 37, -32, 105, 92, 66, -60, -30, 66, Ascii.CR, -110, 89, 68, 103, -56, -111, 71, Ascii.SI, -22, 87, 102, 90, -35, -93, 98, Ascii.SI, -50, 72, 86, 117, -29, -22, 88, 8, -24, 87, 106, 2, -70, -65, 67, 40, -13, 67, 89, 122, -36, -18, 78, Ascii.DLE, -52, 86, 75, 118, -61, -79, 96, 53, -111, 89, 104, 66, -74, -29, Ascii.CAN, 111, -3, 42, 125, 3, -56, -77, 92, 110, -14, 78, 108, 70, -64, UnsignedBytes.MAX_POWER_OF_TWO, 67, 101, -19, 46, 121, 116, -51, -68, 74, 39, -109, 92, 91, 80, -55, -110, 74, Ascii.RS, -55, 123, 88, 11, -33, -116, 99, Ascii.EM, -26, 45, 117, 115, -36, -73, Ascii.FS, 41, -9, 121, 89, 0, -57, -111, 111, 105, -19, 116, 71, 74, -30, -76, 82, 104, -46, 47, 109, Ascii.DEL, -24, UnsignedBytes.MAX_POWER_OF_TWO, 71, Ascii.DLE, -35, 117, 80, 97, -65, -87, 126, 55, -23, 42, 39, 68, -1, -24, 77, Ascii.CAN, -42, 110, 86, 92, -66, -19, 110, Ascii.ETB, -12, 112, 71, 7, -37, -120, 109, 36, -48, 34, 82, 91, -23, -116, 67, 53, -10, 47, 86, 88, -42, -111, 91, 11, -111, 86, 111, 85, -7, -86, Ascii.FS, Ascii.CAN, -97, 79, 123, 123, -56, -19, 88, 4, -58, 35, 84, 90, -39, -111, 82, 40, -14, 40, 38, 90, -53, -112, 99, Ascii.ETB, -60, 45, 119, 90, -32, -74, Ascii.FS, 59, -42, Ascii.DEL, 109, 122, -10, -96, 120, Ascii.US, -54, 91, 40, 122, -35, -113, 125, 110, -30, 82, 81, 1, -21, -126, Ascii.ESC, 10, -60, 44, 41, 96, -24, -73, 66, Ascii.CAN, -56, 96, 118, 66, -24, -126, 98, 105, -30, 40, 70, 117, -12, -84, Ascii.SUB, 48, -108, 86, 110, 122, -26, -30, 125, Ascii.RS, -23, 87, 90, 92, -41, -87, 125, 7, -56, 118, 114, 123, -55, -85, 98, 106, -53, 34, 39, 125, -65, -98, 74, Ascii.DC4, -64, 119, 103, 68, -69, -67, 89, 40, -55, 89, 73, 99, -38, -113, 68, 4, -106, 105, 114, 7, -53, -67, 91, Ascii.DLE, -64, 114, 114, 107, -61, -126, Ascii.EM, Ascii.SO, -55, 78, 75, 91, -73, -21, Ascii.RS, 45, -31, 105, 120, 96, -56, -65, 106, 49, -2, 35, 83, 83, -20, -22, Ascii.CAN, 62, -3, 107, 109, 106, -34, -18, 83, 48, -31, 84, Ascii.DEL, 70, -49, -104, Ascii.SUB, 17, -33, 91, 115, 72, -60, -67, 72, 54, -109, 113, 79, 120, -42, -87, 92, Ascii.CAN, -51, 75, 78, 2, -27, -101, 69, 59, -58, 42, 100, 91, -61, -77, 125, 17, -30, 78, 39, 112, -39, -111, 77, Ascii.US, -51, 75, 89, 92, -19, -73, Ascii.DEL, Ascii.ESC, -29, Ascii.DEL, 82, 101, -33, -86, Ascii.GS, Ascii.ESC, -35, 88, 88, 72, -62, -108, 104, 59, -44, 85, 123, 101, -72, -20, 111, 106, -28, 47, 40, 81, -72, -120, 124, 51, -62, 73, 120, 66, -8, -85, 103, 54, -9, 105, 93, 100, -38, -68, 74, 53, -59, 66, 45, SignedBytes.MAX_POWER_OF_TWO, -44, -100, Ascii.EM, Ascii.DLE, -23, 89, 89, 90, -62, -97, 105, 51, -51, 81, 43, 3, -33, -24, 123, Ascii.SUB, -98, 47, 100, 68, -3, -86, Ascii.SUB, Ascii.ESC, -53, 79, 107, 95, -54, -101, 69, 109, -27, 113, 47, 5, -8, -20, 120, 47, -34, 105, 116, 94, -23, -18, 65, 4, -49, 86, 83, SignedBytes.MAX_POWER_OF_TWO, -28, -79, 91, Ascii.EM, -59, 98, 79, 80, -10, -83, 19, 53, -52, 76, 107, 100, -38, -116, 100, 17, -11, 94, 115, 123, -51, -76, 99, 51, -44, 84, 70, 7, -8, -106, 95, Ascii.FF, -111, 76, 119, 83, -23, -22, 90, 41, -105, 77, 110, 92, -12, -82, 113, 109, -10, 67, 104, 113, -41, -69, 76, 54, -50, 43, 46, 103, -65, -84, 76, Ascii.US, -9, 95, 117, 67, -4, -97, 124, 39, -32, 66, 83, 112, -65, -117, Ascii.CAN, 50, -13, 115, 72, 113, -17, -82, 73, 17, -12, 98, 119, 120, -68, -18, 73, 49, -10, 121, 70, 0, -37, -115, Ascii.GS, 7, -9, 121, 90, 93, -21, -120, 101, Ascii.ETB, -60, 40, 45, 112, -6, -69, 65, 37, -33, 124, 83, 6, -71, -19, 124, 45, -22, 118, 117, 90, -32, -94, 114, 108, -28, 44, 86, SignedBytes.MAX_POWER_OF_TWO, -23, -94, 68, 108, -51, 47, 74, 100, -28, -113, 125, 46, -30, 108, 120, 121, -69, -80, 122, 56, -105, 34, 42, 90, -57, -65, 88, 100, -32, 80, Ascii.DEL, 71, -63, -79, 78, 5, -46, 47, 111, 74, -30, -65, 71, Ascii.DC4, -14, 124, 118, 67, -8, -96, 68, 47, -109, SignedBytes.MAX_POWER_OF_TWO, 110, 4, -5, -115, 108, 56, -27, 105, 108, 113, -42, -105, 105, 9, -54, 108, 73, 112, -69, -79, 126, 53, -3, 84, 87, 106, -21, -101, 106, 52, -28, 120, 45, 103, -35, -21, 122, Ascii.CAN, -106, 94, 125, 122, -67, -93, Ascii.EM, 62, -10, 106, 78, Ascii.DEL, -35, -17, 74, 36, -35, 98, 90, 72, -67, -114, Ascii.GS, 49, -45, 83, 44, 75, -27, -87, 120, 53, -106, 35, 47, 104, -61, -82, 99, 7, -12, 85, 107, 97, -5, -80, 108, Ascii.SO, -47, 108, 103, 121, -68, -87, Ascii.DEL, 105, -14, 124, 39, 94, -3, -114, 122, 54, -27, 86, 90, 75, -7, -17, 93, 51, -33, 125, 115, 5, -51, -105, 106, 111, -27, 44, 119, 125, -40, UnsignedBytes.MAX_POWER_OF_TWO, Ascii.SUB, 56, -16, 121, 85, 96, -38, -114, Ascii.GS, 7, -12, 117, 124, 3, -34, -75, 70, 19, -55, 76, 102, 69, -20, -65, 114, 39, -62, 89, 81, 123, -12, -80, Ascii.US, 104, -49, 34, 82, 93, -1, -77, Ascii.ESC, 9, -35, 40, 78, 5, -54, -112, 96, Ascii.DC2, -54, 117, 85, 5, -68, -107, 103, Ascii.SI, -18, 80, 46, 66, -66, -110, Ascii.FS, 4, -52, 66, 124, 66, -51, -126, 81, Ascii.ETB, -110, 35, 93, 122, -35, -94, Ascii.DEL, 111, -48, 75, 83, 121, -53, -118, 101, 49, -97, 76, 100, 123, -52, -68, 90, Ascii.CAN, -29, 34, 71, 2, -53, -115, 126, Ascii.NAK, -3, 120, 111, 2, -12, -109, 98, 19, -55, 109, 120, 112, -17, -30, 76, 4, -63, 84, 75, 101, -35, -105, 99, Ascii.DC2, -28, 88, 81, 83, -64, -117, 105, Ascii.EM, -105, 79, 41, 72, -24, -20, 
                                120, 44, -63, Ascii.DEL, 112, 100, -10, -23, 67, 10, -35, 44, 121, 117, -8, -109, 68, 59, -108, 95, 106, 81, -30, -38}, new byte[]{43, 93, -89, Ascii.SUB, Ascii.RS, 50, -114, -38}).getBytes(charset);
                                int i8 = 0;
                                int i9 = 0;
                                while (i8 < length2) {
                                    bArr[i8] = (byte) (bArr[i8] ^ bytes[i9 % 1024]);
                                    i8++;
                                    i9++;
                                }
                                str = new String(bArr, StandardCharsets.UTF_8);
                            }
                            fileInputStream.close();
                        } catch (Throwable th) {
                            try {
                                fileInputStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                        }
                    } catch (Exception e8) {
                        AbstractC1880e.m4021d(f11478g, AbstractC1638g.m3250f(new byte[]{109, -71, -108, -109, 48, -5, Ascii.DEL, -10, 122, -65, -102, -109, Ascii.DC4, -59, 114, -62, 122, -80, -100, -103, Ascii.DC4, -81, 59}, new byte[]{Ascii.US, -36, -11, -9, 113, -107, Ascii.ESC, -78}), e8);
                        str = null;
                    }
                    sb.append(str);
                    sb.append("\r\n");
                }
                i++;
            }
        }
        if (this.f11484f) {
            return;
        }
        boolean zM6375r = m6375r(string);
        AbstractC1638g.m3250f(new byte[]{100, 54, -20, -72, Ascii.GS, -93, Ascii.GS, Ascii.RS, 115, 45, -60, -94, 17, -22}, new byte[]{0, 89, -91, -42, 116, -41, 39, 62});
        this.f11481c = m6368a();
        AbstractC1638g.m3250f(new byte[]{-111, 94, Ascii.NAK, -70, 68, -16, 93, -28, -104, 103, 51, -67, 78, -31, 43, -83, -122, 69, 124, -72, 72, -22, 90}, new byte[]{-11, 49, 92, -44, 45, -124, 103, -60});
        if (!zM6375r) {
            m6374p(false);
        } else {
            m6374p(true);
            AbstractC1638g.m3250f(new byte[]{119, 11, 56, 6, 115, -119, -73, -85, 112, Ascii.DLE, 37, Ascii.SO, 121, -103, -124, -92, 109, Ascii.CR, 35, 1, 83, -103, -112, -84, 106, Ascii.FF, 41, 11, 47, -48}, new byte[]{Ascii.EM, 100, 76, 111, Ascii.NAK, -16, -2, -59});
        }
    }

    private static File[] collectPackageFiles(File root) {
        java.util.ArrayList<File> files = new java.util.ArrayList<>();
        java.util.ArrayDeque<File> pending = new java.util.ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            File current = pending.removeFirst();
            File[] children = current.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.isDirectory()) pending.addLast(child);
                else files.add(child);
            }
        }
        return files.toArray(new File[0]);
    }

    /* JADX INFO: renamed from: h */
    public final String m6370h() {
        return m6356q();
    }

    /* JADX INFO: renamed from: i */
    public final boolean m6371i() {
        boolean z7;
        synchronized (this) {
            z7 = this.f11483e;
        }
        return z7;
    }

    /* JADX WARN: Code duplicated, block: B:13:0x002e  */
    /* JADX WARN: Code duplicated, block: B:21:0x005e  */
    /* JADX INFO: renamed from: k */
    public final boolean m6372k(VocalizerVoice vocalizerVoice) {
        VocalizerVoice vocalizerVoice2;
        boolean zM6351j;
        if (m6371i()) {
            VocalizerVoice[] vocalizerVoiceArr = this.f11481c;
            if (vocalizerVoiceArr == null || vocalizerVoiceArr.length == 0) {
                AbstractC1638g.m3250f(new byte[]{Ascii.SYN, 61, -74, -33, -39, 81, 108, 55, Ascii.US, 104, -9, -43, -32, Ascii.RS, 105, 59, Ascii.ESC, 54, -78, -33, -81, 72, 106, 61, Ascii.EM, 55, -9, -35, -32, 75, 107, 48, 84}, new byte[]{122, 82, -41, -69, -113, 62, 5, 84});
                VocalizerVoice[] vocalizerVoiceArrM6368a = m6368a();
                this.f11481c = vocalizerVoiceArrM6368a;
                if (vocalizerVoiceArrM6368a == null || vocalizerVoiceArrM6368a.length == 0) {
                    AbstractC1638g.m3250f(new byte[]{0, -52, 121, -39, -5, -116, -79, 82, 9, -103, 56, -49, -56, -113, -73, 80, 8, -125, 98, -40, -33, -116, -8, 71, 3, -54, 123, -40, -125}, new byte[]{108, -93, Ascii.CAN, -67, -83, -29, -40, 49});
                } else {
                    vocalizerVoice2 = this.f11482d;
                    if (vocalizerVoice2 == null && vocalizerVoice2.equals(vocalizerVoice)) {
                        AbstractC1638g.m3250f(new byte[]{-49, -28, 65, -1, -125, 94, 3, -119, -58, -79, 0, -24, -76, 92, Ascii.SI, -54, -43, -28, 73, -8, -80, Ascii.GS, 74, -103, -56, -30, 80, -75}, new byte[]{-93, -117, 32, -101, -43, 49, 106, -22});
                        return true;
                    }
                    m6367E();
                    zM6351j = m6351j(vocalizerVoice);
                    AbstractC1638g.m3250f(new byte[]{-54, -38, -122, 40, 54, -95, 96, 118, -61, -113, -57, 34, 1, -70, 96, 99, -61, -107, -117, 35, 1, -86, 41, 103, -61, -63, -38}, new byte[]{-90, -75, -25, 76, 96, -50, 9, Ascii.NAK});
                    if (zM6351j) {
                        this.f11482d = vocalizerVoice;
                        return true;
                    }
                }
            } else {
                vocalizerVoice2 = this.f11482d;
                if (vocalizerVoice2 == null) {
                }
                m6367E();
                zM6351j = m6351j(vocalizerVoice);
                AbstractC1638g.m3250f(new byte[]{-54, -38, -122, 40, 54, -95, 96, 118, -61, -113, -57, 34, 1, -70, 96, 99, -61, -107, -117, 35, 1, -86, 41, 103, -61, -63, -38}, new byte[]{-90, -75, -25, 76, 96, -50, 9, Ascii.NAK});
                if (zM6351j) {
                    this.f11482d = vocalizerVoice;
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: renamed from: m */
    public final synchronized void m6373m() {
        System.currentTimeMillis();
        this.f11484f = true;
        if (m6371i()) {
            try {
                m6367E();
                m6353n();
            } catch (Exception unused) {
                AbstractC1638g.m3250f(new byte[]{-117, -65, 2, 109, 8, 60, -65, -31, -39}, new byte[]{-7, -38, 110, 8, 105, 79, -38, -37});
            }
            this.f11482d = null;
            this.f11481c = null;
            m6374p(false);
        }
        AbstractC1638g.m3250f(new byte[]{-15, 103, 111, 92, -112, 47, Ascii.FF, 123, -93, 118, 108, 86, -102, 124}, new byte[]{-125, 2, 3, 57, -15, 92, 105, 65});
        System.currentTimeMillis();
        AbstractC1638g.m3250f(new byte[]{67, 108, -93, 52}, new byte[]{99, 1, -48, Ascii.SUB, -4, -7, 111, -8});
    }

    /* JADX INFO: renamed from: p */
    public final void m6374p(boolean z7) {
        synchronized (this) {
            this.f11483e = z7;
        }
    }

    /* JADX INFO: renamed from: r */
    public native boolean m6375r(String str);

    /* JADX INFO: renamed from: t */
    public final void m6376t() {
        if (m6371i()) {
            m6348f(100);
        }
    }

    /* JADX INFO: renamed from: u */
    public final void m6377u(int i) {
        if (m6371i()) {
            m6350h(i);
        }
    }

    /* JADX INFO: renamed from: v */
    public final void m6378v() {
        if (m6371i()) {
            m6357s(1);
        }
    }
}
