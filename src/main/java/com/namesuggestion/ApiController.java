package com.namesuggestion;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import java.util.*;
/** Session-authenticated JSON routes. POST requires Spring Security's CSRF token. */
@RestController @RequestMapping("/api")
public class ApiController {
 private final OnlineNameService online;private final CatalogueService catalogue;private final PyramidService pyramid;
 public ApiController(CatalogueService c,PyramidService p,OnlineNameService online){this.online=online;catalogue=c;pyramid=p;}
 public record CalculationRequest(String firstName,String surname,Boolean surnameFirst){}
 @PostMapping("/suggestions")public CatalogueService.SearchResult suggestions(@RequestBody SearchForm f){return catalogue.searchWithInternet(f,online);}
 @PostMapping("/calculate/final-number")public PyramidService.Calculation calculate(@RequestBody CalculationRequest r){return pyramid.calculate(r.firstName(),r.surname(),Boolean.TRUE.equals(r.surnameFirst()));}
 @ExceptionHandler(IllegalArgumentException.class)ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
}
