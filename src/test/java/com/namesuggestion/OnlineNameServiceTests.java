package com.namesuggestion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.springframework.http.*;
import java.util.*;
class OnlineNameServiceTests {
 final ObjectMapper mapper=new ObjectMapper();
 String response(String name)throws Exception{return mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",mapper.writeValueAsString(Map.of("names",List.of(Map.of("name",name,"meaning","Traditional given name","language","English","gender","Male")))))))));}
 OnlineNameService service(RestClient client,String key,int batches){return new OnlineNameService(mapper,client,key,"sarvam-105b",batches,180);}
 @Test void requestUsesSarvamAuthenticationAndEverySearchIsFresh()throws Exception{
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
  for(int i=0;i<2;i++)server.expect(requestTo(OnlineNameService.ENDPOINT)).andExpect(method(HttpMethod.POST)).andExpect(header("api-subscription-key","test-key"))
   .andExpect(jsonPath("$.model").value("sarvam-105b")).andExpect(jsonPath("$.response_format.type").value("json_object"))
   .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Language: English. Gender: Male.")))
   .andRespond(withSuccess(response("Peter"),MediaType.APPLICATION_JSON));
  var s=service(b.build(),"test-key",1);assertEquals(1,s.find("English","Male","P",1).names().size());assertEquals(1,s.find("English","Male","P",1).names().size());server.verify();
 }
 @Test void missingKeyMakesNoRequest(){var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();assertTrue(service(b.build(),"",1).find("Tamil","Male","P",100).message().contains("SARVAM_API_KEY"));server.verify();}
 @Test void completedBatchesSurviveRateLimitWithoutRetry()throws Exception{
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();server.expect(requestTo(OnlineNameService.ENDPOINT)).andRespond(withSuccess(response("Peter"),MediaType.APPLICATION_JSON));server.expect(requestTo(OnlineNameService.ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("private error detail"));
  var result=service(b.build(),"test-key",10).find("English","Male","P",200);assertEquals(1,result.names().size());assertTrue(result.message().contains("quota"));assertFalse(result.message().contains("private error detail"));server.verify();
 }
 @Test void rejectsWrongLanguageGenderPrefixUnsafeAndDuplicateNames()throws Exception{
  var rows=new ArrayList<Map<String,String>>();for(String name:List.of("Peter","peter","<script>","Adam"))rows.add(Map.of("name",name,"language","English","gender","Male","meaning","Meaning"));
  rows.add(Map.of("name","Priya","language","Tamil","gender","Male"));rows.add(Map.of("name","Paula","language","English","gender","Female"));
  String json=mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",mapper.writeValueAsString(Map.of("names",rows)))))));
  assertEquals(List.of("Peter"),service(RestClient.create(),"test",1).parse(json,"English","Male","P",100).stream().map(OnlineNameService.OnlineName::name).toList());
 }
 @Test void truncationAndMalformedJsonAreRejected()throws Exception{
  var s=service(RestClient.create(),"test",1);assertThrows(java.io.IOException.class,()->s.parse(response("Peter").replace("\"stop\"","\"length\""),"English","Male","P",100));assertThrows(java.io.IOException.class,()->s.parse("{}","English","Male","P",100));assertThrows(java.io.IOException.class,()->s.parse("null","English","Male","P",100));
 }
 @Test void truncatedBatchKeepsCompleteNamesAndReportsPartialResults()throws Exception{
  var complete=mapper.writeValueAsString(Map.of("names",List.of(
   Map.of("name","Paul","meaning","Small","language","English","gender","Male"),
   Map.of("name","Peter","meaning","Rock","language","English","gender","Male"))));
  String partial=complete.substring(0,complete.length()-2)+",{\"name\":\"Pat";
  String payload=mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","length","message",Map.of("content",partial)))));
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
  server.expect(requestTo(OnlineNameService.ENDPOINT)).andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Return 4 distinct suitable names"))).andRespond(withSuccess(payload,MediaType.APPLICATION_JSON));
  String continuation=mapper.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",mapper.writeValueAsString(Map.of("names",List.of(Map.of("name","Philip","meaning","Lover of horses"),Map.of("name","Patrick","meaning","Noble")))))))));
  server.expect(requestTo(OnlineNameService.ENDPOINT)).andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("Return 2 distinct suitable names"),org.hamcrest.Matchers.containsString("Paul, Peter")))).andRespond(withSuccess(continuation,MediaType.APPLICATION_JSON));
  var result=service(b.build(),"test-key",10).find("English","Male","P",4);
  assertEquals(List.of("Paul","Peter","Philip","Patrick"),result.names().stream().map(OnlineNameService.OnlineName::name).toList());
  assertTrue(result.message().contains("cut off a batch"));assertTrue(result.message().contains("2 API call(s)"));server.verify();
 }
 @Test void duplicateOnlyBatchesStopAndExcludePreviousNames()throws Exception{
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();
  server.expect(requestTo(OnlineNameService.ENDPOINT)).andRespond(withSuccess(response("Peter"),MediaType.APPLICATION_JSON));
  for(int i=0;i<10;i++)server.expect(requestTo(OnlineNameService.ENDPOINT)).andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Do not repeat these already returned names: Peter"))).andRespond(withSuccess(response("Peter"),MediaType.APPLICATION_JSON));
  var result=service(b.build(),"test",100).find("English","Male","P",10000);assertEquals(1,result.names().size());assertTrue(result.message().contains("11 API call(s)"));server.verify();
 }
 @Test void invalidFiltersDoNotCallProvider(){var s=service(RestClient.create(),"test",1);assertThrows(IllegalArgumentException.class,()->s.find("Unknown","Male","P",10));assertThrows(IllegalArgumentException.class,()->s.find("Tamil","Male","P",10001));}
 @Test void authenticationFailureIsActionableAndDoesNotExposeResponse(){var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();server.expect(requestTo(OnlineNameService.ENDPOINT)).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("test-key"));var r=service(b.build(),"test-key",10).find("Tamil","Male","P",100);assertTrue(r.names().isEmpty());assertTrue(r.message().contains("rejected the key"));assertFalse(r.message().contains("test-key"));server.verify();}
}
