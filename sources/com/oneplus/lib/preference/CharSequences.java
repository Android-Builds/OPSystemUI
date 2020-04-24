package com.oneplus.lib.preference;

public class CharSequences {
    public static int compareToIgnoreCase(CharSequence charSequence, CharSequence charSequence2) {
        int length = charSequence.length();
        int length2 = charSequence2.length();
        int i = length < length2 ? length : length2;
        int i2 = 0;
        int i3 = 0;
        while (i2 < i) {
            int i4 = i2 + 1;
            int i5 = i3 + 1;
            int lowerCase = Character.toLowerCase(charSequence.charAt(i2)) - Character.toLowerCase(charSequence2.charAt(i3));
            if (lowerCase != 0) {
                return lowerCase;
            }
            i2 = i4;
            i3 = i5;
        }
        return length - length2;
    }
}
