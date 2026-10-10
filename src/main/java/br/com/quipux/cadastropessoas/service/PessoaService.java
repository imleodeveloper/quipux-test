package br.com.quipux.cadastropessoas.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.quipux.cadastropessoas.Pessoa;
import br.com.quipux.cadastropessoas.exception.PessoaNaoEncontradaException;
import br.com.quipux.cadastropessoas.exception.RegistroDuplicadoException;
import br.com.quipux.cadastropessoas.repository.PessoaRepository;

/** Business rules for registering, finding and deleting people. */
@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;

    public PessoaService(PessoaRepository pessoaRepository){
        this.pessoaRepository = pessoaRepository;
    }

    /** Saves a new person; rejects a document or e-mail that is already registered. */
    public Pessoa cadastrar(Pessoa pessoa){
        if (pessoaRepository.existsByUserDocument(pessoa.getUserDocument())) {
            throw new RegistroDuplicadoException("Já existe uma pessoa com o documento " + pessoa.getUserDocument());
        }
        if (pessoaRepository.existsByUserEmailIgnoreCase(pessoa.getUserEmail())) {
            throw new RegistroDuplicadoException("Já existe uma pessoa com o e-mail " + pessoa.getUserEmail());
        }
        return pessoaRepository.save(pessoa);
    }

    public List<Pessoa> listarTodas(){
        return pessoaRepository.findAll();
    }

    /** @throws PessoaNaoEncontradaException if no person has this id */
    public Pessoa buscarPorId(Long id){
        return pessoaRepository.findById(id)
            .orElseThrow(() ->
                new PessoaNaoEncontradaException("Pessoa não encontrada com ID: " + id));
    }

    /** Stores the predicted nationality on the person. */
    public void salvarNacionalidade(Pessoa pessoa, String nacionalidade){
        pessoa.setNacionalidade(nacionalidade);
        pessoaRepository.save(pessoa);
    }

    /** @throws PessoaNaoEncontradaException if no person has this id */
    public void excluir(Long id){
        Pessoa pessoa = buscarPorId(id);
        pessoaRepository.delete(pessoa);
    }

}
