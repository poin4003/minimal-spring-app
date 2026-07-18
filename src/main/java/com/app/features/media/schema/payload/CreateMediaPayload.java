package com.app.features.media.schema.payload;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMediaPayload {

    @NotNull
    private MultipartFile file;
}
