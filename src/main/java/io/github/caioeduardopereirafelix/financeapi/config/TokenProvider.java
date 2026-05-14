package io.github.caioeduardopereirafelix.financeapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenProvider {

    @Value("${api.security.token.expiration}")
    private Long expirationTime;
    @Value("${api.security.token.secret}")
    private String key;

    //gera o token
    public String generateToken(Authentication authentication){

        var userLog = (UserDetails) authentication.getPrincipal();
        return buildToken(userLog.getUsername());
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
        return Keys.hmacShaKeyFor(key.getBytes());
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
