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
    public String dataPath;
    public String engineParam;
    public int gender;

    public VoiceEntry() {}

    public Locale toLocale() {
        Locale parsed = Locale.forLanguageTag(locale == null ? "" : locale.replace('_', '-'));
        return parsed.getLanguage().isEmpty() ? Locale.ROOT : parsed;
    }
}
