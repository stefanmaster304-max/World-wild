package p252d5;

import es.codefactory.vocalizertts.voices.VocalizerEngine;

/** PCM callback holder expected by the MultiTTS Vocalizer JNI library. */
public final class C3283a {
    public interface SampleSink { boolean accept(short[] pcm); }

    public final String f11287a;
    public VocalizerEngine f11288b;
    public volatile boolean f11291e;
    public String f11290d;
    private volatile SampleSink sink;

    public C3283a(String path, SampleSink sink) {
        this.f11287a = path;
        this.sink = sink;
    }

    public boolean onSamples(short[] pcm) {
        SampleSink active = sink;
        return active != null && active.accept(pcm);
    }

    public void setSink(SampleSink sink) { this.sink = sink; }
}
