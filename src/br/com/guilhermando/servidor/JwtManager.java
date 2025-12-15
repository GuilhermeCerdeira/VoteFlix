package br.com.guilhermando.servidor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;
import java.util.Map;

public class JwtManager {

    private static final String SECRET_KEY = "sua-chave-secreta-muito-forte-e-longa-aqui";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
    private static final JWTVerifier verifier = JWT.require(algorithm)
                                                    .withIssuer("MeuServidor")
                                                    .build();

    public static String createToken(Map<String, String> userData) {
        long agora = System.currentTimeMillis();
        long umaHoraEmMilissegundos = 3600 * 1000;

        String token = JWT.create()
                .withIssuer("MeuServidor")
                .withIssuedAt(new Date(agora))
                .withExpiresAt(new Date(agora + umaHoraEmMilissegundos))
                .withClaim("id", userData.get("id"))
                .withClaim("usuario", userData.get("usuario"))
                .withClaim("funcao", userData.get("funcao"))
                .sign(algorithm);

        return token;
    }

    public static DecodedJWT validateTokenAndGetClaims(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT;
        } catch (JWTVerificationException exception){
            System.err.println("Falha na verificação do JWT: " + exception.getMessage());
            return null;
        }
    }
}