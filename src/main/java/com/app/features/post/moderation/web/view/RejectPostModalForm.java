package com.app.features.post.moderation.web.view;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectPostModalForm {

    @NotBlank(message = "{validation.post.rejectReason.required}")
    @Size(max = 1_000, message = "{validation.post.rejectReason.tooLong}")
    @UiField(
            label = "post.moderation.reject.reason",
            placeholder = "post.moderation.reject.reasonPlaceholder",
            helpText = "post.moderation.reject.reasonHint",
            type = UiInputType.TEXTAREA,
            rows = 5,
            required = true)
    private String reason;
}
