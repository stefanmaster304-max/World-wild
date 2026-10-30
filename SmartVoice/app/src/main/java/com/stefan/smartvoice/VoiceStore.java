package com.stefan.smartvoice;

import android.content.Context;
import android.os.Build;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class VoiceStore {
    private static final String PREFS = "voice_store";
    private static final String KEY_VOICES = "voices_json";
    private static final long MAX_EXPANDED_BYTES = 3L * 1024 * 1024 * 1024;
    private static final int MAX_ENTRIES = 100_000;

    private final Context context;
    private final SharedPreferences prefs;
    private final LicenseVault licenseVault;

    public VoiceStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.licenseVault = new LicenseVault(this.context);
    }

    public synchronized List<VoiceEntry> list() {
        ArrayList<VoiceEntry> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_VOICES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                VoiceEntry v = new VoiceEntry();
                v.id = o.optString("id");
                v.packageId = o.optString("packageId");
                v.engine = o.optString("engine");
                v.name = o.optString("name");
                v.code = o.optString("code");
                v.locale = o.optString("locale");
                v.variant = o.optString("variant");
                v.dataPath = o.optString("dataPath");
                v.engineParam = licenseVault.readSecret(v.id);
                v.gender = o.optInt("gender", -1);
                if (!v.id.isEmpty() && new File(v.dataPath).isDirectory()) result.add(v);
            }
        } catch (Exception ignored) {
            // An unreadable registry is treated as empty; imported data is left untouched.
        }
        return result;
    }

    public String selectedVoiceId() { return prefs.getString("selected_voice", ""); }
    public void setSelectedVoiceId(String id) { prefs.edit().putString("selected_voice", id == null ? "" : id).apply(); }
    public String microsoftModelLicense() { return licenseVault.read(); }
    public void setMicrosoftModelLicense(String value) {
        try { licenseVault.save(value == null ? "" : value.trim()); }
        catch (Exception e) { throw new IllegalStateException("Could not protect the Microsoft model license on this device", e); }
    }

    public synchronized List<VoiceEntry> importZip(Uri uri) throws Exception {
        return importZip(uri, new AtomicBoolean(false));
    }

    public synchronized List<VoiceEntry> importZip(Uri uri, AtomicBoolean cancelled) throws Exception {
        String packageId = UUID.randomUUID().toString();
        File base = new File(context.getFilesDir(), "voice-packages/" + packageId);
        File staging = new File(context.getFilesDir(), "voice-packages/.import-" + packageId);
        deleteTree(staging);
        if (!staging.mkdirs()) throw new IllegalStateException("Could not create import directory");

        long expanded = 0;
        int count = 0;
        InputStream opened = context.getContentResolver().openInputStream(uri);
        if (opened == null) {
            deleteTree(staging);
            throw new IllegalArgumentException("The selected file could not be opened");
        }
        try (InputStream raw = opened; ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                if (cancelled.get()) throw new java.util.concurrent.CancellationException("Voice package import cancelled");
                if (++count > MAX_ENTRIES) throw new IllegalArgumentException("Archive contains too many files");
                File out = safeChild(staging, entry.getName());
                if (entry.isDirectory()) {
                    if (!out.isDirectory() && !out.mkdirs()) throw new IllegalStateException("Could not create package folder");
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IllegalStateException("Could not create package folder");
                    try (FileOutputStream stream = new FileOutputStream(out)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            if (cancelled.get()) throw new java.util.concurrent.CancellationException("Voice package import cancelled");
                            expanded += n;
                            if (expanded > MAX_EXPANDED_BYTES) throw new IllegalArgumentException("Expanded package exceeds the 3 GB safety limit");
                            stream.write(buffer, 0, n);
                        }
                    }
                }
                zip.closeEntry();
            }
            if (count == 0) throw new IllegalArgumentException("The selected ZIP is empty or invalid");

            File descriptor = findDescriptor(staging);
            List<VoiceEntry> parsed;
            File descriptorRoot;
            boolean accessMindLayout;
            if (descriptor != null) {
                accessMindLayout = false;
                descriptorRoot = descriptor.getParentFile();
                parsed = parseDescriptor(descriptor, packageId);
            } else {
                accessMindLayout = true;
                descriptorRoot = staging;
                parsed = parseAccessMindVoice(staging, packageId);
            }
            if (parsed.isEmpty()) {
                String declared = descriptor == null ? "" : declaredEngine(descriptor);
                if (!declared.isEmpty()) throw new IllegalArgumentException("Engine not integrated yet: " + declared);
                throw new IllegalArgumentException("This ZIP is not a supported Microsoft, Google, or Vocalizer voice package. It needs a valid config.yaml/engines.yaml descriptor or AccessMind Vocalizer .dat and .hdr files.");
            }
            for (VoiceEntry voice : parsed) {
                requireRuntimeAbi(voice.engine);
                File voiceRoot = findVoiceFolder(descriptorRoot, voice.engine, voice.locale, voice.code);
                if (voiceRoot == null && voice.engine.equals("google")) voiceRoot = descriptorRoot;
                if (voiceRoot == null && voice.engine.equals("microsoft")) {
                    throw new IllegalArgumentException("Microsoft voice folder is missing for " + voice.name);
                }
                if (voice.engine.equals("vocalizer")) {
                    if (accessMindLayout) voiceRoot = prepareAccessMindRuntimeFolder(staging, voice);
                    if (!hasVocalizerModel(voiceRoot == null ? descriptorRoot : voiceRoot)) {
                        voiceRoot = findVocalizerModelRoot(staging, voice.code);
                    }
                    if (voiceRoot == null) throw new IllegalArgumentException("Vocalizer package is missing its .dat model or .hdr voice descriptor for " + voice.name);
                }
                if (voice.engine.equals("microsoft") && !hasMicrosoftModel(voiceRoot)) {
                    throw new IllegalArgumentException("Microsoft package is missing the per-voice .APM model and .INI metadata for " + voice.name);
                }
                if (voice.engine.equals("google") && !hasGoogleModel(voiceRoot)) {
                    throw new IllegalArgumentException("Google package is missing pipeline.pb or backend_pipeline.textproto for " + voice.name);
                }
                voice.dataPath = (voiceRoot == null ? descriptorRoot : voiceRoot).getAbsolutePath();
            }
            if (base.exists()) deleteTree(base);
            if (!staging.renameTo(base)) throw new IllegalStateException("Could not finalize imported package");
            // Paths are reassigned after the staging folder has been renamed.
            for (VoiceEntry voice : parsed) {
                String relative = voice.dataPath.substring(staging.getAbsolutePath().length());
                voice.dataPath = base.getAbsolutePath() + relative;
            }
            List<VoiceEntry> all = list();
            all.addAll(parsed);
            save(all);
            return parsed;
        } catch (Exception e) {
            deleteTree(staging);
            throw e;
        }
    }

    private static void requireRuntimeAbi(String engine) {
        if ("google".equals(engine) || "vocalizer".equals(engine)) {
            boolean arm64 = false;
            for (String abi : Build.SUPPORTED_ABIS) if ("arm64-v8a".equalsIgnoreCase(abi)) arm64 = true;
            if (!arm64) throw new IllegalArgumentException("Engine is not available on this device architecture: " + engine + " requires ARM64 Android libraries.");
        }
    }

    private File findDescriptor(File root) {
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File nested = findDescriptor(f);
                if (nested != null) return nested;
            } else if (f.getName().equalsIgnoreCase("config.yaml")) {
                return f;
            }
        }
        return null;
    }

    private List<VoiceEntry> parseDescriptor(File file, String packageId) throws Exception {
        String yaml;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder();
            char[] chars = new char[8192];
            int n;
            while ((n = reader.read(chars)) >= 0) b.append(chars, 0, n);
            yaml = b.toString();
        }
        Matcher engineMatcher = Pattern.compile("(?m)^([A-Za-z0-9_-]+):\\s*$").matcher(yaml);
        String engine = "";
        if (engineMatcher.find()) engine = engineMatcher.group(1).toLowerCase(Locale.ROOT);
        if (!engine.equals("microsoft") && !engine.equals("google") && !engine.equals("vocalizer")) {
            String engines = readSibling(file.getParentFile(), "engines.yaml");
            if (engines.toLowerCase(Locale.ROOT).contains("code: microsoft")) engine = "microsoft";
            else if (engines.toLowerCase(Locale.ROOT).contains("code: google")) engine = "google";
            else if (engines.toLowerCase(Locale.ROOT).contains("code: vocalizer")) engine = "vocalizer";
        }
        if (!engine.equals("microsoft") && !engine.equals("google") && !engine.equals("vocalizer")) return new ArrayList<>();

        ArrayList<VoiceEntry> voices = new ArrayList<>();
        String[] lines = yaml.split("\\R", -1);
        String name = null, code = null, locale = null, engineParam = "";
        int gender = -1;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String t = line.trim();
            if (t.startsWith("- ")) {
                if (name != null && code != null && locale != null) voices.add(makeVoice(packageId, engine, name, code, locale, gender, engineParam));
                name = code = locale = null;
                engineParam = "";
                gender = -1;
            }
            if (t.startsWith("name:")) name = scalar(t.substring(5));
            else if (t.startsWith("code:")) code = scalar(t.substring(5));
            else if (t.startsWith("locale:")) locale = scalar(t.substring(7));
            else if (t.startsWith("param:")) {
                String value = t.substring(6).trim();
                if (value.equals("|-") || value.equals("|") || value.equals(">-") || value.equals(">")) {
                    StringBuilder block = new StringBuilder();
                    int keyIndent = line.length() - line.stripLeading().length();
                    while (index + 1 < lines.length) {
                        String next = lines[index + 1];
                        if (next.trim().isEmpty()) { index++; continue; }
                        int nextIndent = next.length() - next.stripLeading().length();
                        if (nextIndent <= keyIndent) break;
                        index++;
                        if (block.length() > 0) block.append('\n');
                        block.append(next.trim());
                    }
                    engineParam = block.toString();
                } else engineParam = scalar(value);
            }
            else if (t.startsWith("gender:")) {
                try { gender = Integer.parseInt(scalar(t.substring(7))); } catch (Exception ignored) { gender = -1; }
            }
        }
        if (name != null && code != null && locale != null) voices.add(makeVoice(packageId, engine, name, code, locale, gender, engineParam));
        return voices;
    }

    private static String declaredEngine(File descriptor) {
        try {
            String yaml = readSibling(descriptor.getParentFile(), descriptor.getName()).toLowerCase(Locale.ROOT);
            Matcher key = Pattern.compile("(?m)^([a-z0-9_-]+):\\s*$").matcher(yaml);
            if (key.find()) return key.group(1);
            String engines = readSibling(descriptor.getParentFile(), "engines.yaml").toLowerCase(Locale.ROOT);
            Matcher code = Pattern.compile("(?m)^\\s*-?\\s*code:\\s*['\"]?([a-z0-9_-]+)").matcher(engines);
            return code.find() ? code.group(1) : "";
        } catch (Exception ignored) { return ""; }
    }

    private static boolean hasMicrosoftModel(File root) {
        if (root == null || !root.isDirectory()) return false;
        boolean apm = false, ini = false;
        ArrayList<File> files = new ArrayList<>(); collectFiles(root, ".apm", files); apm = !files.isEmpty();
        files.clear(); collectFiles(root, ".ini", files); ini = !files.isEmpty();
        return apm && ini;
    }

    private static boolean hasGoogleModel(File root) {
        return root != null && root.isDirectory()
                && new File(root, "pipeline.pb").isFile()
                && new File(root, "backend_pipeline.textproto").isFile();
    }

    /**
     * AccessMind's public Vocalizer downloads omit MultiTTS's YAML descriptor:
     * each ZIP has a locale tree with one model .dat and one XML .hdr file.
     * The header itself supplies the voice name, locale, model quality and rate.
     */
    private List<VoiceEntry> parseAccessMindVoice(File root, String packageId) throws Exception {
        ArrayList<VoiceEntry> voices = new ArrayList<>();
        ArrayList<File> headers = new ArrayList<>();
        collectFiles(root, ".hdr", headers);
        for (File header : headers) {
            String xml = readSibling(header.getParentFile(), header.getName());
            String speaker = xmlTag(xml, "voice");
            String locale = xmlTag(xml, "langietf");
            String quality = xmlTag(xml, "voiceoperatingpoint");
            String code = xmlTag(xml, "langcode");
            if (speaker.isEmpty() || locale.isEmpty() || quality.isEmpty()) continue;
            File modelRoot = findAccessMindModelRoot(root, header);
            if (modelRoot == null) continue;
            VoiceEntry v = makeVoice(packageId, "vocalizer", speaker, speaker, locale, "female".equalsIgnoreCase(xmlTag(xml, "gender")) ? 0 : 1, "");
            v.variant = quality;
            v.dataPath = modelRoot.getAbsolutePath();
            voices.add(v);
        }
        return voices;
    }

    private static String xmlTag(String xml, String tag) {
        Matcher m = Pattern.compile("(?is)<" + Pattern.quote(tag) + ">\\s*([^<]+?)\\s*</" + Pattern.quote(tag) + ">").matcher(xml);
        return m.find() ? m.group(1).trim() : "";
    }

    private static void collectFiles(File dir, String suffix, List<File> result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collectFiles(child, suffix, result);
            else if (child.getName().toLowerCase(Locale.ROOT).endsWith(suffix)) result.add(child);
        }
    }

    private static File findAccessMindModelRoot(File root, File header) {
        File folder = header.getParentFile();
        File[] siblings = folder == null ? null : folder.listFiles();
        if (siblings != null) {
            for (File sibling : siblings) if (sibling.isFile() && sibling.getName().toLowerCase(Locale.ROOT).endsWith(".dat")) return folder;
        }
        // AccessMind's catalog layout puts the model under speech/components and
        // its header under speech/ve. The shared speech folder is the runtime root.
        File ve = header.getParentFile();
        File speech = ve == null ? null : ve.getParentFile();
        if (speech != null && "ve".equalsIgnoreCase(ve.getName())) {
            File components = new File(speech, "components");
            if (components.isDirectory()) {
                File[] models = components.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".dat"));
                if (models != null && models.length > 0) return speech;
            }
        }
        return null;
    }

    private static boolean hasVocalizerModel(File root) {
        if (root == null || !root.isDirectory()) return false;
        ArrayList<File> files = new ArrayList<>();
        collectFiles(root, ".hdr", files);
        if (files.isEmpty()) return false;
        return findAccessMindModelRoot(root, files.get(0)) != null || hasSiblingModel(files.get(0));
    }

    private static boolean hasSiblingModel(File header) {
        File[] siblings = header.getParentFile() == null ? null : header.getParentFile().listFiles();
        if (siblings == null) return false;
        for (File f : siblings) if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".dat")) return true;
        return false;
    }

    private static File findVocalizerModelRoot(File root, String code) {
        ArrayList<File> headers = new ArrayList<>();
        collectFiles(root, ".hdr", headers);
        for (File header : headers) {
            String xml = readSibling(header.getParentFile(), header.getName());
            if (xmlTag(xml, "voice").equalsIgnoreCase(code)) return findAccessMindModelRoot(root, header);
        }
        return headers.isEmpty() ? null : findAccessMindModelRoot(root, headers.get(0));
    }

    /** Places AccessMind's split .dat/.hdr files beside each other as the embedded runtime expects. */
    private static File prepareAccessMindRuntimeFolder(File root, VoiceEntry voice) throws Exception {
        ArrayList<File> headers = new ArrayList<>(), models = new ArrayList<>();
        collectFiles(root, ".hdr", headers); collectFiles(root, ".dat", models);
        File header = null;
        for (File candidate : headers) {
            String xml = readSibling(candidate.getParentFile(), candidate.getName());
            if (xmlTag(xml, "voice").equalsIgnoreCase(voice.name)) { header = candidate; break; }
        }
        if (header == null) throw new IllegalArgumentException("AccessMind package is missing the Vocalizer .hdr file for " + voice.name);
        File model = null;
        for (File candidate : models) {
            if (candidate.getName().toLowerCase(Locale.ROOT).contains(voice.name.toLowerCase(Locale.ROOT))) { model = candidate; break; }
        }
        if (model == null && models.size() == 1) model = models.get(0);
        if (model == null) throw new IllegalArgumentException("AccessMind package is missing a matching Vocalizer .dat model for " + voice.name);
        File target = new File(root, "vocalizer/" + voice.code.replaceAll("[^A-Za-z0-9._-]", "_"));
        if (!target.isDirectory() && !target.mkdirs()) throw new IllegalStateException("Could not prepare the AccessMind voice directory");
        try (InputStream in = new java.io.FileInputStream(header);
             FileOutputStream out = new FileOutputStream(new File(target, header.getName()))) {
            byte[] buffer = new byte[32 * 1024]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        try (InputStream in = new java.io.FileInputStream(model);
             FileOutputStream out = new FileOutputStream(new File(target, model.getName()))) {
            byte[] buffer = new byte[64 * 1024]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        return target;
    }

    private VoiceEntry makeVoice(String packageId, String engine, String name, String code, String locale, int gender, String engineParam) {
        VoiceEntry v = new VoiceEntry();
        v.id = packageId + ":" + engine + ":" + code;
        v.packageId = packageId;
        v.engine = engine;
        v.name = name;
        v.code = code;
        v.locale = locale;
        String modelInfo = (code + " " + name).toLowerCase(Locale.ROOT);
        v.variant = engine.equals("microsoft") && modelInfo.contains("neural") ? "neural"
                : engine.equals("microsoft") && modelInfo.contains("apollo") ? "Apollo" : "";
        v.gender = gender;
        v.engineParam = engineParam == null ? "" : engineParam;
        return v;
    }

    private File findVoiceFolder(File root, String engine, String locale, String code) {
        String norm = code.toLowerCase(Locale.ROOT);
        String shortSpeaker = norm.contains("-x-") ? norm.substring(norm.lastIndexOf("-x-") + 3) : norm;
        File engineRoot = new File(root, engine);
        if (!engineRoot.isDirectory()) return null;
        ArrayList<File> queue = new ArrayList<>();
        ArrayList<File> candidates = new ArrayList<>();
        queue.add(engineRoot);
        for (int i = 0; i < queue.size(); i++) {
            File d = queue.get(i);
            if (d.getName().equalsIgnoreCase(norm) || d.getName().toLowerCase(Locale.ROOT).contains(norm)) return d;
            File[] children = d.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (!child.isDirectory()) continue;
                if (new File(child, "pipeline.pb").isFile() && new File(child, "backend_pipeline.textproto").isFile()) candidates.add(child);
                else queue.add(child);
            }
        }
        candidates.sort((a, b) -> a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath()));
        for (File candidate : candidates) {
            String backend = readSibling(candidate, "backend_pipeline.textproto");
            if (backend.matches("(?s).*speaker\\s*:\\s*\\\"" + Pattern.quote(shortSpeaker) + "\\\".*")) return candidate;
        }
        // Some Google voice packs have one shared model for all IDs in that locale.
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private static String readSibling(File root, String name) {
        try {
            File f = new File(root, name);
            byte[] b = new byte[(int) Math.min(f.length(), 64 * 1024)];
            try (InputStream in = new java.io.FileInputStream(f)) { int n = in.read(b); return n < 0 ? "" : new String(b, 0, n, StandardCharsets.UTF_8); }
        } catch (Exception ignored) { return ""; }
    }

    private static String scalar(String value) {
        String v = value.trim();
        if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) v = v.substring(1, v.length() - 1);
        return v;
    }

    private static File safeChild(File root, String name) throws Exception {
        if (name.startsWith("/") || name.startsWith("\\") || name.indexOf('\0') >= 0) throw new IllegalArgumentException("Unsafe ZIP path");
        File child = new File(root, name);
        String base = root.getCanonicalPath() + File.separator;
        if (!child.getCanonicalPath().startsWith(base) && !child.getCanonicalPath().equals(root.getCanonicalPath())) throw new IllegalArgumentException("Unsafe ZIP path");
        return child;
    }

    private synchronized void save(List<VoiceEntry> voices) throws Exception {
        JSONArray arr = new JSONArray();
        for (VoiceEntry v : voices) {
            try { licenseVault.saveSecret(v.id, v.engineParam == null ? "" : v.engineParam); }
            catch (Exception e) { throw new IllegalStateException("Could not protect imported engine parameter", e); }
            JSONObject o = new JSONObject();
            o.put("id", v.id); o.put("packageId", v.packageId); o.put("engine", v.engine);
            o.put("name", v.name); o.put("code", v.code); o.put("locale", v.locale);
            o.put("variant", v.variant); o.put("dataPath", v.dataPath); o.put("gender", v.gender);
            arr.put(o);
        }
        prefs.edit().putString(KEY_VOICES, arr.toString()).apply();
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
