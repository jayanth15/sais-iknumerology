package com.namesuggestion;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;

/**
 * Geocentric lunar longitude from Jean Meeus, Astronomical Algorithms, Ch. 47
 * (60 periodic longitude terms). Sidereal longitude uses the mean Lahiri
 * (Chitrapaksha) ayanamsa. This is an analytical calculation, not an LLM answer.
 *
 * Supported dates: 1950-2100. Input clock times are India Standard Time (IST).
 * Calculation uses UTC -> TT with a historical Delta T estimate, and the
 * geocentric Moon (birthplace does not alter a geocentric Moon sign).
 *
 * The analytical model is approximate; label close-to-boundary results as
 * needing verification against a full professional ephemeris and exact time.
 */
@Service
public class RaasiCalculatorService {
    private static final double NAKSHATRA = 360.0 / 27.0;
    private static final double PADA = 360.0 / 108.0;
    private static final ZoneOffset IST = ZoneOffset.ofHoursMinutes(5, 30);

    private static final String[] RASI_EN = {
        "Mesham", "Rishabam", "Mithunam", "Kadagam", "Simmam", "Kanni",
        "Thulam", "Viruchigam", "Dhanusu", "Magaram", "Kumbam", "Meenam"
    };
    private static final String[] RASI_TA = {
        "மேஷம்", "ரிஷபம்", "மிதுனம்", "கடகம்", "சிம்மம்", "கன்னி",
        "துலாம்", "விருச்சிகம்", "தனுசு", "மகரம்", "கும்பம்", "மீனம்"
    };
    private static final String[] STAR_EN = {
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Arudra",
        "Punarpoosam", "Poosam", "Ayilyam", "Magam", "Pooram", "Uthiram",
        "Hastham", "Chithirai", "Swathi", "Visakam", "Anusham", "Kettai",
        "Moolam", "Pooradam", "Uthiradam", "Thiruvonam", "Avittam",
        "Sathayam", "Poorattadhi", "Uthirattadhi", "Revathi"
    };
    private static final String[] STAR_TA = {
        "அஸ்வினி", "பரணி", "கார்த்திகை", "ரோகிணி", "மிருகசீரிடம்", "திருவாதிரை",
        "புனர்பூசம்", "பூசம்", "ஆயில்யம்", "மகம்", "பூரம்", "உத்திரம்",
        "ஹஸ்தம்", "சித்திரை", "சுவாதி", "விசாகம்", "அனுஷம்", "கேட்டை",
        "மூலம்", "பூராடம்", "உத்திராடம்", "திருவோணம்", "அவிட்டம்",
        "சதயம்", "பூரட்டாதி", "உத்திரட்டாதி", "ரேவதி"
    };
    private static final String[] RASI_LORDS = {
        "Mars (செவ்வாய்)", "Venus (சுக்கிரன்)", "Mercury (புதன்)",
        "Moon (சந்திரன்)", "Sun (சூரியன்)", "Mercury (புதன்)",
        "Venus (சுக்கிரன்)", "Mars (செவ்வாய்)", "Jupiter (குரு)",
        "Saturn (சனி)", "Saturn (சனி)", "Jupiter (குரு)"
    };
    private static final String[] STAR_LORDS = {
        "Ketu (கேது)", "Venus (சுக்கிரன்)", "Sun (சூரியன்)",
        "Moon (சந்திரன்)", "Mars (செவ்வாய்)", "Rahu (ராகு)",
        "Jupiter (குரு)", "Saturn (சனி)", "Mercury (புதன்)"
    };
    // Table 47.A, Meeus (D, M, M', F, longitude coefficient in 10^-6 degrees).
    private static final int[][] LUNAR_LONGITUDE = {
        {0,0,1,0,6288774}, {2,0,-1,0,1274027}, {2,0,0,0,658314},
        {0,0,2,0,213618}, {0,1,0,0,-185116}, {0,0,0,2,-114332},
        {2,0,-2,0,58793}, {2,-1,-1,0,57066}, {2,0,1,0,53322},
        {2,-1,0,0,45758}, {0,1,-1,0,-40923}, {1,0,0,0,-34720},
        {0,1,1,0,-30383}, {2,0,0,-2,15327}, {0,0,1,2,-12528},
        {0,0,1,-2,10980}, {4,0,-1,0,10675}, {0,0,3,0,10034},
        {4,0,-2,0,8548}, {2,1,-1,0,-7888}, {2,1,0,0,-6766},
        {1,0,-1,0,-5163}, {1,1,0,0,4987}, {2,-1,1,0,4036},
        {2,0,2,0,3994}, {4,0,0,0,3861}, {2,0,-3,0,3665},
        {0,1,-2,0,-2689}, {2,0,-1,2,-2602}, {2,-1,-2,0,2390},
        {1,0,1,0,-2348}, {2,-2,0,0,2236}, {0,1,2,0,-2120},
        {0,2,0,0,-2069}, {2,-2,-1,0,2048}, {2,0,1,-2,-1773},
        {2,0,0,2,-1595}, {4,-1,-1,0,1215}, {0,0,2,2,-1110},
        {3,0,-1,0,-892}, {2,1,1,0,-810}, {4,-1,-2,0,759},
        {0,2,-1,0,-713}, {2,2,-1,0,-700}, {2,1,-2,0,691},
        {2,-1,0,-2,596}, {4,0,1,0,549}, {0,0,4,0,537},
        {4,-1,0,0,520}, {1,0,-2,0,-487}, {2,1,0,-2,-399},
        {0,0,2,-2,-381}, {1,1,1,0,351}, {3,0,-2,0,-340},
        {4,0,-3,0,330}, {2,-1,2,0,327}, {0,2,1,0,-323},
        {1,1,-1,0,299}, {2,0,3,0,294}, {2,0,-1,-2,0}
    };

    public record BirthResult(LocalDate dateOfBirth, LocalTime timeOfBirth,
                              String placeOfBirth, String timezone,
                              String raasi, String raasiTamil,
                              String natchathiram, String natchathiramTamil,
                              int pada, String namingSyllable,
                              String raasiLord, String nakshatraLord,
                              double moonLongitudeDegrees, double ayanamsaDegrees,
                              boolean nearPadaBoundary, String method, String note,
                              int vithiEn, int mathiEn,
                              String vithiCalculation, String mathiCalculation, String friendlyEn,
                              List<NakshatraPadaLetters.PadaLetter> padaLetters,
                              String padaLetterSourceStatus, boolean padaLettersUnverified) {}

    public BirthResult calculate(LocalDate birthDate, LocalTime birthTime, String placeOfBirth) {
        if (birthDate == null || birthTime == null) throw new IllegalArgumentException("Date and time of birth are required.");
        if (birthDate.getYear() < 1950 || birthDate.getYear() > 2100 || birthDate.isAfter(LocalDate.now(IST)))
            throw new IllegalArgumentException("Enter a birth date between 1950 and today.");
        if (placeOfBirth == null || placeOfBirth.isBlank() || placeOfBirth.strip().length() > 120)
            throw new IllegalArgumentException("Enter a place of birth in India (maximum 120 characters).");
        String place = placeOfBirth.strip();
        if (!place.matches("[\\p{L}\\p{M}0-9 .,'()\\-/]+"))
            throw new IllegalArgumentException("Please enter a valid Indian city / town name.");

        Instant instant = LocalDateTime.of(birthDate, birthTime).toInstant(IST);
        double jdUt = 2440587.5 + instant.getEpochSecond()/86400.0 + instant.getNano()/86400000000000.0;
        double sidereal = moonSidereal(jdUt);
        int raasi = (int)(sidereal / 30.0);
        int nakshatra = (int)(sidereal / NAKSHATRA);
        int pada = (int)((sidereal - nakshatra * NAKSHATRA) / PADA) + 1;
        // Issue a warning for near-boundary measurements: small birth-time changes can flip pada.
        double remaining = sidereal % PADA;
        boolean caution = Math.min(remaining, PADA - remaining) < 0.10;
        String note = caution
                ? "Very close to a pada boundary. Verify the recorded birth time and confirm with a high-precision Lahiri ephemeris."
                : "Calculated geocentrically using the Meeus lunar series and an approximate Lahiri ayanamsa.";
        BirthDateNumerology.Numbers numbers = BirthDateNumerology.calculate(birthDate);
        NakshatraPadaLetters.Mapping letterMap = NakshatraPadaLetters.forStarIndex(nakshatra);
        return new BirthResult(birthDate, birthTime, place, "Asia/Kolkata (IST, UTC+05:30)",
                RASI_EN[raasi], RASI_TA[raasi], STAR_EN[nakshatra], STAR_TA[nakshatra],
                pada, letterMap.forPada(pada).english(), RASI_LORDS[raasi],
                STAR_LORDS[nakshatra % 9], round6(sidereal), round6(lahiriMean(jdUt)),
                caution, "Thirukanitha / geocentric Meeus lunar terms + approximate mean Lahiri ayanamsa", note,
                numbers.vithiEn(), numbers.mathiEn(), numbers.vithiCalculation(), numbers.mathiCalculation(),
                FriendlyEn.forVithi(numbers.vithiEn()), letterMap.letters(),
                letterMap.sourceStatus(), letterMap.isUnverified());
    }

    private static double round6(double d) {return Math.round(d * 1_000_000.0) / 1_000_000.0;}
    private static double sinDeg(double degrees) {return Math.sin(Math.toRadians(degrees));}
    private static double norm(double degrees) {double result = degrees % 360.0; return result < 0.0 ? result + 360.0 : result;}

    static double moonSidereal(double jdUt) {
        double year = 2000.0 + (jdUt - 2451545.0) / 365.2425;
        double jdTt = jdUt + deltaTSeconds(year) / 86400.0;
        double T = (jdTt - 2451545.0) / 36525.0;
        double L = norm(218.3164477 + 481267.88123421*T - 0.0015786*T*T + T*T*T/538841.0 - T*T*T*T/65194000.0);
        double D = norm(297.8501921 + 445267.1114034*T - 0.0018819*T*T + T*T*T/545868.0 - T*T*T*T/113065000.0);
        double M = norm(357.5291092 + 35999.0502909*T - 0.0001536*T*T + T*T*T/24490000.0);
        double Mp = norm(134.9633964 + 477198.8675055*T + 0.0087414*T*T + T*T*T/69699.0 - T*T*T*T/14712000.0);
        double F = norm(93.2720950 + 483202.0175233*T - 0.0036539*T*T - T*T*T/3526000.0 + T*T*T*T/863310000.0);
        double E = 1.0 - 0.002516*T - 0.0000074*T*T;
        double sigma = 0.0;
        for (int[] term : LUNAR_LONGITUDE) {
            double arg = term[0]*D + term[1]*M + term[2]*Mp + term[3]*F;
            double eccentricity = Math.abs(term[1]) == 1 ? E : (Math.abs(term[1]) == 2 ? E*E : 1.0);
            sigma += term[4] * eccentricity * sinDeg(arg);
        }
        sigma += 3958*sinDeg(119.75+131.849*T) + 1962*sinDeg(L-F) + 318*sinDeg(53.09+479264.290*T);
        // Meeus Ch. 47 returns geocentric longitude referred to mean ecliptic/equinox of date.
        return norm(L + sigma / 1_000_000.0 - lahiriMean(jdUt));
    }

    /** Mean Chitrapaksha ayanamsa anchored at J2000, with IAU mean precession rate. */
    static double lahiriMean(double jdUt) {
        double t = (jdUt - 2451545.0) / 36525.0;
        // Lahiri at J2000: approximately 23.8571 degrees, aligned to Swiss sidereal outputs. The small quadratic tracks precession.
        return 23.8571000 + (50.290966 / 3600.0) * (100.0*t) + (0.000222 / 3600.0) * (100.0*t)*(100.0*t);
    }

    /** NASA/Meeus piecewise polynomial estimate for TT - UT in seconds, 1950-2100. */
    static double deltaTSeconds(double y) {
        double t;
        if (y < 1961.0) { t = y-1950.0; return 29.07 + 0.407*t - t*t/233.0 + t*t*t/2547.0; }
        if (y < 1986.0) {t=y-1975.0; return 45.45+1.067*t-t*t/260.0-t*t*t/718.0;}
        if (y < 2005.0) {t=y-2000.0;return 63.86+0.3345*t-0.060374*t*t+0.0017275*t*t*t+0.000651814*Math.pow(t,4)+0.00002373599*Math.pow(t,5);}
        if (y < 2050.0) {t=y-2000.0;return 62.92+0.32217*t+0.005589*t*t;}
        t=(y-1820.0)/100.0; return -20.0+32.0*t*t-0.5628*(2150.0-y);
    }
}
