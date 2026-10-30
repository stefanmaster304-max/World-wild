package com.stefan.smartvoice.engine;

import android.media.AudioFormat;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;

import com.microsoft.cognitiveservices.speech.AudioDataStream;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.EmbeddedSpeechConfig;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.stefan.smartvoice.VoiceEntry;
import com.stefan.smartvoice.VoiceStore;
import com.stefan.smartvoice.EmojiTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class SmartVoiceTtsService extends TextToSpeechService {
    private final Object lock = new Object();
    private volatile boolean stopped;
    private SpeechSynthesizer activeSynthesizer;
    private EmbeddedSpeechConfig activeConfig;
    private String activeVoiceId = "";
    private GoogleLocalEngine activeGoogle;
    private String activeGoogleVoiceId = "";
    private VocalizerRuntime activeVocalizer;
    private String activeVocalizerVoiceId = "";

    @Override public int onIsLanguageAvailable(String language, String country, String variant) {
        Locale requested = new Locale(language == null ? "" : language, country == null ? "" : country, variant == null ? "" : variant);
        for (VoiceEntry v : new VoiceStore(this).list()) {
            Locale supported = v.toLocale();
            if (supported.getLanguage().equalsIgnoreCase(requested.getLanguage())) {
                return supported.getCountry().equalsIgnoreCase(requested.getCountry()) ? TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE : TextToSpeech.LANG_COUNTRY_AVAILABLE;
            }
        }
        return TextToSpeech.LANG_NOT_SUPPORTED;
    }

    @Override protected String[] onGetLanguage() {
        List<VoiceEntry> voices = new VoiceStore(this).list();
        if (voices.isEmpty()) return new String[]{"", "", ""};
        Locale locale = voices.get(0).toLocale();
        String language;
        String country;
        try { language = locale.getISO3Language(); } catch (java.util.MissingResourceException e) { language = locale.getLanguage(); }
        try { country = locale.getISO3Country(); } catch (java.util.MissingResourceException e) { country = locale.getCountry(); }
        return new String[]{language, country, ""};
    }

    @Override protected int onLoadLanguage(String language, String country, String variant) {
        return onIsLanguageAvailable(language, country, variant);
    }

    @Override public String onGetDefaultVoiceNameFor(String language, String country, String variant) {
        Locale requested = new Locale(language == null ? "" : language, country == null ? "" : country, variant == null ? "" : variant);
        for (VoiceEntry v : new VoiceStore(this).list()) if (v.toLocale().getLanguage().equalsIgnoreCase(requested.getLanguage())) return v.id;
        return null;
    }

    @Override public List<Voice> onGetVoices() {
        ArrayList<Voice> voices = new ArrayList<>();
        for (VoiceEntry entry : new VoiceStore(this).list()) {
            if (entry.toLocale().equals(Locale.ROOT)) continue;
            voices.add(new Voice(entry.id, entry.toLocale(), Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, Collections.emptySet()));
        }
        return voices;
    }

    @Override public int onIsValidVoiceName(String voiceName) {
        for (VoiceEntry v : new VoiceStore(this).list()) if (v.id.equals(voiceName)) return TextToSpeech.SUCCESS;
        return TextToSpeech.ERROR;
    }

    @Override public int onLoadVoice(String voiceName) {
        // Model initialization is lazy and occurs on the synthesis thread.
        return onIsValidVoiceName(voiceName);
    }

    @Override protected void onStop() {
        stopped = true;
        synchronized (lock) {
            if (activeSynthesizer != null) {
                try { activeSynthesizer.StopSpeakingAsync(); } catch (Exception ignored) { }
            }
            if (activeGoogle != null) activeGoogle.cancel();
            if (activeVocalizer != null) activeVocalizer.stop();
        }
    }

    @Override protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        try {
            synthesizeRequest(request, callback);
        } catch (RuntimeException e) {
            failUnexpectedRequest(request, callback, e);
        } catch (LinkageError e) {
            // Native engines and their Java bridges can fail during class or JNI
            // initialization. Keep that failure inside the TTS request so the
            // service does not silently die before the app receives its error.
            failUnexpectedRequest(request, callback, e);
        }
    }

    private void synthesizeRequest(SynthesisRequest request, SynthesisCallback callback) {
        stopped = false;
        getSharedPreferences("smart_voice_settings", MODE_PRIVATE).edit().remove("last_tts_error").apply();
        String requestedId = request.getVoiceName();
        VoiceEntry voice = findVoice(requestedId);
        if (voice == null) voice = findVoice(new VoiceStore(this).selectedVoiceId());
        if (voice == null) {
            recordError("No imported Smart Voice voice matched this Android TTS request.", null);
            callback.error(TextToSpeech.ERROR);
            return;
        }
        if ("google".equals(voice.engine)) {
            speakGoogle(voice, normalizeText(request.getText().toString(), voice), request, callback);
            return;
        }
        if ("vocalizer".equals(voice.engine)) {
            speakVocalizer(voice, normalizeText(request.getText().toString(), voice), request, callback);
            return;
        }
        if (!"microsoft".equals(voice.engine)) { recordError("No synthesis adapter is registered for engine '" + voice.engine + "'.", null); callback.error(TextToSpeech.ERROR); return; }
        // MultiTTS voice ZIPs carry the Microsoft engine parameter alongside each
        // voice. Use that imported value as-is; the encrypted user setting is only
        // a fallback for compatible Embedded Speech packages that omit it.
        String license = voice.engineParam == null || voice.engineParam.isEmpty()
                ? new VoiceStore(this).microsoftModelLicense() : voice.engineParam;
        if (license.isEmpty()) {
            recordError("The imported Microsoft voice package does not contain the model license required by Embedded Speech.", null);
            callback.error(TextToSpeech.ERROR);
            return;
        }
        try {
            SpeechSynthesizer synthesizer = getOrCreateMicrosoftSynthesizer(voice, license);
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1);
            SharedPreferences settings = getSharedPreferences("smart_voice_settings", MODE_PRIVATE);
            int rate = Math.max(10, Math.min(500, request.getSpeechRate() * settings.getInt("rate", 100) / 100));
            int pitch = Math.max(10, Math.min(500, request.getPitch() * settings.getInt("pitch", 100) / 100));
            String ssml = "<speak version=\"1.0\" xml:lang=\"" + xml(voice.locale) + "\"><prosody rate=\"" + rate + "%\" pitch=\"" + pitch + "%\">" + xml(normalizeText(request.getText().toString(), voice)) + "</prosody></speak>";
            SpeechSynthesisResult result = synthesizer.SpeakSsml(ssml);
            if (result.getReason() != ResultReason.SynthesizingAudioCompleted) {
                String diagnostic = "Microsoft Embedded Speech returned " + result.getReason();
                try {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                    try {
                        diagnostic += "; reason=" + cancellation.getReason()
                                + "; code=" + cancellation.getErrorCode()
                                + "; details=" + cancellation.getErrorDetails();
                    } finally {
                        cancellation.close();
                    }
                } catch (Exception detailsError) {
                    diagnostic += "; cancellation details unavailable: " + detailsError;
                }
                recordError("Microsoft synthesis failed for " + voice.name + ": " + diagnostic, null);
                result.close();
                callback.error(TextToSpeech.ERROR);
                return;
            }
            AudioDataStream stream = AudioDataStream.fromResult(result);
            byte[] buffer = new byte[8192];
            try {
                long size;
                while (!stopped && (size = stream.readData(buffer)) > 0) {
                    int length = (int) Math.min(size, buffer.length);
                    scalePcm16(buffer, length, settings.getInt("volume", 100));
                    if (callback.audioAvailable(buffer, 0, length) != TextToSpeech.SUCCESS) break;
                }
            } finally {
                stream.close();
                result.close();
            }
            callback.done();
        } catch (Exception e) {
            recordError("Microsoft synthesis failed for " + voice.name + ": " + e, e);
            callback.error(TextToSpeech.ERROR);
        }
    }

    private void failUnexpectedRequest(SynthesisRequest request, SynthesisCallback callback, Throwable failure) {
        String engine = "unknown";
        String voiceName = request == null ? "unknown" : request.getVoiceName();
        try {
            VoiceEntry voice = findVoice(voiceName);
            if (voice == null && request != null) voice = findVoice(new VoiceStore(this).selectedVoiceId());
            if (voice != null) {
                engine = voice.engine;
                voiceName = voice.name;
            }
        } catch (RuntimeException ignored) { }
        try {
            recordError("Unexpected TTS failure for " + engine + " voice '" + voiceName + "'.", failure);
        } catch (RuntimeException loggingFailure) {
            Log.e("SmartVoiceTTS", "Could not persist synthesis failure", loggingFailure);
            Log.e("SmartVoiceTTS", "Original synthesis failure", failure);
        }
        try {
            callback.error(TextToSpeech.ERROR);
        } catch (RuntimeException | LinkageError callbackFailure) {
            Log.e("SmartVoiceTTS", "Could not report the synthesis failure to Android", callbackFailure);
        }
    }

    private void speakGoogle(VoiceEntry voice, String text, SynthesisRequest request, SynthesisCallback callback) {
        try {
            GoogleLocalEngine google;
            synchronized (lock) {
                if (activeGoogle == null || !voice.id.equals(activeGoogleVoiceId)) {
                    if (activeGoogle != null) activeGoogle.close();
                    activeGoogle = new GoogleLocalEngine(this, voice.code, voice.gender, voice.dataPath);
                    activeGoogleVoiceId = voice.id;
                    activeGoogle.initialize();
                }
                google = activeGoogle;
            }
            SharedPreferences settings = getSharedPreferences("smart_voice_settings", MODE_PRIVATE);
            int rate = Math.max(10, Math.min(500, request.getSpeechRate() * settings.getInt("rate", 100) / 100));
            int pitch = Math.max(10, Math.min(500, request.getPitch() * settings.getInt("pitch", 100) / 100));
            google.speak(text, callback, rate, pitch, settings.getInt("volume", 100));
        } catch (Exception e) {
            recordError("Google local synthesis failed for " + voice.name + ": " + e, e);
            callback.error(TextToSpeech.ERROR);
        }
    }

    private void speakVocalizer(VoiceEntry voice, String text, SynthesisRequest request, SynthesisCallback callback) {
        try {
            VocalizerRuntime runtime;
            synchronized (lock) {
                if (activeVocalizer == null || !voice.id.equals(activeVocalizerVoiceId)) {
                    if (activeVocalizer != null) activeVocalizer.close();
                    activeVocalizer = new VocalizerRuntime();
                    activeVocalizerVoiceId = voice.id;
                }
                runtime = activeVocalizer;
            }
            SharedPreferences settings = getSharedPreferences("smart_voice_settings", MODE_PRIVATE);
            float rate = Math.max(0.5f, Math.min(2.0f,
                    (request.getSpeechRate() / 100.0f) * (settings.getInt("rate", 100) / 100.0f)));
            float pitch = Math.max(0.5f, Math.min(2.0f,
                    (request.getPitch() / 100.0f) * (settings.getInt("pitch", 100) / 100.0f)));
            callback.start(22050, AudioFormat.ENCODING_PCM_16BIT, 1);
            runtime.speak(voice, text, rate, pitch, settings.getInt("volume", 100), samples -> {
                if (stopped) return false;
                byte[] pcm = new byte[samples.length * 2];
                for (int i = 0; i < samples.length; i++) {
                    pcm[i * 2] = (byte) (samples[i] & 0xff);
                    pcm[i * 2 + 1] = (byte) (samples[i] >>> 8);
                }
                return callback.audioAvailable(pcm, 0, pcm.length) == TextToSpeech.SUCCESS;
            });
            if (stopped) callback.error(TextToSpeech.ERROR); else callback.done();
        } catch (Exception e) {
            recordError("Vocalizer synthesis failed for " + voice.name + ": " + e, e);
            callback.error(TextToSpeech.ERROR);
        }
    }

    private void recordError(String message, Throwable error) {
        String details = error == null ? message : message + "\n" + Log.getStackTraceString(error);
        Log.e("SmartVoiceTTS", message, error);
        getSharedPreferences("smart_voice_settings", MODE_PRIVATE).edit().putString("last_tts_error", details).apply();
    }

    private SpeechSynthesizer getOrCreateMicrosoftSynthesizer(VoiceEntry voice, String license) throws Exception {
        synchronized (lock) {
            if (activeSynthesizer != null && voice.id.equals(activeVoiceId)) return activeSynthesizer;
            releaseActive();
            File root = new File(voice.dataPath);
            if (!root.isDirectory()) throw new IllegalStateException("Imported Microsoft model folder is missing");
            EmbeddedSpeechConfig config = EmbeddedSpeechConfig.fromPath(root.getAbsolutePath());
            config.setSpeechSynthesisVoice(voice.code, license);
            config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm);
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, null);
            activeConfig = config;
            activeSynthesizer = synthesizer;
            activeVoiceId = voice.id;
            return synthesizer;
        }
    }

    private VoiceEntry findVoice(String id) {
        if (id == null) return null;
        for (VoiceEntry v : new VoiceStore(this).list()) if (v.id.equals(id)) return v;
        return null;
    }

    private String normalizeText(String input, VoiceEntry voice) {
        String text = input == null ? "" : input;
        SharedPreferences settings = getSharedPreferences("smart_voice_settings", MODE_PRIVATE);
        String voiceLanguage = voice == null ? settings.getString("ui_language", "en") : voice.locale;
        text = EmojiTable.normalize(this, text, voiceLanguage, settings.getBoolean("read_emojis", true));
        try {
            org.json.JSONArray words = new org.json.JSONArray(settings.getString("dictionary", "[]"));
            for (int i = 0; i < words.length(); i++) {
                org.json.JSONObject entry = words.optJSONObject(i); if (entry == null) continue;
                String word = entry.optString("word"), pronunciation = entry.optString("pronunciation");
                if (!word.isEmpty() && !pronunciation.isEmpty()) text = text.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", Matcher.quoteReplacement(pronunciation));
            }
        } catch (Exception ignored) { }
        return text;
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void scalePcm16(byte[] pcm, int length, int volumePercent) {
        if (volumePercent == 100) return;
        float scale = Math.max(0, Math.min(150, volumePercent)) / 100.0f;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * scale)));
            pcm[i] = (byte) scaled; pcm[i + 1] = (byte) (scaled >>> 8);
        }
    }

    private void releaseActive() {
        if (activeSynthesizer != null) {
            try { activeSynthesizer.close(); } catch (Exception ignored) { }
            activeSynthesizer = null;
        }
        if (activeConfig != null) {
            try { activeConfig.close(); } catch (Exception ignored) { }
            activeConfig = null;
        }
        activeVoiceId = "";
    }

    @Override public void onDestroy() {
        synchronized (lock) {
            releaseActive();
            if (activeGoogle != null) { activeGoogle.close(); activeGoogle = null; activeGoogleVoiceId = ""; }
            if (activeVocalizer != null) { activeVocalizer.close(); activeVocalizer = null; activeVocalizerVoiceId = ""; }
        }
        super.onDestroy();
    }
}
