package com.namesuggestion;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.test.web.servlet.MockMvc;import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.apache.pdfbox.Loader;import org.apache.pdfbox.text.PDFTextStripper;import org.apache.pdfbox.text.TextPosition;import org.apache.poi.xssf.usermodel.XSSFWorkbook;import java.io.*;import java.util.*;
import static org.mockito.Mockito.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"}) @AutoConfigureMockMvc
class ApplicationTests {
 @Autowired PyramidService pyramid;@Autowired CatalogueService catalogue;@Autowired ExportService export;@Autowired MockMvc mvc;@MockitoBean OnlineNameService online;
 SearchForm form(){SearchForm f=new SearchForm();f.setDateOfBirth("1992-07-03");f.setTimeOfBirth("13:49");f.setStartLetter("P");f.setSurname("Saikiran Kamali");return f;}
 List<OnlineNameService.OnlineName> sourceNames(int count){var values=new ArrayList<OnlineNameService.OnlineName>();for(int i=0;i<count;i++){String n="P"+alpha(i);values.add(new OnlineNameService.OnlineName(n,"male given name",""));}return values;}
 String alpha(int value){StringBuilder out=new StringBuilder();do{out.append((char)('A'+value%26));value=value/26-1;}while(value>=0);return out.reverse().toString();}
 @Test void goldenValuesAndWhitespace(){assertEquals(3,pyramid.calculate("AB","",false).finalNumber());assertEquals(3,pyramid.calculate("A B","",false).finalNumber());assertEquals(9,pyramid.calculate("AP","",false).finalNumber());assertEquals(9,pyramid.calculate("F","A",false).finalNumber());assertEquals("A B",pyramid.calculate(" A "," B ",false).combinedName());}
 @Test void surnameSeparatorDoesNotAddAPyramidZero(){
  var givenFirst=pyramid.calculate("AA","A",false);var surnameFirst=pyramid.calculate("AA","A",true);
  assertEquals(List.of(1,1,1),givenFirst.rows().get(0));assertEquals(4,givenFirst.finalNumber());
  assertEquals(List.of(1,1,1),surnameFirst.rows().get(0));assertEquals(4,surnameFirst.finalNumber());
  assertEquals("AA A",givenFirst.combinedName());assertEquals("A AA",surnameFirst.combinedName());
 }
 @Test void originalParityAcrossRandomInputs(){
  String[] groups={"AIJQY","BKR","CGLS","DMT","EHNX","UVW","OZ","FP"};Random random=new Random(42);String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ .-'";
  for(int t=0;t<1000;t++){StringBuilder first=new StringBuilder("A");for(int j=0;j<random.nextInt(30);j++)first.append(alphabet.charAt(random.nextInt(alphabet.length())));String surname="SAIKIRAN KAMALI";
   for(boolean reverse:new boolean[]{false,true}){String input=reverse?surname+" "+first.toString().trim():first.toString().trim()+" "+surname;List<Integer> a=new ArrayList<>();for(int i=0;i<input.length();i++)for(int g=0;g<groups.length;g++)if(groups[g].indexOf(input.charAt(i))>=0){a.add(g+1);break;}
    while(a.size()>1){List<Integer> next=new ArrayList<>();for(int i=0;i<a.size()-1;i++){int sum=a.get(i)+a.get(i+1);next.add(sum>=10?sum/10+sum%10:sum);}a=next;}
    assertEquals(a.get(0),pyramid.calculate(first.toString(),surname,reverse).finalNumber());
   }
  }
 }
 @Test void inputLimits(){assertThrows(IllegalArgumentException.class,()->pyramid.calculate("","",false));assertThrows(IllegalArgumentException.class,()->pyramid.calculate("தமிழ்","",false));assertThrows(IllegalArgumentException.class,()->pyramid.calculate("A".repeat(121),"",false));}
 @Test void filtersAndDedupUseOnlyLiveSource(){SearchForm f=form();var remote=mock(OnlineNameService.class);when(remote.find(anyString(),anyString(),anyString(),anyInt())).thenAnswer(invocation->new OnlineNameService.Result(sourceNames(60),"live"));var all=catalogue.searchWithInternet(f,remote).names();assertEquals(60,all.size());assertEquals(all.size(),all.stream().map(CatalogueService.Suggestion::name).distinct().count());f.setFinalNumber(all.get(0).finalNumber());var filtered=catalogue.searchWithInternet(f,remote).names();assertFalse(filtered.isEmpty());assertTrue(filtered.stream().allMatch(n->n.finalNumber()==f.getFinalNumber()));f.setStartLetter("Z");assertTrue(catalogue.searchWithInternet(f,remote).names().isEmpty());}
 @Test void validationRunsBeforeOnlineLookup(){SearchForm f=form();assertEquals(100,f.getCandidateCount());f.setCandidateCount(100);assertDoesNotThrow(f::validate);f.setCandidateCount(500);assertDoesNotThrow(f::validate);f.setCandidateCount(501);assertThrows(IllegalArgumentException.class,f::validate);f.setCandidateCount(100);f.setFinalNumber(9);assertDoesNotThrow(f::validate);f.setDateOfBirth("2024-02-30");assertThrows(IllegalArgumentException.class,f::validate);}
 @Test @WithMockUser void candidateCountDefaultsToOneHundredAndCannotExceedFiveHundred()throws Exception{
  mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("max=\"500\""))).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"100\"")));
  mvc.perform(post("/suggestions").with(csrf()).param("dateOfBirth","1992-07-03").param("timeOfBirth","13:49").param("startLetter","P").param("surname","Saikiran").param("language","Tamil").param("gender","Male").param("candidateCount","501"))
   .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Candidate count must be from 1 to 500.")));
  verifyNoInteractions(online);
 }
 @Test void fiveHundredCandidateRequestReachesOnlineGeneration(){
  SearchForm f=form(); f.setCandidateCount(500);
  var remote=mock(OnlineNameService.class);
  when(remote.find("Tamil","Male","P",500)).thenReturn(new OnlineNameService.Result(sourceNames(1),"live"));
  assertEquals(1,catalogue.searchWithInternet(f,remote).names().size());
  verify(remote).find("Tamil","Male","P",500);
 }
 @Test void timeOfBirthMustBeProvidedAndValid(){
  SearchForm f=form();f.setTimeOfBirth("");assertThrows(IllegalArgumentException.class,f::validate);
  f.setTimeOfBirth("25:00");assertThrows(IllegalArgumentException.class,f::validate);
  f.setTimeOfBirth("13:49");assertDoesNotThrow(f::validate);
 }
 @Test void customerPdfUsesTwelveHourClockAtMidnightAndNoon()throws Exception{
  var row=new CatalogueService.Suggestion("Pranav","Sacred sound","Male","Test",3,"Pranav Saikiran","");
  SearchForm f=form();f.setTimeOfBirth("00:01");
  try(var pdf=Loader.loadPDF(export.customerPdf(List.of(row),f))){assertTrue(new PDFTextStripper().getText(pdf).contains("Time of Birth: 12:01 AM IST"));}
  f.setTimeOfBirth("12:00");
  try(var pdf=Loader.loadPDF(export.customerPdf(List.of(row),f))){assertTrue(new PDFTextStripper().getText(pdf).contains("Time of Birth: 12:00 PM IST"));}
 }
 @Test void exportsContainRealData()throws Exception{var f=form();var remote=mock(OnlineNameService.class);when(remote.find("Tamil","Male","P",100)).thenReturn(new OnlineNameService.Result(sourceNames(60),"live"));var rows=catalogue.searchWithInternet(f,remote).names();try(var pdf=Loader.loadPDF(export.pdf(rows,f))){String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains(rows.get(0).combinedName()));assertTrue(text.contains("Language:"));assertTrue(text.contains("Starting Pattern:"));assertTrue(text.contains("Surname:"));assertTrue(text.contains("Name Order:"));assertTrue(text.contains("Date of Birth:"));assertTrue(text.contains("Gender:"));assertTrue(text.contains("Final Number:"));assertTrue(text.contains("S.No."));assertTrue(text.contains("1992-07-03"));assertTrue(pdf.getNumberOfPages()>1);var pageTwo=new PDFTextStripper();pageTwo.setStartPage(2);pageTwo.setEndPage(2);String continuation=pageTwo.getText(pdf).stripLeading();assertFalse(continuation.contains("Name Suggestions"));var lastPage=new PDFTextStripper();lastPage.setStartPage(pdf.getNumberOfPages());lastPage.setEndPage(pdf.getNumberOfPages());assertTrue(lastPage.getText(pdf).contains(rows.get(59).combinedName()));}try(var xls=new XSSFWorkbook(new ByteArrayInputStream(export.excel(rows,f)))){assertEquals(1,xls.getSheetAt(0).getRow(10).getCell(0).getNumericCellValue());assertEquals(rows.get(0).combinedName(),xls.getSheetAt(0).getRow(10).getCell(1).getStringCellValue());}}
 @Test void customerPdfPlacesNameTableCloseToItsDetails()throws Exception{
  SearchForm f=form();var remote=mock(OnlineNameService.class);when(remote.find("Tamil","Male","P",100)).thenReturn(new OnlineNameService.Result(sourceNames(1),"live"));
  var rows=catalogue.searchWithInternet(f,remote).names();Map<String,Float> y=new HashMap<>();
  try(var pdf=Loader.loadPDF(export.customerPdf(rows,f))){
   var stripper=new PDFTextStripper(){@Override protected void writeString(String text,List<TextPosition> positions)throws IOException{if(!positions.isEmpty())y.put(text.trim(),positions.get(0).getYDirAdj());super.writeString(text,positions);}};
   stripper.getText(pdf);
  }
  assertTrue(y.containsKey("Date of Birth:"));assertTrue(y.containsKey("Natchathiram:"));assertTrue(y.containsKey("S.No."));
  assertTrue(Math.abs(y.get("S.No.")-y.get("Natchathiram:"))<60f,"Customer details and names should remain visually close");
 }
 @Test void anonymousCannotAccessApp()throws Exception{mvc.perform(get("/")).andExpect(status().is3xxRedirection());mvc.perform(get("/login")).andExpect(status().isOk());mvc.perform(post("/register").param("username","test").param("password","longpassword")).andExpect(status().isForbidden());}
 @Test void registrationAndLogin()throws Exception{mvc.perform(post("/register").with(csrf()).param("username","tester").param("password","testpassword123")).andExpect(redirectedUrl("/login?registered"));mvc.perform(post("/login").with(csrf()).param("username","tester").param("password","testpassword123")).andExpect(redirectedUrl("/"));}
 @Test @WithMockUser void thymeleafFlowsAndExport()throws Exception{
  when(online.find("Tamil","Male","P",100)).thenReturn(new OnlineNameService.Result(sourceNames(1),"live"));
  mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Find a name")));
  var response=mvc.perform(post("/suggestions").with(csrf()).param("dateOfBirth","1992-07-03").param("timeOfBirth","13:49").param("startLetter","P").param("surname","Saikiran").param("language","Tamil").param("gender","Male"))
   .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("matching names")))
   .andExpect(content().string(org.hamcrest.Matchers.containsString("Sarvam AI")))
   .andExpect(content().string(org.hamcrest.Matchers.containsString("Download User PDF")))
   .andExpect(content().string(org.hamcrest.Matchers.containsString("Download Customer PDF")))
   .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Download Excel")))).andReturn();
  var session=(org.springframework.mock.web.MockHttpSession)response.getRequest().getSession();
  mvc.perform(get("/export/pdf").session(session)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
  var userPdf=mvc.perform(get("/export/user-pdf").session(session)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn();
  try(var pdf=Loader.loadPDF(userPdf.getResponse().getContentAsByteArray())){String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("Language:"));assertTrue(text.contains("Starting Pattern:"));assertTrue(text.contains("Name Order:"));assertTrue(text.contains("Final Number:"));}
  var customerPdf=mvc.perform(get("/export/customer-pdf").session(session)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn();
  try(var pdf=Loader.loadPDF(customerPdf.getResponse().getContentAsByteArray())){String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("Surname: Saikiran"));assertTrue(text.contains("Gender: Male"));assertTrue(text.contains("Date of Birth: 03/07/1992"));assertTrue(text.contains("Time of Birth: 01:49 PM IST"));assertTrue(text.contains("Raasi: Kadagam"));assertTrue(text.contains("Natchathiram: Ayilyam"));assertFalse(text.contains("Language:"));assertFalse(text.contains("Starting Pattern:"));assertFalse(text.contains("Name Order:"));assertFalse(text.contains("Final Number:"));assertTrue(text.contains("PA Saikiran"));}
  mvc.perform(post("/calculator").with(csrf()).param("firstName","Pranav").param("surname","Saikiran")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Pranav Saikiran")));
 }
 @Test @WithMockUser void badNumberHandledWithoutServerError()throws Exception{mvc.perform(post("/suggestions").with(csrf()).param("finalNumber","abc")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Final number must")));}
 @Test @WithMockUser void calculatorPdfContainsNameAndEveryPyramidRow()throws Exception{
  var response=mvc.perform(post("/calculator/pdf").with(csrf()).param("firstName","AA").param("surname","A").param("surnameFirst","false"))
   .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn();
  try(var pdf=Loader.loadPDF(response.getResponse().getContentAsByteArray())){
   String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("FINAL NUMBER"));assertTrue(text.contains("Exact input: AA A"));assertTrue(text.contains("4"));
   assertEquals(3,text.split("A=1",-1).length-1);
   assertTrue(text.contains("1 1 1"));assertTrue(text.contains("2 2"));
  }
 }
}
