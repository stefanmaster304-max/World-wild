package com.stefan.smartvoice;

import java.util.Locale;

public final class VoiceEntry {
    public String id;
    public String packageId;
    public String engine;
    public String name;
    public String code;
    public String locale;
    public String variant;
    /** Origin is persisted so Vocalizer remains restricted to Access Mind data. */
    public String source;
    /** Runtime model key, e.g. embedded-compact; variant is the user-facing quality. */
    public String operatingPoint;
    public String dataPath;
    public String engineParam;
    /** Voice name expected by the engine SDK; can differ from the package's voice ID. */
    public String engineVoiceName;
    public int gender;

    public VoiceEntry() {}

    public Locale toLocale() {
        Locale parsed = Locale.forLanguageTag(locale == null ? "" : locale.replace('_', '-'));
        return parsed.getLanguage().isEmpty() ? Locale.ROOT : parsed;
    }
}
