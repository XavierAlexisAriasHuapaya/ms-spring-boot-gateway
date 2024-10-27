package dev.arias.huapaya.gateway.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import dev.arias.huapaya.gateway.dto.JwtDto;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class AuthenticationFilter implements GatewayFilter {

    private final WebClient webClient;

    private final String AUTH_VALIDATE_URI = "http://localhost:3030/api/security/validate";
    private static final String ACCESS_TOKEN_HEADER_NAME = "accessToken";

    private AuthenticationFilter() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return this.onError(exchange);
        }
        final var tokenHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
        final var chunks = tokenHeader.split(" ");
        if (chunks.length != 2 || !chunks[0].equals("Bearer")) {
            return this.onError(exchange);
        }
        final var token = chunks[1];

        return this.webClient.post().uri(AUTH_VALIDATE_URI)
                .header(ACCESS_TOKEN_HEADER_NAME, token)
                .retrieve()
                .bodyToMono(JwtDto.class)
                .map(response -> exchange)
                .flatMap(jwtDto -> {
                    if (!this.hasPermissions(exchange.getRequest().getPath().toString(), token)) {
                        return this.onError(exchange, HttpStatus.FORBIDDEN);
                    }
                    return chain.filter(exchange);
                });
    }

    private SecretKey generateKey() {
        String secret_key = "secrey_key_application_authentication_microservices";
        byte[] password = secret_key.getBytes();
        return Keys.hmacShaKeyFor(password);
    }

    public List<String> extractAuthorities(String token) {
        Claims claims = this.extractAllClaims(token);
        Object authoritiesObject = claims.get("authorities");
        if (authoritiesObject instanceof List<?>) {
            List<?> authoritiesList = (List<?>) authoritiesObject;
            List<String> authorities = new ArrayList<>();

            // Iteramos sobre cada objeto en la lista
            for (Object authorityObj : authoritiesList) {
                if (authorityObj instanceof Map<?, ?>) {
                    // Aseguramos que el objeto sea un mapa
                    Map<?, ?> authorityMap = (Map<?, ?>) authorityObj;
                    // Extraemos el valor del campo 'authority'
                    Object authority = authorityMap.get("authority");
                    if (authority instanceof String) {
                        authorities.add((String) authority);
                    }
                }
            }
            return authorities;
        }
        return Collections.emptyList();
    }

    private Claims extractAllClaims(String jwt) {
        return Jwts.parser().verifyWith(this.generateKey()).build().parseSignedClaims(jwt).getPayload();
    }

    private boolean hasPermissions(String path, String jwt) {
        var authorities = this.extractAuthorities(jwt);
        if (path.startsWith("/proxy-maintenance") && authorities.contains("MAINTENANCE_READ_ALL")) {
            return true;
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange) {
        final var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return response.setComplete();
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        final var response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

}
