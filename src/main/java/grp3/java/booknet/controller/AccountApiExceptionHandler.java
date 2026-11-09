package grp3.java.booknet.controller;

import grp3.java.booknet.exception.AccountException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {AccountAuthApiController.class, AccountApiController.class})
public class AccountApiExceptionHandler {
    @ExceptionHandler(AccountException.class)
    public ResponseEntity<?> accountError(AccountException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ.", "errors", errors));
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<?> malformedRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("message", "Dữ liệu tài khoản đã tồn tại hoặc không hợp lệ."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> unauthenticated(AuthenticationException ex) {
        return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập lại."));
    }
}
