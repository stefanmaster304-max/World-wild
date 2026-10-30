package es.codefactory.vocalizertts.voices;


/* JADX INFO: loaded from: classes2.dex */
public class VocalizerVoice {
    private String mVoiceName = "";
    private String mLanguage = "";
    private String mVoiceModel = "";
    private int mSampleRate = 0;
    private String mLanguageVersion = "";
    private String mLanguageTLW = "";
    private int mLangId = 0;
    private String mVoiceAge = "";
    private String mVoiceType = "";
    private String mVoiceVersion = "";
    private String mInstallationPath = "";

    public boolean equals(VocalizerVoice vocalizerVoice) {
        return vocalizerVoice.getVoiceName().compareToIgnoreCase(this.mVoiceName) == 0 && vocalizerVoice.getLanguage().compareToIgnoreCase(this.mLanguage) == 0 && vocalizerVoice.getVoiceModel().compareToIgnoreCase(this.mVoiceModel) == 0 && vocalizerVoice.getSampleRate() == this.mSampleRate && vocalizerVoice.getInstallationPath() == this.mInstallationPath;
    }

    public String getInstallationPath() {
        return this.mInstallationPath;
    }

    public int getLangId() {
        return this.mLangId;
    }

    public String getLanguage() {
        return this.mLanguage;
    }

    public String getLanguageTLW() {
        return this.mLanguageTLW;
    }

    public String getLanguageVersion() {
        return this.mLanguageVersion;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public String getVoiceAge() {
        return this.mVoiceAge;
    }

    public String getVoiceModel() {
        return this.mVoiceModel;
    }

    public String getVoiceName() {
        return this.mVoiceName;
    }

    public String getVoiceType() {
        return this.mVoiceType;
    }

    public String getVoiceVersion() {
        return this.mVoiceVersion;
    }

    public void setInstallationPath(String str) {
        this.mInstallationPath = str;
    }
}
