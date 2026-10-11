package com.namesuggestion;
import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;
import org.springframework.security.provisioning.JdbcUserDetailsManager;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.core.userdetails.User;
import org.springframework.validation.BindingResult;import org.springframework.http.*;import org.springframework.dao.DuplicateKeyException;
import jakarta.servlet.http.HttpSession;import java.util.*;
@Controller
public class WebController {
 private final OnlineNameService online;private final CatalogueService catalogue;private final PyramidService pyramid;private final ExportService exports;private final JdbcUserDetailsManager users;private final PasswordEncoder encoder;
 public WebController(CatalogueService c,PyramidService p,ExportService e,JdbcUserDetailsManager u,PasswordEncoder encoder,OnlineNameService online){this.online=online;catalogue=c;pyramid=p;exports=e;users=u;this.encoder=encoder;}
 @ModelAttribute void shared(Model m){m.addAttribute("languages",CatalogueService.LANGUAGES);m.addAttribute("genders",CatalogueService.GENDERS);}
 @GetMapping("/login")String login(){return "login";}
 @GetMapping("/register")String register(){return "register";}
 @PostMapping("/register")String register(@RequestParam String username,@RequestParam String password,Model m){
  if(!username.matches("[A-Za-z0-9_]{3,50}")||password.length()<10||password.length()>64||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72){m.addAttribute("error","Username: 3–50 letters, digits or underscores. Password: 10–64 characters, at most 72 UTF-8 bytes.");return "register";}
  try{if(users.userExists(username)){m.addAttribute("error","Username is unavailable.");return "register";}users.createUser(User.withUsername(username).password(encoder.encode(password)).roles("USER").build());}
  catch(DuplicateKeyException e){m.addAttribute("error","Username is unavailable.");return "register";}
  return "redirect:/login?registered";
 }
 @GetMapping({"/","/home"})String home(Model m){m.addAttribute("form",new SearchForm());return "index";}
 @PostMapping("/suggestions")String suggestions(@ModelAttribute("form")SearchForm form,BindingResult binding,Model m,HttpSession session){
  session.removeAttribute("search");session.removeAttribute("results");
  if(binding.hasErrors()){m.addAttribute("error","Final number must be blank or an integer from 1 to 9; candidate count must be an integer from 1 to 500.");return "index";}
  try{var search=catalogue.searchWithInternet(form,online);var result=search.names();m.addAttribute("sourceMessage",search.message());m.addAttribute("results",result);session.setAttribute("results",result);session.setAttribute("search",form);}
  catch(IllegalArgumentException e){m.addAttribute("error",e.getMessage());}return "index";
 }
 @GetMapping("/calculator")String calculator(){return "calculator";}
 @PostMapping("/calculator")String calculate(@RequestParam String firstName,@RequestParam(defaultValue="")String surname,@RequestParam(defaultValue="false")boolean surnameFirst,Model m){
  m.addAttribute("firstName",firstName);m.addAttribute("surname",surname);m.addAttribute("surnameFirst",surnameFirst);
  try{m.addAttribute("calculation",pyramid.calculate(firstName,surname,surnameFirst));}catch(IllegalArgumentException e){m.addAttribute("error",e.getMessage());}return "calculator";
 }
 @PostMapping("/calculator/pdf")ResponseEntity<byte[]> calculationPdf(@RequestParam String firstName,@RequestParam(defaultValue="")String surname,@RequestParam(defaultValue="false")boolean surnameFirst)throws java.io.IOException{
  var calculation=pyramid.calculate(firstName,surname,surnameFirst);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=pyramid-calculation.pdf").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.APPLICATION_PDF).body(exports.calculationPdf(calculation));
 }
 @GetMapping("/export/{format}")ResponseEntity<byte[]> export(@PathVariable String format,HttpSession session)throws Exception{
  Object result=session.getAttribute("results");SearchForm f=(SearchForm)session.getAttribute("search");
  if(result==null||f==null)return ResponseEntity.badRequest().build();
  @SuppressWarnings("unchecked")var rows=(List<CatalogueService.Suggestion>)result;
  byte[] bytes;String type,filename;
  if(format.equals("pdf")||format.equals("user-pdf")){bytes=exports.pdf(rows,f);type="application/pdf";filename=format.equals("user-pdf")?"user-names.pdf":"names.pdf";}
  else if(format.equals("customer-pdf")){bytes=exports.customerPdf(rows,f);type="application/pdf";filename="customer-names.pdf";}
  else if(format.equals("excel")){bytes=exports.excel(rows,f);type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";filename="names.xlsx";}
  else return ResponseEntity.notFound().build();
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+filename).header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(type)).body(bytes);
 }
}
