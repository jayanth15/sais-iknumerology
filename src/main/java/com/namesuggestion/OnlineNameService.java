package com.namesuggestion;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Sarvam is the only name provider. Credentials never enter a prompt or response. */
@Service
public class OnlineNameService {
 static final String ENDPOINT="https://api.sarvam.ai/v1/chat/completions";
 public record OnlineName(String name,String description,String sourceUrl){}
 public record Result(List<OnlineName> names,String message){}
 record Batch(List<OnlineName> names,boolean incomplete){}
 private final RestClient client;
 private final ObjectMapper mapper;
 private final String apiKey,model;
 private final int maxBatches,seconds;
 private final Semaphore generation=new Semaphore(1);

 @Autowired
 public OnlineNameService(ObjectMapper mapper,@Value("${sarvam.api-key:}") String key,
   @Value("${sarvam.model:sarvam-105b}") String model,
   @Value("${sarvam.max-batches:250}") int maxBatches,
   @Value("${sarvam.max-seconds:900}") int seconds){
  this(mapper,httpClient(),key,model,maxBatches,seconds);
 }
 OnlineNameService(ObjectMapper mapper,RestClient client,String key,String model,int maxBatches,int seconds){
  this.mapper=mapper;this.client=client;this.apiKey=key.trim();this.model=model;
  this.maxBatches=Math.max(1,Math.min(500,maxBatches));this.seconds=Math.max(1,Math.min(1800,seconds));
 }
 private static RestClient httpClient(){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(10000);f.setReadTimeout(60000);return RestClient.builder().requestFactory(f).build();}

 public Result find(String language,String gender,String prefix,int target){
  if(!CatalogueService.LANGUAGES.contains(language)||!CatalogueService.GENDERS.contains(gender)||prefix==null||!prefix.matches("[A-Za-z]{1,30}")||target<1||target>10000)throw new IllegalArgumentException("Invalid generation filters or candidate count (1–10,000).");
  if(apiKey.isBlank())return new Result(List.of(),"Set SARVAM_API_KEY on the server and restart the application to generate names.");
  if(!generation.tryAcquire())return new Result(List.of(),"Another generation is running. Please try again after it finishes.");
  try {
   var names=new LinkedHashMap<String,OnlineName>();int calls=0,stalled=0,batchSize=25;
   long deadline=System.nanoTime()+seconds*1_000_000_000L;
   String status="Requested candidate count reached.";
   while(names.size()<target){
    if(calls>=maxBatches){status="API call limit reached; returning partial results.";break;}
    if(System.nanoTime()>=deadline){status="Time budget reached; returning partial results.";break;}
    int batch=Math.min(batchSize,target-names.size());
    try {
     calls++;
     String json=client.post().uri(ENDPOINT).header("api-subscription-key",apiKey)
       .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
       .body(request(language,gender,prefix,batch,names.values(),calls)).retrieve().body(String.class);
     Batch parsed=parseBatch(json,language,gender,prefix,batch);List<OnlineName> received=parsed.names();
     int before=names.size();for(var name:received){names.putIfAbsent(name.name().toLowerCase(Locale.ROOT),name);if(names.size()==target)break;}
     stalled=names.size()==before?stalled+1:0;
     if(parsed.incomplete()){
      batchSize=Math.max(5,Math.min(batchSize-1,Math.max(5,received.size()*2)));
      status="Sarvam cut off a batch; complete names were kept and generation continued with a smaller batch.";
     }
     if(stalled>=10){status="Sarvam supplied no new usable unique names in ten consecutive calls; returning available results.";break;}
    }catch(RestClientResponseException e){
     int code=e.getStatusCode().value();
     status=switch(code){case 401,403 -> "Sarvam rejected the key or model access. Check SARVAM_API_KEY and your account permissions.";case 402 -> "Sarvam reported a billing/credit issue. Check your account balance.";case 429 -> "Sarvam rate or quota limit reached. Try later or check your account quota.";default -> "Sarvam request failed (HTTP "+code+"). Check model availability and account settings.";};break;
    }catch(IOException e){
     stalled++;batchSize=Math.max(5,batchSize/2);
     status="Sarvam returned a cut-off or invalid batch; retrying with a smaller batch. Completed batches are retained.";
     if(stalled>=10){status="Sarvam returned ten consecutive cut-off or invalid batches; returning available results.";break;}
    }catch(RestClientException e){status="Sarvam could not be reached or timed out. Any completed batches are retained.";break;}
   }
   return new Result(List.copyOf(names.values()),"Sarvam supplied "+names.size()+" unique candidates before the number filter, from "+calls+" API call(s). "+status+" Names, meanings and language associations are AI suggestions, not independently verified.");
  }finally{generation.release();}
 }

 Map<String,Object> request(String language,String gender,String prefix,int batch,Collection<OnlineName> excluded,int batchNumber){
  String system="Suggest established personal given names genuinely used in the requested language community and suitable for the requested gender. Do not translate unrelated names or invent names to fill a quota. Use conventional English/ASCII transliterations for numerology. No surnames or initials. If uncertain about meaning, write 'Meaning uncertain'. Return only compact JSON: {\"names\":[{\"name\":\"...\",\"meaning\":\"...\"}]}. Names at most 39 characters; meanings concise English, at most 60 characters. Aim to return the full requested batch of distinct names. Return an empty array only when no suitable names remain.";
  String prompt="Language: "+language+". Gender: "+gender+". English spelling must start with: "+prefix+". Return "+batch+" distinct suitable names if possible. This is generation batch "+batchNumber+"; vary choices and prefer alternatives rather than a short common list. Do not repeat these already returned names: "+String.join(", ",excluded.stream().map(OnlineName::name).toList());
  Map<String,Object> body=new LinkedHashMap<>();body.put("model",model);body.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",prompt)));body.put("temperature",0.65);body.put("max_tokens",8192);body.put("reasoning_effort",null);body.put("stream",false);body.put("n",1);body.put("response_format",Map.of("type","json_object"));return body;
 }
List<OnlineName> parse(String json,String language,String gender,String prefix,int limit)throws IOException{
 Batch result=parseBatch(json,language,gender,prefix,limit);if(result.incomplete())throw new IOException("Completion was cut off; recovered entries are in the partial batch.");return result.names();
}
 Batch parseBatch(String json,String language,String gender,String prefix,int limit)throws IOException{
 if(json==null||json.length()>2_000_000)throw new IOException("Invalid response");
 JsonNode root=mapper.readTree(json);if(root==null)throw new IOException("Empty response");
 JsonNode choice=root.path("choices").path(0);
  String finish=choice.path("finish_reason").asText("");
 String content=choice.path("message").path("content").asText("").trim();
 if(content.startsWith("```"))content=content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
  if(!finish.equals("stop")){
   List<OnlineName> recovered=recoverCompleteNames(content,language,gender,prefix,limit);
   return new Batch(recovered,true);
  }
  JsonNode document;
  try{document=mapper.readTree(content);}catch(IOException malformed){List<OnlineName> recovered=recoverCompleteNames(content,language,gender,prefix,limit);if(recovered.isEmpty())throw new IOException("Invalid completion JSON.");return new Batch(recovered,true);}
  if(document==null||!document.path("names").isArray()){List<OnlineName> recovered=recoverCompleteNames(content,language,gender,prefix,limit);return new Batch(recovered,true);}
  return new Batch(validNames(document.path("names"),language,gender,prefix,limit),false);
 }
 private List<OnlineName> recoverCompleteNames(String content,String language,String gender,String prefix,int limit){
  int namesAt=content.indexOf("\"names\"");if(namesAt<0)return List.of();int arrayAt=content.indexOf('[',namesAt);if(arrayAt<0)return List.of();
  var rows=new ArrayList<JsonNode>();boolean inString=false,escaped=false;int depth=0,objectStart=-1;
  for(int i=arrayAt+1;i<content.length();i++){
   char c=content.charAt(i);
   if(inString){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')inString=false;continue;}
   if(c=='\"'){inString=true;continue;}
   if(c=='{'){if(depth++==0)objectStart=i;continue;}
   if(c=='}'&&depth>0&&--depth==0){try{JsonNode row=mapper.readTree(content.substring(objectStart,i+1));if(row!=null)rows.add(row);}catch(IOException ignored){}objectStart=-1;continue;}
   if(c==']'&&depth==0)break;
  }
  return validNames(mapper.valueToTree(rows),language,gender,prefix,limit);
 }
 private List<OnlineName> validNames(JsonNode rows,String language,String gender,String prefix,int limit){
 var names=new LinkedHashMap<String,OnlineName>();
  for(JsonNode row:rows){
  String name=row.path("name").asText("").trim().replaceAll("\\s+"," ");
  String returnedLanguage=row.path("language").asText("").trim(),returnedGender=row.path("gender").asText("").trim();
  if((!returnedLanguage.isBlank()&&!language.equalsIgnoreCase(returnedLanguage))||(!returnedGender.isBlank()&&!gender.equalsIgnoreCase(returnedGender))||!name.matches("[A-Za-z][A-Za-z '-]{0,38}")||!name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))continue;
  String meaning=row.path("meaning").asText("").trim();if(meaning.isBlank())meaning="Meaning uncertain";if(meaning.length()>300)meaning=meaning.substring(0,300);
  names.putIfAbsent(name.toLowerCase(Locale.ROOT),new OnlineName(name,meaning,""));if(names.size()>=limit)break;
 }
 return List.copyOf(names.values());
}
}
