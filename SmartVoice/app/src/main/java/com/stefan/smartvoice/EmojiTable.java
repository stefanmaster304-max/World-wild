package com.stefan.smartvoice;

import android.content.Context;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** Offline, localized emoji speech names from Unicode CLDR annotations. */
public final class EmojiTable {
    private static final Map<String, Node> TABLES = new HashMap<>();
    private static final class Node {
        final Map<Integer, Node> children = new HashMap<>();
        String speech;
    }

    private EmojiTable() { }

    public static String normalize(Context context, String input, String language, boolean speakEmojis) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        String requested = language == null || language.isEmpty() ? "en" : language.replace('_', '-');
        Locale locale = Locale.forLanguageTag(requested);
        String lang = locale.getLanguage().isEmpty() ? "en" : locale.toLanguageTag();
        Node trie = table(context, lang);
        StringBuilder result = new StringBuilder(input.length() + 32);
        int offset = 0;
        while (offset < input.length()) {
            Node node = trie;
            int cursor = offset, matchEnd = -1;
            String matchSpeech = null;
            while (cursor < input.length()) {
                int cp = input.codePointAt(cursor);
                if (cp == 0xFE0F) { cursor += Character.charCount(cp); continue; }
                if (cp == 0xFE0E) break;
                node = node.children.get(cp);
                if (node == null) break;
                cursor += Character.charCount(cp);
                if (node.speech != null && isDisplayedEmoji(input, offset, cursor)) { matchEnd = cursor; matchSpeech = node.speech; }
            }
            if (matchEnd >= 0) {
                if (speakEmojis) {
                    if (result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))) result.append(' ');
                    result.append(matchSpeech).append(' ');
                }
                offset = matchEnd;
                continue;
            }
            int cp = input.codePointAt(offset);
            int next = offset + Character.charCount(cp);
            String flag = flagName(input, offset, lang);
            if (flag != null) {
                if (speakEmojis) {
                    if (result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))) result.append(' ');
                    result.append(flag).append(' ');
                }
                offset = next + Character.charCount(input.codePointAt(next));
            } else if (UCharacter.hasBinaryProperty(cp, UProperty.EMOJI_PRESENTATION) || isSkinTone(cp)) {
                if (speakEmojis) {
                    if (result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))) result.append(' ');
                    result.append("emoji").append(' ');
                }
                offset = skipEmojiContinuation(input, next);
            } else if (cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E) {
                offset = next;
            } else {
                result.appendCodePoint(cp);
                offset = next;
            }
        }
        return result.toString().replaceAll("\\s+([,.;:!?])", "$1").replaceAll("[ \\t]{2,}", " ").trim();
    }

    private static boolean isDisplayedEmoji(String text, int start, int end) {
        boolean explicitEmojiForm = false;
        for (int i = start; i < end;) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if (cp == 0xFE0F || cp == 0x20E3 || cp == 0x200D || isSkinTone(cp) || (cp >= 0x1F1E6 && cp <= 0x1F1FF)) explicitEmojiForm = true;
            if (UCharacter.hasBinaryProperty(cp, UProperty.EMOJI_PRESENTATION)) return true;
        }
        return explicitEmojiForm;
    }

    private static boolean isSkinTone(int cp) { return cp >= 0x1F3FB && cp <= 0x1F3FF; }

    private static String flagName(String text, int offset, String lang) {
        int first = text.codePointAt(offset);
        if (first < 0x1F1E6 || first > 0x1F1FF) return null;
        int next = offset + Character.charCount(first);
        if (next >= text.length()) return null;
        int second = text.codePointAt(next);
        if (second < 0x1F1E6 || second > 0x1F1FF) return null;
        String countryCode = new String(new char[]{(char) ('A' + first - 0x1F1E6), (char) ('A' + second - 0x1F1E6)});
        String countryName = new Locale("", countryCode).getDisplayCountry(Locale.forLanguageTag(lang));
        if (countryName == null || countryName.isEmpty()) countryName = countryCode;
        String language = Locale.forLanguageTag(lang).getLanguage();
        return "pt".equals(language) ? "bandeira de " + countryName : "es".equals(language) ? "bandera de " + countryName : "flag of " + countryName;
    }

    private static int skipEmojiContinuation(String text, int offset) {
        int cursor = offset;
        boolean afterJoiner = false;
        while (cursor < text.length()) {
            int cp = text.codePointAt(cursor);
            if (cp == 0xFE0E || cp == 0xFE0F || cp == 0x20E3 || isSkinTone(cp)) {
                cursor += Character.charCount(cp); continue;
            }
            if (cp == 0x200D) { cursor += Character.charCount(cp); afterJoiner = true; continue; }
            if (afterJoiner && UCharacter.hasBinaryProperty(cp, UProperty.EMOJI)) {
                cursor += Character.charCount(cp); afterJoiner = false; continue;
            }
            break;
        }
        return cursor;
    }

    private static synchronized Node table(Context context, String languageTag) {
        Node cached = TABLES.get(languageTag);
        if (cached != null) return cached;
        Node root = new Node();
        Locale locale = Locale.forLanguageTag(languageTag);
        String language = locale.getLanguage();
        java.util.ArrayList<String> candidates = new java.util.ArrayList<>();
        if (!language.isEmpty()) candidates.add(language);
        if (!locale.getScript().isEmpty()) candidates.add(language + "-" + locale.getScript());
        if (!locale.getCountry().isEmpty()) candidates.add(language + "-" + locale.getCountry());
        String exact = locale.toLanguageTag();
        if (!exact.equals(language)) candidates.add(exact);
        boolean loadedAny = false;
        for (String candidate : candidates) {
            if (candidate.isEmpty() || !hasAsset(context, candidate + ".json")) continue;
            loadedAny |= addAnnotations(context, root, candidate);
        }
        if (!loadedAny && !"en".equals(language)) addAnnotations(context, root, "en");
        TABLES.put(languageTag, root);
        return root;
    }

    private static boolean hasAsset(Context context, String name) {
        try {
            for (String file : context.getAssets().list("emoji")) if (file.equals(name)) return true;
        } catch (Exception ignored) { }
        return false;
    }

    private static boolean addAnnotations(Context context, Node root, String locale) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("emoji/" + locale + ".json"), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[8192]; int read;
            while ((read = reader.read(buffer)) != -1) json.append(buffer, 0, read);
            JSONObject entries = new JSONObject(json.toString()).getJSONObject("annotations").getJSONObject("annotations");
            Iterator<String> keys = entries.keys();
            while (keys.hasNext()) {
                String emoji = keys.next(); JSONObject entry = entries.optJSONObject(emoji);
                if (entry == null || !entry.has("tts") || !containsEmoji(emoji)) continue;
                org.json.JSONArray names = entry.optJSONArray("tts");
                if (names == null || names.length() == 0) continue;
                String name = names.optString(0, "").trim(); if (name.isEmpty()) continue;
                Node node = root;
                for (int i = 0; i < emoji.length();) {
                    int cp = emoji.codePointAt(i); i += Character.charCount(cp);
                    if (cp == 0xFE0F) continue;
                    node = node.children.computeIfAbsent(cp, ignored -> new Node());
                }
                node.speech = name;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean containsEmoji(String text) {
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if (UCharacter.hasBinaryProperty(cp, UProperty.EMOJI)) return true;
        }
        return false;
    }
}
