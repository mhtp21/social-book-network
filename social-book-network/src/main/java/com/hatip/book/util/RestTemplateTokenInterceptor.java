package com.hatip.book.util;

import com.hatip.book.dto.TokenResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestTemplateTokenInterceptor {

    private final KeycloakService keycloakService;
    private final TokenCookieUtil tokenCookieUtil;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final RestTemplate restTemplate;

    public ResponseEntity<String> secureApiCall(String url, HttpMethod method, Object body) {

        String accessToken = getTokenFromCookie("access_token");

        if (accessToken == null || isTokenExpired(accessToken)) {
            String refreshToken = getTokenFromCookie("refresh_token");
            if (refreshToken != null) {
                TokenResponseDto newTokenDto = keycloakService.refreshAccessToken(refreshToken);
                accessToken = newTokenDto.getAccessToken();
                tokenCookieUtil.setTokenCookies(response, newTokenDto.getAccessToken(), newTokenDto.getRefreshToken());
            } else {
                throw new RuntimeException("Refresh Token is missing or expired");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, method, entity, String.class);
    }

    private boolean isTokenExpired(String token) {
        try {
            keycloakService.validateToken(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private String getTokenFromCookie(String cookieName) {
        if (request.getCookies() != null){
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
