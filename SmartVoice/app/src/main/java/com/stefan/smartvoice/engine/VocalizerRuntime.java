package com.stefan.smartvoice.engine;

import com.stefan.smartvoice.VoiceEntry;

/**
 * Vocalizer playback is intentionally blocked until Smart Voice has a runtime
 * authorized by the engine rights holder. The MultiTTS native library rejects
 * this app's package identity during JNI_OnLoad; repackaging voice data cannot
 * make that runtime usable.
 */
public final class VocalizerRuntime implements AutoCloseable {
    public interface PcmSink { boolean write(short[] samples); }

    private static final String UNAVAILABLE = "The Vocalizer native library shipped in MultiTTS is licensed to its host application and rejects Smart Voice during JNI_OnLoad. A Smart Voice-authorized Vocalizer SDK/runtime is required; voice packages alone cannot enable synthesis.";

    public VocalizerRuntime() { }

    public void speak(VoiceEntry entry, String text, float rate, float pitch, int volume, PcmSink sink) {
        throw new IllegalStateException(UNAVAILABLE);
    }

    public void stop() { }

    @Override public void close() { }
}
