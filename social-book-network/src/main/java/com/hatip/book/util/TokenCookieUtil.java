package com.hatip.book.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class TokenCookieUtil {

    public void setTokenCookies(HttpServletResponse httpServletResponse, String accessToken, String refreshToken){
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token",refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(86400*30)
                .build();

        httpServletResponse.addHeader("Set-Cookie",accessTokenCookie.toString());
        httpServletResponse.addHeader("Set-Cookie",refreshTokenCookie.toString());
    }

    public void clearTokenCookies(HttpServletResponse httpServletResponse){
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token","")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token","")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        httpServletResponse.addHeader("Set-Cookie", accessTokenCookie.toString());
        httpServletResponse.addHeader("Set-Cookie", refreshTokenCookie.toString());
    }
}