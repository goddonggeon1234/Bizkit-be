package com.caro.bizkit.common.S3.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record PresignedUploadRequest(

        @NotNull(message = "업로드 카테고리는 필수입니다.")
        UploadCategory category,

        @NotBlank(message = "원본 파일명은 필수입니다.")
        String originName
) {
}
