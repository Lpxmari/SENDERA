package com.sendera.exception;

import com.sendera.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorDto> handleNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ArchivoImagenInvalidoException.class)
    public ResponseEntity<ApiErrorDto> handleImagenInvalida(ArchivoImagenInvalidoException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorDto> handleImagenDemasiadoGrande(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return construir(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen no puede superar los 5 MB.", request);
    }

    // ---- Autenticación y validación (más específicos que RuntimeException, por eso tienen prioridad) ----

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ApiErrorDto> handleCorreoYaRegistrado(CorreoYaRegistradoException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiErrorDto> handleCredencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TokenRecuperacionInvalidoException.class)
    public ResponseEntity<ApiErrorDto> handleTokenInvalido(TokenRecuperacionInvalidoException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(FiltroInvalidoException.class)
    public ResponseEntity<ApiErrorDto> handleFiltroInvalido(FiltroInvalidoException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(DestinoConItinerariosException.class)
    public ResponseEntity<ApiErrorDto> handleDestinoConItinerarios(DestinoConItinerariosException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(OperacionAdminNoPermitidaException.class)
    public ResponseEntity<ApiErrorDto> handleOperacionAdmin(OperacionAdminNoPermitidaException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ReservaNoPermitidaException.class)
    public ResponseEntity<ApiErrorDto> handleReservaNoPermitida(ReservaNoPermitidaException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDto> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Datos de la petición inválidos.");
        return construir(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDto> handleCuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handleAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDto> handleNoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida o expiró. Inicia sesión de nuevo.", request);
    }

    /** Último recurso: errores de Spring MVC conservan su código; el resto es un 500 sin detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleInesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            if (status.is4xxClientError()) {
                return construir(status, mensajeCliente(status), request);
            }
        }
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo más tarde.", request);
    }

    private String mensajeCliente(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para esta ruta.";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no es compatible.";
            default -> "La solicitud no es válida.";
        };
    }

    private ResponseEntity<ApiErrorDto> construir(HttpStatus status, String mensaje, HttpServletRequest request) {
        ApiErrorDto error = ApiErrorDto.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensaje)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}