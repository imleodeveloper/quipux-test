package br.com.quipux.cadastropessoas.exception;

/** Thrown when a document or e-mail is already registered. Mapped to HTTP 409. */
public class RegistroDuplicadoException extends RuntimeException {
    public RegistroDuplicadoException(String mensagem){
        super(mensagem);
    }
}
