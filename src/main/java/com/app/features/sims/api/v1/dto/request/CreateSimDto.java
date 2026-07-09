package com.app.features.sims.api.v1.dto.request;

import com.app.features.sims.enums.SimStatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSimDto {
    
    @NotBlank(message = "Phone number must not be blank")
    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Phone number must contain only digits")
    private String phoneNumber;

    @NotNull
    private SimStatusEnum status;

    private Integer sellingPrice;
    private Integer dealerPrice;
    private Integer importPrice;
}
