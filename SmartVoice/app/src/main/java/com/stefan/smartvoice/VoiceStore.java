package com.stefan.smartvoice;

import android.content.Context;
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
            if (descriptor == null) throw new IllegalArgumentException("This ZIP does not contain a supported Smart Voice voice descriptor");
            List<VoiceEntry> parsed = parseDescriptor(descriptor, packageId);
            if (parsed.isEmpty()) throw new IllegalArgumentException("The package contains no readable voice entries");
            File descriptorRoot = descriptor.getParentFile();
            for (VoiceEntry voice : parsed) {
                File voiceRoot = findVoiceFolder(descriptorRoot, voice.engine, voice.locale, voice.code);
                if (voiceRoot == null && voice.engine.equals("google")) voiceRoot = descriptorRoot;
                if (voiceRoot == null && voice.engine.equals("microsoft")) {
                    throw new IllegalArgumentException("Microsoft voice folder is missing for " + voice.name);
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
        if (!engine.equals("microsoft") && !engine.equals("google")) {
            String engines = readSibling(file.getParentFile(), "engines.yaml");
            if (engines.toLowerCase(Locale.ROOT).contains("code: microsoft")) engine = "microsoft";
            else if (engines.toLowerCase(Locale.ROOT).contains("code: google")) engine = "google";
        }
        if (!engine.equals("microsoft") && !engine.equals("google")) return new ArrayList<>();

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
