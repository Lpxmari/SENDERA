package com.sendera.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sendera.dto.ApiErrorDto;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sendera.security.CuentaActivaVerificador;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String ROL_ADMIN = "ROLE_ADMIN";
    public static final String EMISOR_JWT = "sendera";

    /** Rutas de autenticación que no requieren sesión. */
    private static final String[] AUTH_PUBLICAS = {
            "/auth/registro", "/auth/login", "/auth/recuperar-contrasena", "/auth/verificar-codigo",
            "/auth/cambiar-contrasena"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                                   ObjectProvider<CuentaActivaVerificador> verificadorCuenta,
                                                   @Value("${app.cors.allowed-origins}") List<String> origenesPermitidos) throws Exception {
        // Una cabecera Authorization vencida no debe bloquear rutas públicas (login, destinos).
        RequestMatcher publicas = rutasPublicas();
        DefaultBearerTokenResolver resolverBase = new DefaultBearerTokenResolver();
        BearerTokenResolver resolver = request -> publicas.matches(request) ? null : resolverBase.resolve(request);

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(origenesPermitidos)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Chequeo de salud del despliegue: solo health, sin detalles (ver application.yaml)
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(AUTH_PUBLICAS).permitAll()
                        // Consulta de destinos abierta; crear/modificar/eliminar solo administradores
                        .requestMatchers(HttpMethod.GET, "/destinos", "/destinos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/destinos/*/resenas").authenticated()
                        .requestMatchers(HttpMethod.POST, "/destinos", "/destinos/**").hasAuthority(ROL_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/destinos/**").hasAuthority(ROL_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/destinos/**").hasAuthority(ROL_ADMIN)
                        // Gestión administrativa (RF-013): todo /admin/** exige ROLE_ADMIN firmado en el JWT
                        .requestMatchers("/admin/**").hasAuthority(ROL_ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(resolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt(verificadorCuenta)))
                        .authenticationEntryPoint((req, res, ex) -> escribirError(req, res, objectMapper,
                                HttpStatus.UNAUTHORIZED, "Tu sesión no es válida o expiró. Inicia sesión de nuevo."))
                        .accessDeniedHandler((req, res, ex) -> escribirError(req, res, objectMapper,
                                HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.")))
                .build();
    }

    /** Orígenes explícitos tomados de la configuración; nunca comodines. */
    private static CorsConfigurationSource corsConfigurationSource(List<String> origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        // Valida firma, expiración y emisor
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR_JWT));
        return decoder;
    }

    /**
     * Toma los roles del claim "roles" firmado en el JWT; nunca de datos enviados por el cliente.
     * Además rechaza el token si la cuenta fue desactivada por un administrador.
     */
    private static Converter<Jwt, AbstractAuthenticationToken> convertidorJwt(ObjectProvider<CuentaActivaVerificador> verificadorCuenta) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(authorities);
        return jwt -> {
            CuentaActivaVerificador verificador = verificadorCuenta.getIfAvailable();
            if (verificador != null) {
                Number uid = jwt.getClaim("uid");
                if (uid == null || !verificador.estaActiva(uid.longValue())) {
                    throw new DisabledException("La cuenta no está activa.");
                }
            }
            return convertidor.convert(jwt);
        };
    }

    private static RequestMatcher rutasPublicas() {
        List<RequestMatcher> matchers = new java.util.ArrayList<>();
        for (String ruta : AUTH_PUBLICAS) {
            matchers.add(new AntPathRequestMatcher(ruta));
        }
        matchers.add(new AntPathRequestMatcher("/destinos", HttpMethod.GET.name()));
        matchers.add(new AntPathRequestMatcher("/destinos/**", HttpMethod.GET.name()));
        matchers.add(new AntPathRequestMatcher("/actuator/health/**", HttpMethod.GET.name()));
        matchers.add(new AntPathRequestMatcher("/actuator/health", HttpMethod.GET.name()));
        return new OrRequestMatcher(matchers);
    }

    private static void escribirError(HttpServletRequest req, HttpServletResponse res, ObjectMapper mapper,
                                      HttpStatus status, String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), ApiErrorDto.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensaje)
                .path(req.getRequestURI())
                .build());
    }
}
