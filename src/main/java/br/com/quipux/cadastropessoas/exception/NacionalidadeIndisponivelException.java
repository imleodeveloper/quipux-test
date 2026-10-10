package br.com.quipux.cadastropessoas.exception;

/** Thrown when the external nationality API fails, is rate limited or has no answer. Mapped to HTTP 502. */
public class NacionalidadeIndisponivelException extends RuntimeException {
    public NacionalidadeIndisponivelException(String mensagem){
        super(mensagem);
    }
}
