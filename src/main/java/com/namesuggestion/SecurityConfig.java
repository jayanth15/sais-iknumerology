package com.namesuggestion;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean JdbcUserDetailsManager users(DataSource source){return new JdbcUserDetailsManager(source);}
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
  return http.authorizeHttpRequests(a->a.requestMatchers("/login","/register","/style.css","/error").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",true).permitAll())
   .logout(l->l.logoutSuccessUrl("/login?logout")).build();
 }
}
