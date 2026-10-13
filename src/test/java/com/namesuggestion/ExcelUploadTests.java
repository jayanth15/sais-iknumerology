package com.namesuggestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:excel-test;DB_CLOSE_DELAY=-1", "sarvam.api-key="})
@AutoConfigureMockMvc
class ExcelUploadTests {
 @Autowired ExcelNameService uploads;
 @Autowired CatalogueService catalogue;
 @Autowired ExportService exports;
 @Autowired MockMvc mvc;
 @MockitoBean OnlineNameService online;

 private byte[] workbook(boolean legacy, boolean header) throws IOException {
  try(Workbook book=legacy?new HSSFWorkbook():new XSSFWorkbook(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   Sheet sheet=book.createSheet("Names");
   Row titles=sheet.createRow(0);titles.createCell(0).setCellValue(header?"Name":"Given Name");titles.createCell(1).setCellValue("Meaning");
   sheet.createRow(1).createCell(0).setCellValue("Pranav");sheet.getRow(1).createCell(1).setCellValue("Sacred sound Om");
   sheet.createRow(2).createCell(0).setCellValue("Pranav");sheet.getRow(2).createCell(1).setCellValue("Duplicate");
   sheet.createRow(3).createCell(0).setCellValue("Praanav");sheet.getRow(3).createCell(1).setCellValue("Traditional name");
   sheet.createRow(4).createCell(0).setCellValue("Arun");sheet.getRow(4).createCell(1).setCellValue("Sunrise");
   book.write(out);return out.toByteArray();
  }
 }
 private MockMultipartFile upload(boolean legacy,boolean header)throws IOException{
  return new MockMultipartFile("namesFile",legacy?"sample.xls":"sample.xlsx",legacy?"application/vnd.ms-excel":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",workbook(legacy,header));
 }
 private SearchForm form(){SearchForm f=new SearchForm();f.setNameSource("EXCEL");f.setDateOfBirth("1992-07-03");f.setStartLetter("P");f.setSurname("Saikiran");return f;}
 @Test void bothExcelFormatsAndSamePyramidFilters()throws Exception {
  for(boolean legacy:new boolean[]{false,true}){
   var parsed=uploads.read(upload(legacy,true));
   assertEquals(3,parsed.names().size());assertEquals(1,parsed.duplicateRows());
   var rows=catalogue.searchFromExcel(form(),parsed).names();
   assertEquals(3,rows.size());
   assertTrue(rows.stream().anyMatch(r -> r.name().equals("Arun")), "Excel should ignore the former P prefix");assertEquals("Sacred sound Om",rows.stream().filter(r->r.name().equals("Pranav")).findFirst().orElseThrow().meaning());
   assertTrue(rows.stream().allMatch(r->r.combinedName().endsWith(" Saikiran")));
   assertTrue(rows.stream().allMatch(r->r.origin().contains("Uploaded Excel")));
   var n=rows.get(0).finalNumber();SearchForm f=form();f.setFinalNumber(n);
   assertTrue(catalogue.searchFromExcel(f,parsed).names().stream().allMatch(r->r.finalNumber()==n));
  }
 }
 @Test void invalidHeaderRejected()throws Exception {assertThrows(IllegalArgumentException.class,()->uploads.read(upload(false,false)));}
 @Test @WithMockUser void uploadRendersResultsWithoutCallingSarvam()throws Exception{
  mvc.perform(multipart("/suggestions").file(upload(false,true)).with(csrf())
   .param("nameSource","EXCEL").param("candidateCount","100")
   .param("dateOfBirth","1992-07-03").param("surname","Saikiran")
   .param("language","Tamil").param("gender","Male"))
    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sacred sound Om")))
    .andExpect(content().string(org.hamcrest.Matchers.containsString("Arun Saikiran")))
    .andExpect(content().string(org.hamcrest.Matchers.containsString("No Sarvam API calls")))
    .andExpect(content().string(org.hamcrest.Matchers.containsString("Download Customer PDF")));
  verifyNoInteractions(online);
 }
 @Test @WithMockUser void excelModeRequiresFile()throws Exception{
  mvc.perform(post("/suggestions").with(csrf()).param("nameSource","EXCEL").param("dateOfBirth","1992-07-03")
   .param("surname","Saikiran")).andExpect(status().isOk())
   .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose an Excel file")));
  verifyNoInteractions(online);
 }
 @Test void startingPatternOptionalOnlyForExcel(){
  SearchForm excel=form();excel.setStartLetter("");assertDoesNotThrow(excel::validate);
  SearchForm sarvam=form();sarvam.setNameSource("SARVAM");sarvam.setStartLetter("");
  assertThrows(IllegalArgumentException.class,sarvam::validate);
 }
 @Test void customerPdfUsesNewEmail()throws Exception {
  var rows=catalogue.searchFromExcel(form(),uploads.read(upload(false,true))).names();
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(exports.customerPdf(rows,form()))){
   String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
   assertTrue(text.contains("iksnumerology@gmail.com"));
   assertFalse(text.contains("iksaikiran.kiran@gmail.com"));
  }
 }
}
