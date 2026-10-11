package com.namesuggestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Fake transport only. No fabricated names ever enter the production application. */
class SarvamFastGenerationTests {
 private static String letters(int num){
  StringBuilder text=new StringBuilder();
  do{text.append((char)('a'+num%26));num/=26;}while(num>0);
  return text.toString();
 }
 @Test void cold500RunsInParallelAndRepeatUsesOnlySarvamCache()throws Exception{
  var mapper=new ObjectMapper();
  String database="jdbc:h2:mem:parallel-"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
  var jdbc=new JdbcTemplate(new DriverManagerDataSource(database,"sa",""));
  jdbc.execute("CREATE TABLE sarvam_name_cache (language VARCHAR(40), gender VARCHAR(20), prefix VARCHAR(30), name_key VARCHAR(80), given_name VARCHAR(80), meaning VARCHAR(400), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, PRIMARY KEY(language,gender,prefix,name_key))");
  AtomicInteger calls=new AtomicInteger(),active=new AtomicInteger(),peak=new AtomicInteger();
  RestClient client=RestClient.builder().requestInterceptor((request,body,chain)->{
   calls.incrementAndGet();
   int inflight=active.incrementAndGet();peak.accumulateAndGet(inflight,Math::max);
   try{
    JsonNode params=mapper.readTree(body);
    var matcher=Pattern.compile("generation batch (\\d+)").matcher(params.path("messages").path(1).path("content").asText());
    assertTrue(matcher.find());
    int seed=Integer.parseInt(matcher.group(1));
    Thread.sleep(200);
    List<Map<String,String>> names=new ArrayList<>();
    for(int i=0;i<50;i++)names.add(Map.of("name","P"+letters(seed*60+i),"meaning","Test provider meaning"));
    byte[] payload=mapper.writeValueAsBytes(Map.of("choices",List.of(Map.of("finish_reason","stop", "message",Map.of("content",mapper.writeValueAsString(Map.of("names",names)))))));
    return new ClientHttpResponse(){
     public HttpStatusCode getStatusCode(){return HttpStatus.OK;}
     public String getStatusText(){return "OK";}
     public InputStream getBody(){return new ByteArrayInputStream(payload);}
     public HttpHeaders getHeaders(){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);return h;}
     public void close(){}
    };
   }catch(InterruptedException ex){Thread.currentThread().interrupt();throw new java.io.IOException(ex);}
    finally{active.decrementAndGet();}
  }).build();
  var generator=new OnlineNameService(mapper,client,"test-key","sarvam-105b",36,28,14,new SarvamNameCache(jdbc));
  var initial=generator.find("Tamil","Male","P",500);
  assertEquals(500,initial.names().size());
  assertTrue(peak.get()>=5,"Should execute API batches concurrently");
  assertTrue(jdbc.queryForObject("SELECT count(*) FROM sarvam_name_cache",Integer.class)>=500);
  int callsBefore=calls.get();
  var repeated=generator.find("Tamil","Male","P",500);
  assertEquals(500,repeated.names().size());
  assertEquals(callsBefore,calls.get(),"Cache hit must not create paid API requests");
 }
}
