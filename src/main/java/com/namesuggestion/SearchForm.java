package com.namesuggestion;
import java.time.LocalDate;
public class SearchForm {
 private String dateOfBirth="",startLetter="",surname="",language="Tamil",gender="Male";
 private String nameSource="SARVAM";
 public String getNameSource(){return nameSource;}
 public void setNameSource(String source){nameSource=source==null?"":source.trim().toUpperCase(java.util.Locale.ROOT);}
 private int candidateCount=100;
 public int getCandidateCount(){return candidateCount;} public void setCandidateCount(int value){candidateCount=value;}
 private Integer finalNumber;private boolean surnameFirst;
 public void validate(){
  if(!nameSource.equals("SARVAM")&&!nameSource.equals("EXCEL"))throw new IllegalArgumentException("Select Sarvam AI or Upload Excel as the name source.");
  if(candidateCount<1||candidateCount>500)throw new IllegalArgumentException("Candidate count must be from 1 to 500.");
  if(dateOfBirth.isBlank())throw new IllegalArgumentException("Date of birth is required.");
  try{if(LocalDate.parse(dateOfBirth).isAfter(LocalDate.now()))throw new IllegalArgumentException("Date of birth cannot be in the future.");}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Enter a valid date of birth.");}
  if(nameSource.equals("SARVAM")&&!startLetter.matches("[A-Za-z]{1,30}"))throw new IllegalArgumentException("Enter a starting pattern of 1–30 English letters for Sarvam AI.");
  if(surname.isBlank()||surname.length()>80||!surname.matches("[A-Za-z .'-]+"))throw new IllegalArgumentException("Enter a surname of up to 80 English letters, spaces, periods, apostrophes or hyphens.");
  if(language==null||gender==null||!CatalogueService.LANGUAGES.contains(language)||!CatalogueService.GENDERS.contains(gender))throw new IllegalArgumentException("Select a supported language and gender.");
  if(finalNumber!=null&&(finalNumber<1||finalNumber>9))throw new IllegalArgumentException("Final number must be 1–9, or blank for any.");
 }
 public String getDateOfBirth(){return dateOfBirth;}public void setDateOfBirth(String v){dateOfBirth=v==null?"":v.trim();}
 public String getStartLetter(){return startLetter;}public void setStartLetter(String v){startLetter=v==null?"":v.trim();}
 public String getSurname(){return surname;}public void setSurname(String v){surname=v==null?"":v.trim();}
 public String getLanguage(){return language;}public void setLanguage(String v){language=v;}
 public String getGender(){return gender;}public void setGender(String v){gender=v;}
 public Integer getFinalNumber(){return finalNumber;}public void setFinalNumber(Integer v){finalNumber=v;}
 public boolean isSurnameFirst(){return surnameFirst;}public void setSurnameFirst(boolean v){surnameFirst=v;}
}
