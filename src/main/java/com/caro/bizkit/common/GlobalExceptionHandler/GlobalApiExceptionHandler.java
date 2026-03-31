package com.caro.bizkit.common.GlobalExceptionHandler;



import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.common.exception.KakaoOAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.validation.ConstraintViolationException;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation Error: {} | Status: {}", errorMessage, status);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.failed(status, errorMessage));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        String message;

        if (statusCode.is5xxServerError()) {
            // 5xx: 내부 정보 숨김
            message = "서버 내부 오류가 발생했습니다.";
            log.error("Error: {} | Status: {} | at {}", ex.getMessage(), statusCode, ex.getStackTrace()[0]);
        } else {

            // 4xx: 실제 에러 메시지 노출
            message = ex.getMessage();
            log.warn("Error: {} | Status: {} | at {}", ex.getMessage(), statusCode, ex.getStackTrace()[0]);
        }

        return ResponseEntity
                .status(statusCode)
                .body(ApiResponse.failed(statusCode, message));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<String>> handleCustomException(CustomException ex) {
        HttpStatusCode statusCode = ex.getStatus();

        String message = ex.getMessage();

        if (statusCode.is5xxServerError()) {
            log.error("Custom Error: {} | Status: {} | at {}", message, statusCode, ex.getStackTrace()[0]);
        } else {
            log.warn("Custom Error: {} | Status: {} | at {}", message, statusCode, ex.getStackTrace()[0]);
        }

        return ResponseEntity
                .status(statusCode)
                .body(ApiResponse.failed(statusCode, message));
    }
    @ExceptionHandler(KakaoOAuthException.class)
    public ResponseEntity<ApiResponse<String>> handleKakaoOAuthException(KakaoOAuthException ex) {
        HttpStatusCode statusCode = ex.getStatus();

        String message = ex.getMessage();
        String apiMessage = ex.getApiMessage();

        log.warn("Custom Error: {} | Status: {} | at {}", apiMessage, statusCode, ex.getStackTrace()[0]);


        return ResponseEntity
                .status(statusCode)
                .body(ApiResponse.failed(statusCode, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "필수 값이 누락되었거나 데이터 형식이 올바르지 않습니다.";
        log.warn("Data Integrity Error: {} | Status: {} | at {}", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), ex.getStackTrace()[0]);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<String>> handleDateTimeParse(DateTimeParseException ex) {
        String message = "날짜 형식이 올바르지 않습니다. (예: 2024-01-01)";
        log.warn("DateTime Parse Error: {} | Status: {} | at {}", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), ex.getStackTrace()[0]);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<ApiResponse<String>> handleClassCast(ClassCastException ex) {
        String message = "요청 데이터 타입이 올바르지 않습니다.";
        log.warn("Type Cast Error: {} | Status: {} | at {}", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), ex.getStackTrace()[0]);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint Violation Error: {} | Status: {} | at {}", message, HttpStatus.BAD_REQUEST.value(), ex.getStackTrace()[0]);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();

        String message = (ex.getReason() != null) ? ex.getReason() : "요청 처리 중 오류가 발생했습니다.";

        if (statusCode.is5xxServerError()) {
            log.error("Server Error: {} | Status: {} | at {}", message, statusCode, ex.getStackTrace()[0]);
        } else {
            log.warn("Client Error: {} | Status: {} | at {}", message, statusCode, ex.getStackTrace()[0]);
        }


        return ResponseEntity
                .status(statusCode)
                .body(ApiResponse.failed(statusCode, message));
    }

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<Object> handleBadJson(HttpMessageNotReadableException ex) {
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.failed(HttpStatus.BAD_REQUEST, "json 형식이 맞지 않습니다."));
//    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;


        log.error("[Internal Server Error] Message: {} | Status: {} | at {}", ex.getMessage(), status.value(), ex.getStackTrace()[0]);


        return ResponseEntity
                .status(status)
                .body(ApiResponse.failed(
                        HttpStatusCode.valueOf(status.value()),
                        "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}
