package com.app.features.samplepartner.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SampleLoginMetadata {
    private String token;
    private UserData user;

    @Data
    public static class UserData {
        @JsonProperty("_id")
        private String id;
        private String name;
        private String email;
        private List<String> role;
    }
}
