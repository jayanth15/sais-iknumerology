package com.namesuggestion;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Independent Raasi/Nakshatra/Pada page; no Sarvam LLM calls or quota usage. */
@Controller
public class RaasiController {
    private final RaasiCalculatorService calculator;
    private final RaasiPdfService raasiPdf;

    public RaasiController(RaasiCalculatorService calculator, RaasiPdfService raasiPdf) {
        this.calculator = calculator;
        this.raasiPdf = raasiPdf;
    }

    @GetMapping("/raasi")
    public String screen() {
        return "raasi";
    }

    @PostMapping("/raasi")
    public String calculateOnScreen(@RequestParam String dateOfBirth,
                                    @RequestParam String timeOfBirth,
                                    @RequestParam String placeOfBirth, Model model) {
        model.addAttribute("dateOfBirth", dateOfBirth);
        model.addAttribute("timeOfBirth", timeOfBirth);
        model.addAttribute("placeOfBirth", placeOfBirth);
        try {
            model.addAttribute("birthResult", calculate(dateOfBirth, timeOfBirth, placeOfBirth));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "raasi";
    }

    /** One-click, authenticated PDF download. Recalculates from submitted birth details. */
    @PostMapping(value = "/raasi/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadPdf(@RequestParam String dateOfBirth,
                                              @RequestParam String timeOfBirth,
                                              @RequestParam String placeOfBirth) throws IOException {
        try {
            RaasiCalculatorService.BirthResult result = calculate(dateOfBirth, timeOfBirth, placeOfBirth);
            String fileName = "IKs_Numerology_Raasi_Natchathiram_" + result.dateOfBirth() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(raasiPdf.render(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    public record CalculateRequest(String dateOfBirth, String timeOfBirth, String placeOfBirth) {}

    /** POST JSON, authenticated; same CSRF protection as existing app. */
    @PostMapping("/api/raasi/calculate")
    @ResponseBody
    public ResponseEntity<?> calculateApi(@RequestBody CalculateRequest request) {
        try {
            return ResponseEntity.ok(calculate(request.dateOfBirth(), request.timeOfBirth(), request.placeOfBirth()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private RaasiCalculatorService.BirthResult calculate(String date, String time, String place) {
        try {
            return calculator.calculate(LocalDate.parse(date), LocalTime.parse(time), place);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Enter a valid birth date (YYYY-MM-DD) and time (HH:mm) in IST.");
        }
    }
}
