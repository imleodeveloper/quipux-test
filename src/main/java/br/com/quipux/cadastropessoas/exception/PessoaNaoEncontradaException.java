package br.com.quipux.cadastropessoas.exception;

/** Thrown when no person exists with the requested id. Mapped to HTTP 404. */
public class PessoaNaoEncontradaException extends RuntimeException {
    public PessoaNaoEncontradaException(String mensagem){
        super(mensagem);
    }
}
