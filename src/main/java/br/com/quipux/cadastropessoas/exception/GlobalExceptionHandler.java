package br.com.quipux.cadastropessoas.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converts exceptions into JSON error responses ({"erro": "..."}) with the right HTTP status.
 * The messages are shown to end users, so they stay in Portuguese.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404: the id does not exist. */
    @ExceptionHandler (PessoaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> tratarPessoaNaoEncontrada(PessoaNaoEncontradaException exception){
        return erro(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** 409: the document or e-mail is already registered. */
    @ExceptionHandler (RegistroDuplicadoException.class)
    public ResponseEntity<Map<String, String>> tratarRegistroDuplicado(RegistroDuplicadoException exception){
        return erro(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** 502: the external nationality API is unavailable. */
    @ExceptionHandler (NacionalidadeIndisponivelException.class)
    public ResponseEntity<Map<String, String>> tratarNacionalidadeIndisponivel(NacionalidadeIndisponivelException exception){
        return erro(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    /** 400: bean validation failed; returns the message of each invalid field. */
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException exception){
        Map<String, String> campos = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
            .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", "Dados inválidos");
        corpo.put("campos", campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    /** 400: a path variable that should be a number (e.g. /list/abc). */
    @ExceptionHandler (MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tratarTipoInvalido(MethodArgumentTypeMismatchException exception){
        return erro(HttpStatus.BAD_REQUEST,
            "O parâmetro '" + exception.getName() + "' deve ser um número inteiro válido");
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem){
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
