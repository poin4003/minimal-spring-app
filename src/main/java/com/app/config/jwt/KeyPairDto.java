package com.app.config.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeyPairDto {

    private String privateKey;

    private String publicKey; 
}
