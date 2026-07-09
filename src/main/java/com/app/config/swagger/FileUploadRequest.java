package com.app.config.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadRequest {
    @Schema(description = "Multipart file")
    private MultipartFile file;
    
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}
