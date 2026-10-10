package br.com.quipux.cadastropessoas.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.quipux.cadastropessoas.Pessoa;
import br.com.quipux.cadastropessoas.service.NacionalidadeService;
import br.com.quipux.cadastropessoas.service.PessoaService;
import jakarta.validation.Valid;

/**
 * REST endpoints for people. The person is identified by its numeric id.
 * All of them require a logged-in session (see SecurityConfig).
 */
@RestController
public class PessoaController {

    private final PessoaService pessoaService;
    private final NacionalidadeService nacionalidadeService;

    public PessoaController(PessoaService pessoaService, NacionalidadeService nacionalidadeService){
        this.pessoaService = pessoaService;
        this.nacionalidadeService = nacionalidadeService;
    }

    /** Registers a person. 400 if the body is invalid, 409 if the document or e-mail already exists. */
    @PostMapping("/registrarName")
    @ResponseStatus(HttpStatus.CREATED)
    public Pessoa cadastrar(@Valid @RequestBody Pessoa pessoa){
        return pessoaService.cadastrar(pessoa);
    }

    /** Lists every registered person. */
    @GetMapping("/list")
    public List<Pessoa> listarTodas() {
        return pessoaService.listarTodas();
    }

    /** Finds one person by id. 400 if the id is not a number, 404 if it does not exist. */
    @GetMapping("/list/{id}")
    public Pessoa buscarPorId(@PathVariable Long id){
        return pessoaService.buscarPorId(id);
    }

    /** Deletes one person by id. 400 if the id is not a number, 404 if it does not exist. */
    @DeleteMapping("/list/{id}")
    public Map<String, String> excluir(@PathVariable Long id){
        pessoaService.excluir(id);
        return Map.of("mensagem", "Pessoa " + id + " excluída com sucesso");
    }

    /** Predicts (and saves) the nationality of a person using the nationalize.io API. */
    @GetMapping("/findNacionalityByPerson/{id}")
    public Map<String, Object> buscarNacionalidade(@PathVariable Long id){
        return nacionalidadeService.buscarNacionalidade(id);
    }
}
