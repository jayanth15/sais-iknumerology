package com.namesuggestion;
/** User-supplied Friendly En lookup, distinct from astronomical calculations. */
public final class FriendlyEn {
    private FriendlyEn() {}
    public static String forVithi(int vithiEn) {
        return switch (vithiEn) {
            case 1 -> "4";
            case 2 -> "7";
            case 3 -> "9";
            case 4 -> "1";
            case 5, 6 -> "9";
            case 7 -> "2";
            case 8 -> "5, 6";
            case 9 -> "";
            default -> throw new IllegalArgumentException("Vithi En must be between 1 and 9");
        };
    }
}
