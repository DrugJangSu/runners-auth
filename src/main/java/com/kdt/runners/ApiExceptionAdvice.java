package com.kdt.runners;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionAdvice {
    @ExceptionHandler({
            InvalidInputException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> invalidInput(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "invalid input");
    }

    @ExceptionHandler(CannotChangeOwnRoleException.class)
    public ResponseEntity<ApiError> cannotChangeOwnRole(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "CANNOT_CHANGE_OWN_ROLE","cannot change own role");
    }

    @ExceptionHandler(CannotDeleteSelfException.class)
    public ResponseEntity<ApiError> cannotDeleteSelf(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SELF", "cannot delete self");
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiError> loginFailed(Exception ex) {
        return error(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "login failed");
    }

    @ExceptionHandler(TokenFailedException.class)
    public ResponseEntity<ApiError> tokenFailed(Exception ex) {
        return error(HttpStatus.UNAUTHORIZED, "TOKEN_FAILED", "token failed");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(Exception ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "forbidden");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(Exception ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "runner not found");
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<ApiError> duplicateLoginId(Exception ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "duplicate loginId");
    }


    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

}
