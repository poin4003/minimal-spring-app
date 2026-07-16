package com.app.config.jwt;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtHeader {

    @JsonProperty("kid")
    private UUID keyStoreId;
}
