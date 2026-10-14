package com.example.market.common;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(basePackages = "com.example.market.controller")
public class GlobalExceptionHandler {
    public record ErrorResponse(int code, String message, Object data) {}

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException exception) {
        int code = exception.getStatusCode().value();
        return ResponseEntity.status(exception.getStatusCode())
            .body(new ErrorResponse(code, exception.getReason() == null ? "请求失败" : exception.getReason(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldError();
        return badRequest(error == null || error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        return badRequest("请求参数或内容格式不正确");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorResponse> handleDatabase(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "数据库操作失败，请稍后重试", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse(413, "单张图片不能超过5MB", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "服务器处理请求失败", null));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, message, null));
    }
}
