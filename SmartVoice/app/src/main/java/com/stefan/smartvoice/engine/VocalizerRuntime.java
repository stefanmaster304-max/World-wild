package com.stefan.smartvoice.engine;

import com.stefan.smartvoice.VoiceEntry;
import es.codefactory.vocalizertts.voices.VocalizerEngine;
import es.codefactory.vocalizertts.voices.VocalizerVoice;
import p252d5.C3283a;

import java.io.File;

/** ARM64 bridge to the Vocalizer JNI runtime carried by the repository's MultiTTS APK. */
public final class VocalizerRuntime implements AutoCloseable {
    public interface PcmSink { boolean write(short[] samples); }

    private C3283a callback;
    private VocalizerEngine engine;
    private VocalizerVoice activeVoice;
    private String activeId = "";

    public VocalizerRuntime() { }

    public synchronized void load(VoiceEntry entry) throws Exception {
        if (entry == null || !"vocalizer".equals(entry.engine)) throw new IllegalArgumentException("Not a Vocalizer voice");
        if (entry.id.equals(activeId) && engine != null && engine.m6371i()) return;
        closeEngine();
        File root = new File(entry.dataPath);
        if (!root.isDirectory()) throw new IllegalStateException("Vocalizer voice data folder is missing");
        callback = new C3283a(root.getAbsolutePath(), null);
        callback.f11291e = false;
        engine = new VocalizerEngine(root.getAbsolutePath(), callback);
        callback.f11288b = engine;
        synchronized (engine) {
            if (!engine.m6371i()) {
                engine.f11484f = false;
                engine.m6369e();
            }
        }
        VocalizerVoice[] voices = engine.m6368a();
        if (voices == null || voices.length == 0 || !engine.m6371i()) {
            closeEngine();
            throw new IllegalStateException("Vocalizer could not load the model and header in this package");
        }
        VocalizerVoice match = null;
        for (VocalizerVoice candidate : voices) {
            candidate.setInstallationPath(root.getAbsolutePath());
            if (candidate.getVoiceName().equalsIgnoreCase(entry.name)
                    || candidate.getLanguage().equalsIgnoreCase(entry.locale)) {
                match = candidate;
                if (candidate.getVoiceName().equalsIgnoreCase(entry.name)) break;
            }
        }
        if (match == null) match = voices[0];
        if (!engine.m6372k(match)) {
            closeEngine();
            throw new IllegalStateException("Vocalizer refused the selected voice model");
        }
        engine.m6365C();
        engine.m6376t();
        engine.m6378v();
        engine.m6363A();
        activeVoice = match;
        activeId = entry.id;
    }

    public synchronized void speak(VoiceEntry entry, String text, float rate, float pitch,
                                   int volume, PcmSink sink) throws Exception {
        load(entry);
        callback.f11291e = false;
        callback.setSink(samples -> {
            if (callback.f11291e) return false;
            if (volume == 100) return sink.write(samples);
            short[] scaled = new short[samples.length];
            float gain = Math.max(0, Math.min(150, volume)) / 100.0f;
            for (int i = 0; i < samples.length; i++) {
                int value = Math.round(samples[i] * gain);
                scaled[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
            }
            return sink.write(scaled);
        });
        engine.m6364B(Math.max(1, Math.min(5, Math.round(rate * 2.0f))));
        engine.m6377u(Math.max(1, Math.min(5, Math.round(pitch * 2.5f))));
        engine.m6366D(text == null ? "" : text);
    }

    public synchronized void stop() {
        if (callback != null) {
            callback.f11291e = true;
            callback.setSink(null);
        }
        if (engine != null && engine.m6371i()) engine.m6367E();
    }

    private void closeEngine() {
        if (engine != null) {
            try { engine.m6373m(); } catch (Exception ignored) { }
            engine = null;
        }
        activeVoice = null;
        activeId = "";
    }

    @Override public synchronized void close() {
        stop();
        closeEngine();
    }
}
