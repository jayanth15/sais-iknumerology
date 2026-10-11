package com.namesuggestion;
import java.util.*;
import org.springframework.stereotype.Service;
/** Pyramid calculation over letters only; spaces and punctuation are separators, not values. */
@Service
public class PyramidService {
 private static final int[] VALUES={1,2,3,4,5,8,3,5,1,1,2,3,4,5,7,8,1,2,3,4,6,6,6,5,1,7};
 public record Calculation(String combinedName,int finalNumber,List<List<Integer>> rows){}
 public Calculation calculate(String first,String surname,boolean surnameFirst){
  first=Objects.requireNonNullElse(first,"").trim();surname=Objects.requireNonNullElse(surname,"").trim();
  String joined;
  if(first.isBlank())joined=surname;
  else if(surname.isBlank())joined=first;
  else joined=surnameFirst?surname+" "+first:first+" "+surname;
  if(first.isBlank()||joined.length()>120||!joined.matches(".*[A-Za-z].*"))throw new IllegalArgumentException("Enter a name in English letters; combined name must be at most 120 characters.");
  List<Integer> row=new ArrayList<>();
  for(char c:joined.toUpperCase(Locale.ROOT).toCharArray())if(c>='A'&&c<='Z')row.add(VALUES[c-'A']);
  List<List<Integer>> rows=new ArrayList<>();rows.add(List.copyOf(row));
  while(row.size()>1){List<Integer> next=new ArrayList<>();for(int i=0;i<row.size()-1;i++){int sum=row.get(i)+row.get(i+1);next.add(sum<10?sum:sum/10+sum%10);}row=next;rows.add(List.copyOf(row));}
  return new Calculation(joined,row.get(0),List.copyOf(rows));
 }
}
