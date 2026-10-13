package com.vftech.fluxopro.auth;

public record TokenResponse(String accessToken, String tokenType) {

    public static TokenResponse bearer(String token) {
        return new TokenResponse(token, "Bearer");
    }
}