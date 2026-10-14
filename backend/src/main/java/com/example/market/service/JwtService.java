package com.example.market.service;

import com.example.market.common.SecurityConfiguration;
import com.example.market.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    public JwtService(JwtEncoder encoder) { this.encoder = encoder; }

    public String issue(User user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfiguration.ISSUER)
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plus(2, ChronoUnit.HOURS))
                .claim("username", user.getUsername()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
