package com.app.config.jwt;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtAccessPayload {

    private String userEmail;

    @Builder.Default
    private Set<String> roles = Set.of();

    @Builder.Default
    private Set<String> permissions = Set.of();
}
