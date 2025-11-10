package com.example.websocket_service.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtDecoder {

    private final PublicKey publicKey;
    private final JwtParser jwtParser;

    public JwtDecoder(ResourceLoader resourceLoader) {
        this.publicKey = getPublicKey(resourceLoader);
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build();
    }

    private PublicKey getPublicKey(ResourceLoader resourceLoader) {
        try {
            InputStream inputStream = resourceLoader.getResource("classpath:keys/public_key.pem").getInputStream();
            String keyString = new String(inputStream.readAllBytes());

            keyString = keyString
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error loading Public Key for JWT validation", e);
        }
    }

    public Jws<Claims> parseToken(String token) {
        return jwtParser.parseClaimsJws(token);
    }
}