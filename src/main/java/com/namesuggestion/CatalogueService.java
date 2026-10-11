package com.namesuggestion;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Applies the application's existing filters and pyramid calculation to names
 * fetched live from the configured public source. This service deliberately has
 * no bundled or user-supplied name catalogue.
 */
@Service
public class CatalogueService {
 public static final List<String> LANGUAGES=List.of("English","Hindi","Tamil","Sanskrit","Bengali","Malayalam","Telugu","Spanish","German","Kannada");
 public static final List<String> GENDERS=List.of("Male","Female","Neutral");
 public record Suggestion(String name,String meaning,String gender,String origin,int finalNumber,String combinedName,String sourceUrl){}
 public record SearchResult(List<Suggestion> names,String message){}
 private final PyramidService pyramid;

 public CatalogueService(PyramidService pyramid){this.pyramid=pyramid;}

 public SearchResult searchWithInternet(SearchForm f,OnlineNameService online){
  f.validate();
  var remote=online.find(f.getLanguage(),f.getGender(),f.getStartLetter(),f.getCandidateCount());
  Map<String,Suggestion> results=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  for(var n:remote.names()){
   if(!n.name().toLowerCase(Locale.ROOT).startsWith(f.getStartLetter().toLowerCase(Locale.ROOT)))continue;
   var c=pyramid.calculate(n.name(),f.getSurname(),f.isSurnameFirst());
   if(f.getFinalNumber()==null||f.getFinalNumber()==c.finalNumber())
    results.putIfAbsent(n.name(),new Suggestion(n.name(),n.description(),f.getGender(),f.getLanguage()+" name (Sarvam AI)",c.finalNumber(),c.combinedName(),n.sourceUrl()));
  }
  return new SearchResult(results.values().stream().limit(10000).toList(),remote.message());
 }
}
