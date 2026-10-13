package com.namesuggestion;

import org.springframework.stereotype.Service;
import java.util.*;

/** Applies the same pyramid and final-number filters to both sources; a starting prefix is required only for Sarvam. */
@Service
public class CatalogueService {
 public static final List<String> LANGUAGES=List.of("English","Hindi","Tamil","Sanskrit","Bengali","Malayalam","Telugu","Spanish","German","Kannada");
 public static final List<String> GENDERS=List.of("Male","Female","Neutral");
 public record Suggestion(String name,String meaning,String gender,String origin,int finalNumber,String combinedName,String sourceUrl){}
 public record SearchResult(List<Suggestion> names,String message){}
 private record Candidate(String name,String meaning,String sourceUrl){}
 private final PyramidService pyramid;
 public CatalogueService(PyramidService pyramid){this.pyramid=pyramid;}

 public SearchResult searchWithInternet(SearchForm f, OnlineNameService online){
  f.validate();
  if (!f.getNameSource().equals("SARVAM"))
   throw new IllegalArgumentException("Excel mode requires an uploaded workbook. Use the Suggestions page.");
  var remote=online.find(f.getLanguage(),f.getGender(),f.getStartLetter(),f.getCandidateCount());
  List<Candidate> candidates=remote.names().stream().map(n->new Candidate(n.name(),n.description(),n.sourceUrl())).toList();
  return filterCandidates(f,candidates,"Sarvam AI",remote.message(),f.getCandidateCount());
 }

 public SearchResult searchFromExcel(SearchForm f, ExcelNameService.ReadResult file){
  f.validate();
  if (!f.getNameSource().equals("EXCEL"))
   throw new IllegalArgumentException("Select Upload Excel before submitting a workbook.");
  List<Candidate> candidates=file.names().stream().map(n->new Candidate(n.name(),n.meaning(),"")).toList();
  String message="Excel upload: "+file.names().size()+" unique valid names read ("+file.duplicateRows()+" duplicates and "
   +file.skippedRows()+" invalid rows skipped). ";
  return filterCandidates(f,candidates,"Uploaded Excel",message,f.getCandidateCount());
 }

 private SearchResult filterCandidates(SearchForm f,List<Candidate> candidates,String source,String message,int limit){
  Map<String,Suggestion> results=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  Set<String> seen=new HashSet<>();
  int selected=0;
  for(var n:candidates){
   if(n.name()==null || (f.getNameSource().equals("SARVAM") && !n.name().toLowerCase(Locale.ROOT).startsWith(f.getStartLetter().toLowerCase(Locale.ROOT))))continue;
   if(!seen.add(n.name().toLowerCase(Locale.ROOT)))continue;
   if(selected>=limit)break;
   selected++;
   var c=pyramid.calculate(n.name(),f.getSurname(),f.isSurnameFirst());
   if(f.getFinalNumber()==null||f.getFinalNumber()==c.finalNumber())
    results.putIfAbsent(n.name(),new Suggestion(n.name(),n.meaning(),f.getGender(),
      f.getLanguage()+" name ("+source+")",c.finalNumber(),c.combinedName(),n.sourceUrl()));
  }
  if(source.equals("Uploaded Excel"))message += selected+" unique workbook names were used without starting-letter filtering (limit "
    +limit+") before numerology filtering. No Sarvam API calls were made.";
  return new SearchResult(results.values().stream().toList(),message);
 }
}
