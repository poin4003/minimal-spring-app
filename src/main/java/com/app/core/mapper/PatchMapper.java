package com.app.core.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PatchMapper extends ModelMapper {
    
    public PatchMapper() {
        super();

        this.getConfiguration().setSkipNullEnabled(true);
    }
}
