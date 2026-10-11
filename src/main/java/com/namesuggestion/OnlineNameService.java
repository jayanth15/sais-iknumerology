package com.namesuggestion;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
 private final int maxBatches,seconds,parallelism;
 private final SarvamNameCache cache;
 private final Semaphore generation=new Semaphore(1);
 private static final int FAST_BATCH_SIZE=50;
 private static final String[] FOCUSES={
  "regional everyday names", "classical and historical personal names", "modern names with established usage",
  "literature and poetry inspired names", "nature and landscape related established names",
  "traditional devotional names", "short personal names", "longer three or four syllable names",
  "names from different regions where this language is spoken", "traditional family names used as given names",
  "less common but genuinely attested names", "names from historical figures and artists",
  "names with varied second letters", "names with varied vowels and syllable patterns",
  "older established names", "recently popular names that are in real use"
 };

 @Autowired
 public OnlineNameService(ObjectMapper mapper,SarvamNameCache cache,
    @Value("${sarvam.api-key:}") String key,
    @Value("${sarvam.model:sarvam-105b}") String model,
    @Value("${sarvam.max-batches:36}") int maxBatches,
    @Value("${sarvam.max-seconds:28}") int seconds,
    @Value("${sarvam.parallelism:14}") int parallelism){
  this(mapper,httpClient(),key,model,maxBatches,seconds,parallelism,cache);
 }
 // Compatibility constructor for deterministic single-threaded unit tests.
 OnlineNameService(ObjectMapper mapper,RestClient client,String key,String model,int maxBatches,int seconds){
  this(mapper,client,key,model,maxBatches,seconds,1,null);
 }
 OnlineNameService(ObjectMapper mapper,RestClient client,String key,String model,int maxBatches,int seconds,
                  int parallelism,SarvamNameCache cache){
  this.mapper=mapper;this.client=client;this.apiKey=key.trim();this.model=model;
  this.maxBatches=Math.max(1,Math.min(100,maxBatches));this.seconds=Math.max(1,Math.min(30,seconds));
  this.parallelism=Math.max(1,Math.min(16,parallelism));this.cache=cache;
 }
 private static RestClient httpClient(){
  var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(3000);f.setReadTimeout(24000);
  return RestClient.builder().requestFactory(f).build();
 }

 public Result find(String language,String gender,String prefix,int target){
  if(!CatalogueService.LANGUAGES.contains(language)||!CatalogueService.GENDERS.contains(gender)
    ||prefix==null||!prefix.matches("[A-Za-z]{1,30}")||target<1||target>500)
   throw new IllegalArgumentException("Invalid generation filters or candidate count (1–500).");
  if(apiKey.isBlank())return new Result(List.of(),"Set SARVAM_API_KEY on the server and restart the application to generate names.");
  // A cache hit never creates synthetic names; records came from prior Sarvam completions.
  var names=new LinkedHashMap<String,OnlineName>();
  if(cache!=null){
   for(var n:cache.load(language,gender,prefix))names.putIfAbsent(n.name().toLowerCase(Locale.ROOT),n);
  }
  int fromCache=Math.min(target,names.size());
  if(names.size()>=target)
   return finish(names,target,0,fromCache,"Served from persistent Sarvam results cache.");
  if(!generation.tryAcquire())
   return finish(names,target,0,fromCache,"Another generation is running. Returning previously cached Sarvam names; try again shortly.");
  try{
   if(parallelism>1 && target>=100)return findParallel(language,gender,prefix,target,names,fromCache);
   return findSerial(language,gender,prefix,target,names,fromCache);
  }finally{generation.release();}
 }
 private Result finish(LinkedHashMap<String,OnlineName> names,int target,int calls,int cached,String status){
  return new Result(names.values().stream().limit(target).toList(),
   "Sarvam supplied "+Math.min(names.size(),target)+" of "+target+" unique candidates before the number filter ("
    +cached+" reused from saved Sarvam data, "+calls+" API calls started). "+status+
    " Names and meanings are AI suggestions, not independently verified.");
 }

 private Result findSerial(String language,String gender,String prefix,int target,
                          LinkedHashMap<String,OnlineName> names,int fromCache){
  int calls=0,stalled=0,batchSize=25;
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
      .body(request(language,gender,prefix,batch,names.values(),calls))
      .retrieve().body(String.class);
    Batch parsed=parseBatch(json,language,gender,prefix,batch);
    if(cache!=null)cache.save(language,gender,prefix,parsed.names());
    int before=names.size();for(var n:parsed.names())names.putIfAbsent(n.name().toLowerCase(Locale.ROOT),n);
    stalled=names.size()==before?stalled+1:0;
    if(parsed.incomplete()){
     batchSize=Math.max(5,Math.min(batchSize-1,Math.max(5,parsed.names().size()*2)));
     status="Sarvam cut off a batch; complete names were retained and generation continued with a smaller batch.";
    }
    if(stalled>=10){status="Sarvam supplied no new usable unique names in ten consecutive calls; returning available results.";break;}
   }catch(RestClientResponseException e){status=apiError(e.getStatusCode().value());break;}
    catch(IOException e){stalled++;batchSize=Math.max(5,batchSize/2);
     status="Sarvam returned a truncated or invalid batch; retrying with smaller batches.";
     if(stalled>=10)break;
    }catch(RestClientException e){status="Sarvam timed out or could not be reached; returning available names.";break;}
  }
  return finish(names,target,calls,fromCache,status);
 }

 private record CompletedBatch(List<OnlineName> names,boolean truncated,int errorCode,boolean invalid){}
 private CompletedBatch generateOne(String language,String gender,String prefix,int batch,
                                    Collection<OnlineName> excluded,int seed){
  try{
   // Stagger burst starts modestly to reduce provider throttling risk.
   if(parallelism>1)Thread.sleep(Math.min(600,(seed-1)*45L));
   String json=client.post().uri(ENDPOINT).header("api-subscription-key",apiKey)
     .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
     .body(request(language,gender,prefix,batch,excluded,seed)).retrieve().body(String.class);
   Batch result=parseBatch(json,language,gender,prefix,batch);
   // Store immediately so a completed Sarvam response isn't lost if the browser times out.
   if(cache!=null){
    try{cache.save(language,gender,prefix,result.names());}
    catch(org.springframework.dao.DataAccessException ignored){
     // Keep the actual Sarvam results even if the persistent cache is temporarily unavailable.
    }
   }
   return new CompletedBatch(result.names(),result.incomplete(),0,false);
  }catch(RestClientResponseException e){return new CompletedBatch(List.of(),false,e.getStatusCode().value(),false);}
   catch(InterruptedException e){Thread.currentThread().interrupt();return new CompletedBatch(List.of(),false,0,true);}
   catch(IOException | RestClientException e){return new CompletedBatch(List.of(),false,0,true);}
 }

 private Result findParallel(String language,String gender,String prefix,int target,
                             LinkedHashMap<String,OnlineName> names,int fromCache){
  long deadline=System.nanoTime()+seconds*1_000_000_000L;
  int workers=Math.min(parallelism,maxBatches),started=0,inFlight=0,stalled=0;
  boolean throttled=false,truncated=false;
  int completedBatches=0,totalNew=0;
  String status="Requested candidate count reached.";
  AtomicInteger threadNumber=new AtomicInteger();
  ExecutorService pool=Executors.newFixedThreadPool(workers, runnable->{
    Thread t=new Thread(runnable,"sarvam-batch-"+threadNumber.incrementAndGet());t.setDaemon(true);return t;
  });
  CompletionService<CompletedBatch> completions=new ExecutorCompletionService<>(pool);
  try{
   // Over-request modestly for duplicates, but do not waste API calls while earlier batches are pending.
   while(names.size()<target && System.nanoTime()<deadline){
    int yield=completedBatches==0 ? 47 : Math.max(8,Math.min(50,totalNew/completedBatches));
    int desiredJobs=Math.min(workers,Math.max(1,(int)Math.ceil((target-names.size())*1.15/yield)));
    while(!throttled && inFlight<desiredJobs && started<maxBatches && System.nanoTime()<deadline){
     final int seed=++started;
     // A small context: passing 500 names to each request would make Sarvam slower.
     final List<OnlineName> exclude=names.values().stream().limit(80).toList();
     completions.submit(()->generateOne(language,gender,prefix,FAST_BATCH_SIZE,exclude,seed));
     inFlight++;
    }
    if(inFlight==0){if(!throttled)status="Sarvam did not provide enough unique names within the call limit.";break;}
    Future<CompletedBatch> completed;
    try{
     long remaining=deadline-System.nanoTime();if(remaining<=0)break;
     completed=completions.poll(remaining,TimeUnit.NANOSECONDS);
     if(completed==null)break;
     inFlight--;
     CompletedBatch r=completed.get();
     if(r.errorCode()!=0){
      if(r.errorCode()==429 || r.errorCode()==401 || r.errorCode()==403 || r.errorCode()==402){
       throttled=true;status=apiError(r.errorCode());
      }else{status="Sarvam returned an HTTP error; successfully generated names were retained.";}
     }
     if(r.invalid())status="Some Sarvam requests timed out or returned unusable data; completed batches were retained.";
     if(r.truncated()){truncated=true;status="Some batches were cut off; all completed names were kept.";}
     int before=names.size();
     for(var n:r.names())names.putIfAbsent(n.name().toLowerCase(Locale.ROOT),n);
     if(!r.invalid() && r.errorCode()==0){completedBatches++;totalNew+=names.size()-before;}
     stalled=names.size()==before?stalled+1:0;
     if(stalled>=Math.max(10,workers+2)){
      status="Sarvam repeatedly returned duplicates or no usable names; returning genuine available names.";break;
     }
     // Refill capacity with new prompts that exclude the unique names collected so far.
     if(throttled && inFlight==0)break;
    }catch(InterruptedException e){Thread.currentThread().interrupt();status="Generation interrupted; completed results retained.";break;}
     catch(ExecutionException e){status="A Sarvam request failed; completed results retained.";}
   }
   if(names.size()<target && System.nanoTime()>=deadline && !throttled)
    status="28-second API response budget reached; returning genuine available names. Repeat the search to reuse saved Sarvam data.";
   else if(names.size()>=target)
    status=truncated?"Target reached; truncated batches were recovered when possible.":"Requested candidate count reached.";
   return finish(names,target,started,fromCache,status);
  }finally{pool.shutdownNow();}
 }
 private static String apiError(int code){
  return switch(code){
   case 401,403 -> "Sarvam rejected the API key or model access. Check your account permissions.";
   case 402 -> "Sarvam reported a billing/credit issue. Check your account balance.";
   case 429 -> "Sarvam account rate or quota limit reached. Returning completed data; try again after the limit resets.";
   default -> "Sarvam request failed (HTTP "+code+"). Completed names have been retained.";
  };
 }

 Map<String,Object> request(String language,String gender,String prefix,int batch,Collection<OnlineName> excluded,int batchNumber){
  String system="Suggest established personal given names genuinely used in the requested language community and suitable for the requested gender. Do not translate unrelated names or invent names to fill a quota. Use conventional English/ASCII transliterations for numerology. No surnames or initials. If uncertain about meaning, write 'Meaning uncertain'. Return only compact JSON: {\"names\":[{\"name\":\"...\",\"meaning\":\"...\"}]}. Names at most 39 characters; meanings concise English, at most 60 characters. Aim to return the full requested batch of distinct names. Return an empty array only when no suitable names remain.";
  String prompt="Language: "+language+". Gender: "+gender+". English spelling must start with: "+prefix+". Return "+batch+" distinct suitable names if possible. This is generation batch "+batchNumber+"; vary choices and prefer alternatives rather than a short common list. Do not repeat these already returned names: "+String.join(", ",excluded.stream().map(OnlineName::name).toList());
  Map<String,Object> body=new LinkedHashMap<>();body.put("model",model);body.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",prompt)));body.put("temperature",0.65);body.put("max_tokens",3500);body.put("reasoning_effort",null);body.put("stream",false);body.put("n",1);body.put("response_format",Map.of("type","json_object"));return body;
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
