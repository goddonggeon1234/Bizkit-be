package com.caro.bizkit.common.ApiResponse;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponse<T> {
    @Schema(hidden = true)
    private HttpStatusCode code;
    private String message;
    private T data;
    @Schema(hidden = true)
    private Object pagination;


    @Builder
    public ApiResponse(HttpStatusCode code, String message, T data, Object pagination){
        this.code = code;
        this.message = message;
        this.data = data;
        this.pagination = pagination;
    }



    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> successWithPagination(String message, T data, Object pagination) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(data)
                .pagination(pagination)
                .build();
    }



    public static <T> ApiResponse<T> failed(HttpStatusCode code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

}
