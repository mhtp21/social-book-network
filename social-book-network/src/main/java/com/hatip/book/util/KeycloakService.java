package com.hatip.book.util;

import com.hatip.book.dto.LoginRequestDto;
import com.hatip.book.dto.RegistrationRequestDto;
import com.hatip.book.dto.TokenResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate;
    private final TokenCookieUtil tokenCookieUtil;

    private final String keycloakUrl = "http://localhost:9090";
    private final String realm = "social-book-network";
    private final String clientId = "social-book-network-api";

    public String getAdminToken(){
        String url = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "admin");
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        return Objects.requireNonNull(response.getBody()).get("access_token").toString();
    }

    public boolean registerUser(RegistrationRequestDto registrationRequestDto){
        String url = keycloakUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(getAdminToken())){
            headers.set("Authorization", "Bearer " + getAdminToken());
        }else {
            return false;
        }

        Map<String, Object> user = Map.of(
                "username", registrationRequestDto.getEmail(),
                "email", registrationRequestDto.getEmail(),
                "firstName", registrationRequestDto.getFirstname(),
                "lastName", registrationRequestDto.getLastname(),
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type","password",
                        "value",registrationRequestDto.getPassword(),
                        "temporary",false
                ))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.exchange(url,HttpMethod.POST,entity,String.class);

        if (response.getStatusCode().is2xxSuccessful()) { // 200 - 299 arasındaki durum kodları başarılıdır.
            return assignRoleToUser(registrationRequestDto.getEmail(), "USER");
        } else {
            return false;
        }
    }

    private boolean assignRoleToUser(String username, String roleName) {

        String userId = getUserIdByUsername(username);
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAdminToken());

        Map<String, Object> role = Map.of(
                "name", roleName,
                "clientRole", false,
                "containerId", realm
        );

        HttpEntity<Object> request = new HttpEntity<>(List.of(role), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        // 200 - 299 arasındaki durum kodları başarılıdır.
        return response.getStatusCode().is2xxSuccessful();
    }

    private String getUserIdByUsername(String username) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users?username=" + username;

        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(getAdminToken())){
            headers.set("Authorization", "Bearer " + getAdminToken());
        } else {
            return "";
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        if (response.getBody() != null && !response.getBody().isEmpty()) {
            return response.getBody().getFirst().get("id").toString();
        }
        throw new RuntimeException("User not found");
    }

    public TokenResponseDto login(LoginRequestDto loginRequestDto, HttpServletResponse httpServletResponse){
        String url = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("username", loginRequestDto.getEmail());
        body.add("password", loginRequestDto.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        TokenResponseDto tokenResponseDto = new TokenResponseDto(
                Objects.requireNonNull(responseEntity.getBody()).get("access_token").toString(),
                Objects.requireNonNull(responseEntity.getBody()).get("refresh_token").toString()
        );

        tokenCookieUtil.setTokenCookies(httpServletResponse,tokenResponseDto.getAccessToken(),tokenResponseDto.getRefreshToken());

        return tokenResponseDto;
    }

    public void logoutUser(HttpServletResponse response) {
        tokenCookieUtil.clearTokenCookies(response);
    }

    public TokenResponseDto refreshAccessToken(String refreshToken){
        String url = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()){
            return new TokenResponseDto(
                    Objects.requireNonNull(responseEntity.getBody()).get("access_token").toString(),
                    Objects.requireNonNull(responseEntity.getBody()).get("refresh_token").toString()
            );
        }

        throw new RuntimeException("RefreshAccessToken not found");
    }

    public void validateToken(String token) {
        // Keycloak'tan token'ı validate et
        String url = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        if (!Boolean.TRUE.equals(responseEntity.getBody().get("active"))) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Token is invalid or expired");
        }
    }
}