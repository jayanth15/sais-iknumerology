package com.namesuggestion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * User-requested date-of-birth numerology. These values are derived exclusively
 * from the birth date, independently of the astronomical birth-star calculation.
 *
 * Vithi En (விதி எண்): digital root of all eight DDMMYYYY digits.
 * Mathi En (மதி எண்): digital root of the two DD digits.
 */
final class BirthDateNumerology {
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd");

    private BirthDateNumerology() {}

    record Numbers(int vithiEn, int mathiEn, String vithiCalculation, String mathiCalculation) {}
    private record ReducedNumber(int value, String working) {}

    static Numbers calculate(LocalDate dateOfBirth) {
        Objects.requireNonNull(dateOfBirth, "Date of birth is required");
        ReducedNumber vithi = reduceDigits(dateOfBirth.format(DDMMYYYY));
        ReducedNumber mathi = reduceDigits(dateOfBirth.format(DD));
        return new Numbers(vithi.value(), mathi.value(), vithi.working(), mathi.working());
    }

    private static ReducedNumber reduceDigits(String digits) {
        int sum = 0;
        StringBuilder working = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0) working.append(" + ");
            int digit = digits.charAt(i) - '0';
            sum += digit;
            working.append(digit);
        }
        working.append(" = ").append(sum);
        while (sum > 9) {
            String previous = Integer.toString(sum);
            int next = 0;
            working.append(" → ");
            for (int i = 0; i < previous.length(); i++) {
                if (i > 0) working.append(" + ");
                int digit = previous.charAt(i) - '0';
                next += digit;
                working.append(digit);
            }
            sum = next;
            working.append(" = ").append(sum);
        }
        return new ReducedNumber(sum, working.toString());
    }
}
