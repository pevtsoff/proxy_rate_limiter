package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.in.web.error.ErrorResponses;
import com.example.ratelimiter.adapter.in.web.security.JsonAccessDeniedHandler;
import com.example.ratelimiter.adapter.in.web.security.JsonAuthenticationEntryPoint;
import com.example.ratelimiter.adapter.out.security.JwtTokenIssuerAdapter;
import com.example.ratelimiter.application.port.out.TokenIssuerPort;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Four filter chains, most specific first:
 * <ol>
 *   <li>{@code /admin/api/**} - HTTP Basic, role ADMIN;</li>
 *   <li>{@code /actuator/**} - health public, the rest ADMIN;</li>
 *   <li>admin UI - form login, role ADMIN;</li>
 *   <li>everything else is proxied traffic - stateless HS256 bearer JWT (CUC1.1).</li>
 * </ol>
 * Explicit Ant matchers are used because the Vaadin servlet makes plain string patterns
 * ambiguous for Spring Security.
 */
@Configuration(proxyBeanMethods = false)
@SuppressWarnings("deprecation")
public class SecurityConfiguration {

    private static final String ADMIN = "ADMIN";

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiChain(HttpSecurity http, ErrorResponses errors)
            throws Exception {
        JsonAuthenticationEntryPoint entryPoint =
                new JsonAuthenticationEntryPoint(errors, "Basic realm=\"admin\"");
        return stateless(http, errors, entryPoint)
                .securityMatcher(ant("/admin/api/**"))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ADMIN))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain actuatorChain(HttpSecurity http, ErrorResponses errors)
            throws Exception {
        JsonAuthenticationEntryPoint entryPoint =
                new JsonAuthenticationEntryPoint(errors, "Basic realm=\"admin\"");
        return stateless(http, errors, entryPoint)
                .securityMatcher(ant("/actuator/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ant("/actuator/health/**"), ant("/actuator/info"))
                        .permitAll()
                        .anyRequest().hasRole(ADMIN))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .build();
    }

    /** Vaadin protects its own UIDL requests against CSRF, so CSRF is only kept for login. */
    @Bean
    @Order(3)
    public SecurityFilterChain adminUiChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new OrRequestMatcher(
                        ant("/ui/**"), ant("/VAADIN/**"), ant("/login"), ant("/logout")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ant("/login"), ant("/logout")).permitAll()
                        .anyRequest().hasRole(ADMIN))
                .formLogin(form -> form.defaultSuccessUrl("/ui/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(ant("/ui/**"), ant("/VAADIN/**")))
                .build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain proxyChain(HttpSecurity http, ErrorResponses errors)
            throws Exception {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(errors,
                "Bearer");
        return stateless(http, errors, entryPoint)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(ProxySecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(ProxySecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    public TokenIssuerPort tokenIssuerPort(JwtEncoder encoder, ProxySecurityProperties properties) {
        return new JwtTokenIssuerAdapter(encoder, properties.jwtTtl(), Clock.systemUTC());
    }

    @Bean
    public UserDetailsService adminUsers(ProxySecurityProperties properties) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername(properties.adminUsername())
                .password(encoder.encode(properties.adminPassword()))
                .roles(ADMIN)
                .build());
    }

    private static HttpSecurity stateless(HttpSecurity http, ErrorResponses errors,
            JsonAuthenticationEntryPoint entryPoint) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new JsonAccessDeniedHandler(errors)));
    }

    private static AntPathRequestMatcher ant(String pattern) {
        return AntPathRequestMatcher.antMatcher(pattern);
    }

    private static SecretKey secretKey(ProxySecurityProperties properties) {
        byte[] secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
