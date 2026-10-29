package com.stefan.smartvoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int PICK_PACKAGE = 100, PICK_DICTIONARY = 101, EXPORT_DICTIONARY = 102;
    private VoiceStore store;
    private android.content.SharedPreferences prefs;
    private TextToSpeech tts;
    private TextView selected, status;
    private EditText sample;
    private String selectedVoiceId = "", screen = "home";
    private ProgressDialog importDialog;
    private AtomicBoolean cancelImport;
    private List<VoiceEntry> dictionaryVoices = Collections.emptyList();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new VoiceStore(this);
        prefs = getSharedPreferences("smart_voice_settings", MODE_PRIVATE);
        selectedVoiceId = store.selectedVoiceId();
        tts = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) configureTts(); }, getPackageName());
        show(prefs.getBoolean("welcome_completed", false) ? "home" : "welcome");
    }

    private void configureTts() {
        tts.setEngineByPackageName(getPackageName());
        // Smart Voice applies its own controls inside the service. Keep this
        // preview client neutral to avoid applying the multipliers twice.
        tts.setSpeechRate(1.0f);
        tts.setPitch(1.0f);
        VoiceEntry voice = selectedVoice();
        if (voice != null) tts.setVoice(new android.speech.tts.Voice(voice.id, voice.toLocale(), 400, 400, false, Collections.emptySet()));
    }

    private void show(String target) {
        screen = target;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(28));
        scroll.addView(root);
        setContentView(scroll);
        if (target.equals("welcome")) { welcome(root); return; }
        heading(root, target.equals("dictionary") ? tr("dictionary") : target.equals("settings") ? tr("settings") : "Smart Voice");
        if (target.equals("home")) home(root);
        else if (target.equals("settings")) settings(root);
        else if (target.equals("dictionary")) dictionary(root);
    }

    private void welcome(LinearLayout root) {
        heading(root, "Smart Voice");
        label(root, tr("version") + " 0.0.1 · " + tr("developer") + " Stefan", 16);
        label(root, tr("welcome_text"), 18);
        label(root, tr("welcome_engines"), 16);
        button(root, tr("interface_language"), this::chooseLanguage);
        button(root, tr("done"), () -> { prefs.edit().putBoolean("welcome_completed", true).apply(); show("home"); });
    }

    private void home(LinearLayout root) {
        label(root, tr("version") + " 0.0.1 · " + tr("developer") + " Stefan", 14);
        button(root, tr("import_package"), this::choosePackage);
        selected = label(root, "", 18);
        button(root, tr("select_voice"), this::chooseVoice);
        sample = new EditText(this);
        sample.setHint(tr("sample_hint")); sample.setMinLines(2); sample.setText(tr("sample_text"));
        sample.setContentDescription(tr("sample_hint")); root.addView(sample, matchWrap());
        button(root, tr("speak_test"), this::speakSample);
        label(root, tr("speech_controls"), 20);
        addSlider(root, "volume", 0, 150, 5, prefs.getInt("volume", 100), "%", "volume");
        addSlider(root, "pitch", 50, 200, 5, prefs.getInt("pitch", 100), "%", "pitch");
        addSlider(root, "rate", 50, 200, 5, prefs.getInt("rate", 100), "%", "rate");
        label(root, tr("custom_settings"), 20);
        CheckBox emoji = new CheckBox(this); emoji.setText(tr("read_emojis"));
        emoji.setChecked(prefs.getBoolean("read_emojis", true));
        emoji.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean("read_emojis", checked).apply()); root.addView(emoji, matchWrap());
        button(root, tr("dictionary"), () -> show("dictionary"));
        button(root, tr("settings"), () -> show("settings"));
        status = label(root, "", 14); refresh();
    }

    private void settings(LinearLayout root) {
        button(root, tr("interface_language"), this::chooseLanguage);
        button(root, tr("system_tts_settings"), () -> openSystemSettings("android.settings.TTS_SETTINGS"));
        button(root, tr("accessibility_settings"), () -> openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        button(root, tr("dictionary_import"), () -> pickFile(PICK_DICTIONARY, "text/*"));
        button(root, tr("dictionary_export"), () -> pickFile(EXPORT_DICTIONARY, "text/plain"));
        button(root, tr("dictionary_clear"), this::clearDictionary);
        button(root, tr("back"), () -> show("home"));
        label(root, "Smart Voice 0.0.1 · Stefan", 14);
    }

    private void dictionary(LinearLayout root) {
        button(root, tr("dictionary_add"), () -> editWord(-1, null, null));
        JSONArray words = readDictionary();
        if (words.length() == 0) label(root, tr("dictionary_empty"), 16);
        for (int i = 0; i < words.length(); i++) {
            JSONObject item = words.optJSONObject(i); if (item == null) continue;
            final int index = i;
            String word = item.optString("word"), pronunciation = item.optString("pronunciation");
            button(root, word + (pronunciation.isEmpty() ? "" : " → " + pronunciation), () -> new AlertDialog.Builder(this)
                    .setTitle(word).setItems(new String[]{tr("edit"), tr("delete")}, (d, choice) -> {
                        if (choice == 0) editWord(index, word, pronunciation); else deleteWord(index);
                    }).show());
        }
        button(root, tr("dictionary_import"), () -> pickFile(PICK_DICTIONARY, "text/*"));
        button(root, tr("dictionary_export"), () -> pickFile(EXPORT_DICTIONARY, "text/plain"));
        button(root, tr("back"), () -> show("home"));
    }

    private void addSlider(LinearLayout parent, String labelKey, int min, int max, int step, int current, String suffix, String prefKey) {
        TextView title = label(parent, tr(labelKey) + ": " + current + suffix, 16);
        SeekBar seek = new SeekBar(this); seek.setMax((max - min) / step); seek.setProgress(Math.max(0, Math.min(seek.getMax(), (current - min) / step)));
        seek.setContentDescription(tr(labelKey));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                int actual = min + value * step; title.setText(tr(labelKey) + ": " + actual + suffix);
                if (fromUser) { prefs.edit().putInt(prefKey, actual).apply(); if (tts != null) configureTts(); }
            }
            public void onStartTrackingTouch(SeekBar b) { }
            public void onStopTrackingTouch(SeekBar b) { }
        });
        parent.addView(seek, matchWrap());
    }

    private void choosePackage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_PACKAGE);
    }

    private void pickFile(int request, String mime) {
        Intent intent = new Intent(request == EXPORT_DICTIONARY ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (request == PICK_DICTIONARY) { intent.setType("*/*"); intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "application/octet-stream"}); }
        else intent.setType(mime);
        if (request == EXPORT_DICTIONARY) intent.putExtra(Intent.EXTRA_TITLE, "smart-voice-dictionary.txt");
        startActivityForResult(intent, request);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == PICK_PACKAGE) importPackage(uri);
        else if (request == PICK_DICTIONARY) importDictionary(uri);
        else if (request == EXPORT_DICTIONARY) exportDictionary(uri);
    }

    private void importPackage(Uri uri) {
        cancelImport = new AtomicBoolean(false);
        importDialog = new ProgressDialog(this); importDialog.setTitle(tr("importing")); importDialog.setMessage(tr("please_wait"));
        importDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); importDialog.setCancelable(false);
        importDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, tr("cancel"), (d, w) -> cancelImport.set(true)); importDialog.show();
        new Thread(() -> {
            try {
                List<VoiceEntry> imported = store.importZip(uri, cancelImport);
                runOnUiThread(() -> {
                    if (importDialog != null) importDialog.dismiss();
                    if (!imported.isEmpty() && selectedVoiceId.isEmpty()) select(imported.get(0));
                    Toast.makeText(this, tr("imported") + " " + imported.size(), Toast.LENGTH_LONG).show(); show("home");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (importDialog != null) importDialog.dismiss();
                    Toast.makeText(this, safeMessage(e), Toast.LENGTH_LONG).show();
                });
            }
        }, "voice-package-import").start();
    }

    private void chooseVoice() {
        List<VoiceEntry> voices = store.list();
        if (voices.isEmpty()) { new AlertDialog.Builder(this).setMessage(tr("no_voices")).setPositiveButton(tr("ok"), null).show(); return; }
        dictionaryVoices = voices;
        String[] labels = new String[voices.size()];
        for (int i = 0; i < voices.size(); i++) {
            VoiceEntry v = voices.get(i); String variant = v.variant == null || v.variant.isEmpty() ? "" : " · " + v.variant;
            labels[i] = engineName(v.engine) + " · " + v.name + " · " + v.locale + variant;
        }
        new AlertDialog.Builder(this).setTitle(tr("select_voice")).setItems(labels, (dialog, which) -> { select(voices.get(which)); show("home"); }).show();
    }

    private void select(VoiceEntry voice) {
        selectedVoiceId = voice.id; store.setSelectedVoiceId(voice.id); if (tts != null) configureTts(); refresh();
    }

    private void speakSample() {
        if (selectedVoice() == null) { Toast.makeText(this, tr("no_voices"), Toast.LENGTH_SHORT).show(); return; }
        if (tts == null) { Toast.makeText(this, tr("tts_starting"), Toast.LENGTH_SHORT).show(); return; }
        tts.speak(sample.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, "smartvoice-test");
    }

    private void chooseLanguage() {
        String[] labels = {"English", "Português", "Español"};
        new AlertDialog.Builder(this).setTitle(tr("interface_language")).setItems(labels, (d, which) -> {
            prefs.edit().putString("ui_language", new String[]{"en", "pt", "es"}[which]).apply(); show(screen);
        }).show();
    }

    private void openSystemSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception e) { Toast.makeText(this, tr("settings_unavailable"), Toast.LENGTH_SHORT).show(); }
    }

    private void editWord(int index, String initialWord, String initialPronunciation) {
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL);
        EditText word = new EditText(this); word.setHint(tr("word")); if (initialWord != null) word.setText(initialWord); fields.addView(word, matchWrap());
        EditText pronunciation = new EditText(this); pronunciation.setHint(tr("pronunciation")); if (initialPronunciation != null) pronunciation.setText(initialPronunciation); fields.addView(pronunciation, matchWrap());
        new AlertDialog.Builder(this).setTitle(tr("dictionary")).setView(fields).setNegativeButton(tr("cancel"), null).setPositiveButton(tr("save"), (d, w) -> {
            String key = word.getText().toString().trim(); if (key.isEmpty()) return;
            JSONArray items = readDictionary(); JSONObject item = new JSONObject();
            try { item.put("word", key); item.put("pronunciation", pronunciation.getText().toString().trim());
                if (index >= 0 && index < items.length()) items.put(index, item); else items.put(item); saveDictionary(items); show("dictionary");
            } catch (Exception ignored) { }
        }).show();
    }

    private void deleteWord(int index) {
        JSONArray old = readDictionary(), next = new JSONArray();
        for (int i = 0; i < old.length(); i++) if (i != index) next.put(old.opt(i));
        saveDictionary(next); show("dictionary");
    }

    private void clearDictionary() {
        new AlertDialog.Builder(this).setMessage(tr("confirm_clear")).setNegativeButton(tr("cancel"), null)
                .setPositiveButton(tr("delete"), (d, w) -> { saveDictionary(new JSONArray()); Toast.makeText(this, tr("done"), Toast.LENGTH_SHORT).show(); }).show();
    }

    private void importDictionary(Uri uri) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
            JSONArray items = readDictionary(); String line;
            while ((line = reader.readLine()) != null) {
                int sep = line.indexOf('='); if (sep < 1) continue;
                JSONObject item = new JSONObject(); item.put("word", line.substring(0, sep).trim()); item.put("pronunciation", line.substring(sep + 1).trim()); items.put(item);
            }
            saveDictionary(items); Toast.makeText(this, tr("dictionary_imported"), Toast.LENGTH_LONG).show(); show(screen);
        } catch (Exception e) { Toast.makeText(this, safeMessage(e), Toast.LENGTH_LONG).show(); }
    }

    private void exportDictionary(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            JSONArray items = readDictionary(); StringBuilder text = new StringBuilder();
            for (int i = 0; i < items.length(); i++) { JSONObject item = items.optJSONObject(i); if (item != null) text.append(item.optString("word")).append('=').append(item.optString("pronunciation")).append('\n'); }
            out.write(text.toString().getBytes(StandardCharsets.UTF_8)); Toast.makeText(this, tr("dictionary_exported"), Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, safeMessage(e), Toast.LENGTH_LONG).show(); }
    }

    private JSONArray readDictionary() { try { return new JSONArray(prefs.getString("dictionary", "[]")); } catch (Exception e) { return new JSONArray(); } }
    private void saveDictionary(JSONArray value) { prefs.edit().putString("dictionary", value.toString()).apply(); }

    private void refresh() {
        VoiceEntry current = selectedVoice();
        if (selected != null) selected.setText(current == null ? tr("no_voice_selected") : tr("using") + " " + engineName(current.engine) + " · " + current.name + " · " + current.locale);
        if (status != null) status.setText(tr("voices_imported") + ": " + store.list().size());
    }
    private VoiceEntry selectedVoice() { for (VoiceEntry voice : store.list()) if (voice.id.equals(selectedVoiceId)) return voice; return null; }
    private String engineName(String engine) { return "microsoft".equals(engine) ? "Microsoft" : "google".equals(engine) ? "Google" : engine; }

    private String tr(String key) {
        String lang = prefs == null ? "en" : prefs.getString("ui_language", "en");
        String[][] values = {
                {"welcome_text", "Smart Voice is an Android speech engine for accessible reading. Import compatible offline voice packages and use voices from supported speech engines.", "O Smart Voice é um mecanismo de fala para Android e acessibilidade. Importe pacotes offline compatíveis e use vozes dos mecanismos disponíveis.", "Smart Voice es un motor de voz para Android y accesibilidad. Importa paquetes sin conexión compatibles y usa voces de los motores disponibles."},
                {"welcome_engines", "Microsoft and Google are the first integrated engines. More offline engines may be added over time. To suggest an engine, contact the developer with its technical details and compatible voice packages.", "Microsoft e Google são os primeiros motores integrados. Outros motores offline poderão ser adicionados. Para sugerir um motor, envie ao desenvolvedor os detalhes técnicos e pacotes de voz compatíveis.", "Microsoft y Google son los primeros motores integrados. Se podrán añadir otros motores sin conexión. Para sugerir uno, envía al desarrollador los detalles técnicos y paquetes de voz compatibles."},
                {"version", "Version", "Versão", "Versión"}, {"developer", "Developer", "Desenvolvedor", "Desarrollador"},
                {"done", "Done", "Concluído", "Hecho"}, {"import_package", "Import voice package (.zip)", "Importar pacote de voz (.zip)", "Importar paquete de voz (.zip)"},
                {"select_voice", "Select voice", "Selecionar voz", "Seleccionar voz"}, {"sample_hint", "Text to test the voice", "Texto para testar a voz", "Texto para probar la voz"},
                {"sample_text", "Hello. This is a Smart Voice speech test.", "Olá. Este é um teste de fala do Smart Voice.", "Hola. Esta es una prueba de voz de Smart Voice."},
                {"speak_test", "Play test", "Ouvir teste", "Reproducir prueba"}, {"speech_controls", "Speech controls", "Controles de fala", "Controles de voz"},
                {"volume", "Volume", "Volume", "Volumen"}, {"pitch", "Pitch", "Tom", "Tono"}, {"rate", "Rate", "Velocidade", "Velocidad"},
                {"custom_settings", "Custom settings", "Configurações personalizadas", "Configuración personalizada"},
                {"read_emojis", "Read emojis", "Ler emojis", "Leer emojis"}, {"dictionary", "User dictionary", "Dicionário do usuário", "Diccionario del usuario"},
                {"settings", "Settings", "Configurações", "Configuración"}, {"interface_language", "Interface language", "Idioma da interface", "Idioma de la interfaz"},
                {"system_tts_settings", "Open system speech settings", "Abrir configurações de fala do sistema", "Abrir ajustes de voz del sistema"},
                {"accessibility_settings", "Open accessibility settings", "Abrir configurações de acessibilidade", "Abrir ajustes de accesibilidad"},
                {"dictionary_import", "Import dictionary (TXT/CFG)", "Importar dicionário (TXT/CFG)", "Importar diccionario (TXT/CFG)"},
                {"dictionary_export", "Export dictionary", "Exportar dicionário", "Exportar diccionario"}, {"dictionary_clear", "Delete dictionary", "Apagar dicionário", "Borrar diccionario"},
                {"dictionary_add", "Add word", "Adicionar palavra", "Añadir palabra"}, {"dictionary_empty", "No words in the dictionary yet.", "Ainda não há palavras no dicionário.", "El diccionario aún está vacío."},
                {"edit", "Edit", "Editar", "Editar"}, {"delete", "Delete", "Excluir", "Eliminar"}, {"word", "Word", "Palavra", "Palabra"},
                {"pronunciation", "Pronunciation", "Pronúncia", "Pronunciación"}, {"save", "Save", "Salvar", "Guardar"}, {"cancel", "Cancel", "Cancelar", "Cancelar"},
                {"back", "Back", "Voltar", "Volver"}, {"importing", "Importing voice package", "Importando pacote de voz", "Importando paquete de voz"},
                {"please_wait", "Extracting voice files…", "Extraindo arquivos de voz…", "Extrayendo archivos de voz…"}, {"imported", "Imported voices", "Vozes importadas", "Voces importadas"},
                {"no_voices", "No voices imported. Import a voice package first.", "Nenhuma voz importada. Importe um pacote primeiro.", "No hay voces importadas. Importa un paquete primero."},
                {"no_voice_selected", "No voice selected", "Nenhuma voz selecionada", "Ninguna voz seleccionada"}, {"using", "Using", "Usando", "Usando"},
                {"voices_imported", "Imported voices", "Vozes importadas", "Voces importadas"}, {"tts_starting", "Speech service is starting", "O serviço de fala está iniciando", "El servicio de voz está iniciando"},
                {"ok", "OK", "OK", "OK"}, {"confirm_clear", "Delete every dictionary entry?", "Apagar todas as palavras do dicionário?", "¿Borrar todas las palabras del diccionario?"},
                {"dictionary_imported", "Dictionary imported", "Dicionário importado", "Diccionario importado"}, {"dictionary_exported", "Dictionary exported", "Dicionário exportado", "Diccionario exportado"},
                {"settings_unavailable", "These system settings are unavailable on this device.", "Estas configurações do sistema não estão disponíveis neste aparelho.", "Estos ajustes no están disponibles en este dispositivo."}
        };
        for (String[] row : values) if (row[0].equals(key)) return row["pt".equals(lang) ? 2 : "es".equals(lang) ? 3 : 1];
        return key;
    }

    private TextView heading(LinearLayout root, String text) { TextView v = label(root, text, 28); v.setPadding(0, 0, 0, dp(16)); return v; }
    private TextView label(LinearLayout parent, String text, int size) { TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setPadding(0, dp(8), 0, dp(8)); parent.addView(v, matchWrap()); return v; }
    private void button(LinearLayout parent, String text, Runnable action) { Button b = new Button(this); b.setText(text); b.setOnClickListener(v -> action.run()); parent.addView(b, matchWrap()); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private static String safeMessage(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }

    @Override protected void onDestroy() { if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }
}
