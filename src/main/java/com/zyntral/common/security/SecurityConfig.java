package com.zyntral.common.security;

import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.web.ApiConstants;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless JWT security. Public endpoints: auth, webhooks, the embeddable support
 * widget, Swagger, and actuator health. Everything else requires a valid access token.
 */
@Configuration
@EnableMethodSecurity                       // enables @PreAuthorize across the app
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_GET = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/actuator/health", "/actuator/health/**", "/actuator/prometheus",
            ApiConstants.API_V1 + "/ai-images/**"   // generated images served by unguessable id
    };
    private static final String[] PUBLIC_ANY = {
            ApiConstants.API_V1 + "/auth/**",
            ApiConstants.API_V1 + "/billing/webhooks/**",
            ApiConstants.API_V1 + "/support/public/**",        // embeddable widget
            ApiConstants.API_V1 + "/social/oauth/callback/**"  // OAuth provider redirect (state-secured)
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final SecurityErrorWriter errorWriter;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          SecurityErrorWriter errorWriter,
                          org.springframework.core.env.Environment env) {
        this.jwtFilter = jwtFilter;
        this.errorWriter = errorWriter;
        this.allowedOrigins = env.getProperty("zyntral.cors.allowed-origins", "http://localhost:3000");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())               // stateless JWT API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                    .requestMatchers(PUBLIC_ANY).permitAll()
                    .requestMatchers(ApiConstants.API_V1 + "/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            errorWriter.write(req, res, ErrorCode.UNAUTHORIZED))
                    .accessDeniedHandler((req, res, e) ->
                            errorWriter.write(req, res, ErrorCode.FORBIDDEN)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
