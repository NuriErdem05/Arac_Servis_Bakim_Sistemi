package com.nurierdem.vehicle_service.exception;

import com.nurierdem.vehicle_service.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessError(ResponseStatusException exception){
        String message = exception.getReason() != null
                ? exception.getReason()
                : "İşlem gerçekleştirilemedi.";

        ApiErrorResponse response = new ApiErrorResponse(
                exception.getStatusCode().value(),
                message,
                Map.of()
        );

        return ResponseEntity.status(exception.getStatusCode())
                .headers(exception.getHeaders())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception){
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage() != null ? error.getDefaultMessage() : "Geçersiz değer."));

        ApiErrorResponse response = new ApiErrorResponse(400, "Gönderilen bilgileri kontrol edin.", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception){
        String message = "İstek biçimi veya alan değerleri geçersiz.";

        if (exception instanceof MissingServletRequestParameterException missing){
            message = "Zorunlu parametre eksik: " + missing.getParameterName();
        } else if (exception instanceof  MethodArgumentTypeMismatchException mismatch){
            message = "Parametre değeri geçersiz: " + mismatch.getName();
        }

        ApiErrorResponse response = new ApiErrorResponse(400, message ,Map.of());

        return ResponseEntity.badRequest().body(response);
    }
}
