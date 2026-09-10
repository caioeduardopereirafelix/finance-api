package io.github.caioeduardopereirafelix.financeapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenProvider {

    @Value("${api.security.token.expiration}")
    private Long expirationTime;
    @Value("${api.security.token.secret:}")
    private String key;


    private static final int MIN_SECRET_BYTES = 32;

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException();
        }

        int bytes = key.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET tem %d bytes, mas o minimo e %d (recomendado: 64). Gere outro com: openssl rand -base64 48"
                            .formatted(bytes, MIN_SECRET_BYTES));
        }
    }

    //gera o token
    public String generateToken(Authentication authentication){

        var userLog = (UserDetails) authentication.getPrincipal();
        return buildToken(userLog.getUsername());
    }

    public String generateToken(UserDetails userDetails){
        return buildToken(userDetails.getUsername());
    }

    private String buildToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigninKey())
                .compact();
    }

    private SecretKey getSigninKey() {
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    //validar o token
    public boolean isTokenValid(String token){
        try {
            getClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private Claims getClaims(String token){

        //validar assinatura do token
        //validar expiracao do token

        return Jwts.parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    //extrair informacoes do token
    public String getUserName(String token){
        return getClaims(token)
                .getSubject();
    }

}
