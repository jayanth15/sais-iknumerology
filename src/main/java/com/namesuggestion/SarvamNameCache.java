package com.namesuggestion;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Locale;

/** Persistent H2 cache containing ONLY names and meanings returned by Sarvam. */
@Repository
public class SarvamNameCache {
 private final JdbcTemplate jdbc;
 public SarvamNameCache(JdbcTemplate jdbc){this.jdbc=jdbc;}

 public List<OnlineNameService.OnlineName> load(String language,String gender,String prefix){
  return jdbc.query("SELECT given_name, meaning FROM sarvam_name_cache " +
    "WHERE language=? AND gender=? AND prefix=? ORDER BY created_at, name_key LIMIT 2000",
    (rs,row)->new OnlineNameService.OnlineName(rs.getString(1),rs.getString(2),""),
    language.toLowerCase(Locale.ROOT),gender.toLowerCase(Locale.ROOT),prefix.toLowerCase(Locale.ROOT));
 }

 public void save(String language,String gender,String prefix,List<OnlineNameService.OnlineName> names){
  if(names.isEmpty())return;
  String lang=language.toLowerCase(Locale.ROOT),gen=gender.toLowerCase(Locale.ROOT),pre=prefix.toLowerCase(Locale.ROOT);
  // Insert only: preserve original Sarvam meanings if a different response repeats a name.
  // A duplicate's constraint violation must not discard the rest of this batch.
  for(var n:names){
   try{
    jdbc.update("INSERT INTO sarvam_name_cache (language,gender,prefix,name_key,given_name,meaning) VALUES (?,?,?,?,?,?)",
      lang,gen,pre,n.name().toLowerCase(Locale.ROOT),n.name(),n.description());
   }catch(org.springframework.dao.DuplicateKeyException alreadyCached){/* original preserved */}
  }
 }
}
