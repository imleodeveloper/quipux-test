package br.com.quipux.cadastropessoas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.quipux.cadastropessoas.Pessoa;

/** Data access for {@link Pessoa}. Basic CRUD comes from Spring Data JPA. */
public interface PessoaRepository extends JpaRepository<Pessoa, Long>{

    /** True if some person already uses this document. */
    boolean existsByUserDocument(String userDocument);

    /** True if some person already uses this e-mail (case-insensitive). */
    boolean existsByUserEmailIgnoreCase(String userEmail);
}
