package com.hatip.book.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt,AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        return new JwtAuthenticationToken(
                source,
                Stream.concat(
                        extractRealmRoles(source).stream(),
                        extractResourceRoles(source).stream()
                ).collect(Collectors.toSet()));
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String,Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }
        return ((Collection<String>) realmAccess.get("roles")).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String,Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorityList = new ArrayList<>();

        resourceAccess.forEach((k, v) -> {
            Map<String,Object> clientRoles = (Map<String,Object>) v;
            if (clientRoles.containsKey("roles")) {
                ((Collection<String>) clientRoles.get("roles")).forEach(role ->
                        authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
            }
        });

        return authorityList;
    }
}
