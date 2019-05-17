package com.kubra.prepay.config;

import com.kubra.security.SecurityConstants;
import com.kubra.security.oidc.rs.context.User;
import com.kubra.security.oidc.rs.context.UserContext;
import com.kubra.security.oidc.rs.security.Auth0WebSecurityConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.GenericFilterBean;

/**
 *
 */
@Configuration
@Profile("functional")
public class FunctionalTestingSecurityConfig extends Auth0WebSecurityConfiguration {

  public static final String TEST_USERNAME_AND_PASSWORD = "test";

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .httpBasic()
        .and()
        .csrf().disable()
        .addFilterAfter(new TenantPassThroughFilter(), BasicAuthenticationFilter.class);
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/actuator/**");
  }

  /**
   * This `{noop}` prefix is required due to some password formatting
   * (a.k.a. Spring magic) that happens.
   */
  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
        .withUser(TEST_USERNAME_AND_PASSWORD)
        .password("{noop}" + TEST_USERNAME_AND_PASSWORD)
        .roles("USER", "ADMIN");
  }

  class TenantPassThroughFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
        FilterChain filterChain) throws IOException, ServletException {

      if (servletRequest instanceof HttpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String tenantId = request.getHeader(SecurityConstants.TENANT_HEADER);
        String principal = request.getHeader(SecurityConstants.AUTH_HEADER_ID);
        List<String> groups = new ArrayList<>();

        UUID tenantUUID = null;
        try {
          tenantUUID = UUID.fromString(tenantId);
        } catch (Exception e) {
          filterChain.doFilter(servletRequest, servletResponse);
          return;
        }

        User user = new User(tenantUUID, principal, groups);
        UserContext.setUserUnverifiedTenant(user);
      }
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }
}