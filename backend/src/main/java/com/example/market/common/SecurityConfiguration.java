package com.example.market.common;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.market.service.AuthService;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/** 使用Spring Security内置BearerTokenAuthenticationFilter验证JWT签名、有效期和签发者。 */
@Configuration
public class SecurityConfiguration {
    public static final String ISSUER = "secondhand-market";

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecretKey jwtSigningKey(@Value("${app.jwt.secret:}") String configuredSecret) {
        byte[] bytes;
        if (configuredSecret.isBlank()) {
            // 本地开发无需在代码中存放固定密钥；重启后已有Token失效。
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            try { bytes = Base64.getDecoder().decode(configuredSecret); }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("JWT_SECRET必须为Base64编码的密钥");
            }
            if (bytes.length < 32) { throw new IllegalArgumentException("JWT_SECRET解码后至少32字节"); }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请登录，或重新登录以获取有效Token\",\"data\":null}");
        };
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/me", "/api/products/my", "/api/orders", "/api/orders/**").authenticated()
                        .requestMatchers("/api/favorites", "/api/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/uploads/images").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/health/database", "/api/categories", "/api/products", "/api/products/{id}", "/api/uploads/images/{name}").permitAll()
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request,response,exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"无权访问此接口\",\"data\":null}");
                        }))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            try {
                                // 每个JWT请求读取数据库，禁用、恢复或角色变更立即生效。
                                var user=authService.currentUser(token.getSubject());
                                return new JwtAuthenticationToken(token,List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
                            } catch(ResponseStatusException exception) {
                                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),"账号不可用");
                            }
                        }))
                        .authenticationEntryPoint(unauthorized))
                .build();
    }
}
