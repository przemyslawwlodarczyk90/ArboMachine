package com.example.arbomachine.domain.projectdocument.assembler;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Kolejnosc "naturalna" po polsku: "Grupa drzew 2" przed "Grupa drzew 10" (liczby porownywane jako liczby). */
final class NaturalTextOrder implements Comparator<String> {

    static final NaturalTextOrder INSTANCE = new NaturalTextOrder();
    private static final Pattern CHUNK = Pattern.compile("\\d+|\\D+");
    private final Collator polishCollator = Collator.getInstance(Locale.of("pl", "PL"));

    private NaturalTextOrder() {
    }

    @Override
    public int compare(String first, String second) {
        Matcher firstChunks = CHUNK.matcher(first);
        Matcher secondChunks = CHUNK.matcher(second);
        while (firstChunks.find() && secondChunks.find()) {
            String a = firstChunks.group();
            String b = secondChunks.group();
            int result = Character.isDigit(a.charAt(0)) && Character.isDigit(b.charAt(0))
                    ? new java.math.BigInteger(a).compareTo(new java.math.BigInteger(b))
                    : polishCollator.compare(a, b);
            if (result != 0) return result;
        }
        return Integer.compare(first.length(), second.length());
    }
}
