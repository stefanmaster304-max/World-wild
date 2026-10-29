package com.stefan.smartvoice.engine;

import android.content.Context;
import android.media.AudioFormat;
import android.os.ParcelFileDescriptor;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.TextToSpeech;

import com.google.android.apps.speech.tts.googletts.audio.AndroidDecoder;
import com.google.android.apps.speech.tts.googletts.local.greco3.AndroidComposerImpl;
import com.google.android.apps.speech.tts.googletts.proto.Config$ConfigMessage;
import com.google.android.apps.speech.tts.googletts.proto.Markup$ContentBlock;
import com.google.android.apps.speech.tts.googletts.proto.Markup$ContentDetails;
import com.google.android.apps.speech.tts.googletts.proto.Markup$DataInfo;
import com.google.android.apps.speech.tts.googletts.proto.Markup$FloatData;
import com.google.android.apps.speech.tts.googletts.proto.Markup$InnerMessage;
import com.google.android.apps.speech.tts.googletts.proto.Markup$MarkupMessage;
import com.google.android.apps.speech.tts.googletts.proto.Markup$ParamData;
import com.google.android.apps.speech.tts.googletts.proto.Markup$TextBlock;
import com.google.android.apps.speech.tts.googletts.proto.Session$FeatureConfig;
import com.google.android.apps.speech.tts.googletts.proto.Session$SessionMessage;
import com.google.android.apps.speech.tts.googletts.proto.Session$VoiceConfig;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

import V2.a;
import W2.b;
import W2.f;
import W2.h;
import W2.i;
import W2.j;
import W2.k;
import W2.l;
import W2.m;
import W2.n;
import W2.r;
import W2.t;
import W2.u;

/**
 * Local Google voice-package adapter. The package layout and speech-session contract
 * follow the Google engine path already used by MultiTTS; the model remains user imported.
 */
public final class GoogleLocalEngine implements AutoCloseable {
    private static final String DEFAULT_CONFIG =
            "CmsKBWVuLUdCEmIKMwoGbGJsc2VxEhcKB2VuLUdCLjASDEUiIGwgLiBiIGk6IhoOCgRlbi4wEgZsYmxzZXEiABIrIikSJwoPXnwgfFtbOnB1bmN0Ol1dEgJMQhoIIFtyUl1vYWQiBmxibHNlcQp5CgVlbi1VUxJwEm4ibBJqCi1bWzpkaWdpdDpdXSsoPzpcXC5bWzpkaWdpdDpdXSk/ID8oPzrCsEZ8wrBDKT8SBSA/LSA/GixbWzpkaWdpdDpdXSsoPzpcXC5bWzpkaWdpdDpdXSk/ID8oPzrCsEZ8wrBDKSIEIHRvIA==";

    private final Context context;
    private final String voiceId;
    private final String gender;
    private final File voiceFolder;
    private AndroidComposerImpl composer;
    private volatile boolean cancelled;

    public GoogleLocalEngine(Context context, String voiceId, int gender, String voiceFolder) {
        this.context = context.getApplicationContext();
        this.voiceId = voiceId;
        this.gender = gender == 1 ? "male" : "female";
        this.voiceFolder = new File(voiceFolder);
    }

    public synchronized void initialize() throws Exception {
        if (composer != null) return;
        File pipeline = new File(voiceFolder, "pipeline.pb");
        File backend = new File(voiceFolder, "backend_pipeline.textproto");
        if (!voiceFolder.isDirectory() || !pipeline.isFile() || !backend.isFile()) {
            throw new IllegalStateException("Google voice package is missing its pipeline files");
        }
        AndroidComposerImpl engine = new AndroidComposerImpl(context, new AndroidDecoder());
        boolean loaded;
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pipeline, ParcelFileDescriptor.MODE_READ_ONLY)) {
            engine.d(pfd, voiceFolder.getAbsolutePath(), defaultConfig());
            loaded = engine.d;
        }
        if (!loaded) {
            engine.b();
            throw new IllegalStateException("Google local speech engine could not load this voice package");
        }
        composer = engine;
    }

    public void speak(String text, SynthesisCallback callback) throws Exception {
        speak(text, callback, 100, 100, 100);
    }

    public void speak(String text, SynthesisCallback callback, int ratePercent, int pitchPercent) throws Exception {
        speak(text, callback, ratePercent, pitchPercent, 100);
    }

    public void speak(String text, SynthesisCallback callback, int ratePercent, int pitchPercent, int volumePercent) throws Exception {
        initialize();
        cancelled = false;
        callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1);
        Markup$MarkupMessage markup = markup(text, 38.0f * ratePercent / 100.0f, 10.0f * pitchPercent / 100.0f);
        Session$SessionMessage session = session(voiceId, gender);
        if (!composer.c(markup, session)) {
            callback.error(TextToSpeech.ERROR);
            return;
        }
        while (!cancelled) {
            a state = composer.f();
            if (state != a.d) {
                if (state == a.c) callback.done();
                else callback.error(TextToSpeech.ERROR);
                return;
            }
            if (composer.e()) {
                byte[] pcm = composer.a();
                scalePcm16(pcm, volumePercent);
                if (pcm.length > 0 && callback.audioAvailable(pcm, 0, pcm.length) != TextToSpeech.SUCCESS) {
                    cancel();
                    return;
                }
            } else {
                Thread.sleep(2L);
            }
        }
        composer.g();
    }

    public synchronized void cancel() {
        cancelled = true;
        if (composer != null) composer.g();
    }

    @Override public synchronized void close() {
        cancelled = true;
        if (composer != null) {
            try { composer.b(); } catch (Exception ignored) { }
            composer = null;
        }
    }

    private static Config$ConfigMessage defaultConfig() throws InvalidProtocolBufferException {
        b builder = Config$ConfigMessage.newBuilder();
        builder.mergeFrom(Base64.getDecoder().decode(DEFAULT_CONFIG));
        return (Config$ConfigMessage) builder.build();
    }

    private static void scalePcm16(byte[] pcm, int volumePercent) {
        if (volumePercent == 100) return;
        float scale = Math.max(0, Math.min(150, volumePercent)) / 100.0f;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * scale)));
            pcm[i] = (byte) scaled; pcm[i + 1] = (byte) (scaled >>> 8);
        }
    }

    private static Markup$MarkupMessage markup(String text, float rate, float pitch) {
        n textBuilder = Markup$TextBlock.newBuilder();
        textBuilder.a(text);
        h detailsBuilder = Markup$ContentDetails.newBuilder();
        detailsBuilder.a((Markup$TextBlock) textBuilder.build());
        f blockBuilder = Markup$ContentBlock.newBuilder();
        blockBuilder.a((Markup$ContentDetails) detailsBuilder.build());
        k innerBuilder = Markup$InnerMessage.newBuilder();
        innerBuilder.a((Markup$ContentBlock) blockBuilder.build());
        m rateBuilder = Markup$ParamData.newBuilder();
        rateBuilder.a(rate);
        j pitchBuilder = Markup$FloatData.newBuilder();
        pitchBuilder.a(pitch);
        i dataBuilder = Markup$DataInfo.newBuilder();
        dataBuilder.a((Markup$ParamData) rateBuilder.build());
        dataBuilder.b((Markup$FloatData) pitchBuilder.build());
        innerBuilder.b((Markup$DataInfo) dataBuilder.build());
        l markupBuilder = Markup$MarkupMessage.newBuilder();
        markupBuilder.a((Markup$InnerMessage) innerBuilder.build());
        return (Markup$MarkupMessage) markupBuilder.build();
    }

    private static Session$SessionMessage session(String voice, String gender) {
        u voiceBuilder = Session$VoiceConfig.newBuilder();
        voiceBuilder.a(voice);
        voiceBuilder.b(gender);

        r features = Session$FeatureConfig.newBuilder();
        features.a("VocaineSilenceMuter__enable_muting", false);
        features.a("Darwinn__enable_darwinn", false);
        features.a("Darwinn__compile_on_device", false);
        features.a("Lemonbalm__enable_compile_on_device", false);
        features.a("Lemonbalm__enable_lemonbalm_darwinn_rio", false);
        features.a("Locomel__enable_compile_on_device", false);
        features.a("Locomel__enable_havoc_on_32bit", false);
        features.a("Locomel__enable_havoc_on_64bit", true);
        features.b("Locomel__duration_delegate");
        features.b("Locomel__mel_delegate");
        features.b("Locomel__nvoc_delegate");

        t sessionBuilder = Session$SessionMessage.newBuilder();
        sessionBuilder.a();
        sessionBuilder.b();
        sessionBuilder.c((Session$VoiceConfig) voiceBuilder.build());
        sessionBuilder.d((Session$FeatureConfig) features.build());
        return (Session$SessionMessage) sessionBuilder.build();
    }
}
