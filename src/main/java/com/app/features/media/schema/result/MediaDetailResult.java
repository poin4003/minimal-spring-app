package com.app.features.media.schema.result;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaDetailResult extends MediaResult {

    private List<MediaVariantResult> variants;
}
