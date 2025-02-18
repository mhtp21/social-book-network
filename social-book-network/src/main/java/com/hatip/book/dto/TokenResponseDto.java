package com.hatip.book.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;
}
