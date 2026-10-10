package br.com.quipux.cadastropessoas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Application entry point: a REST API (and static web UI) to register people. */
@SpringBootApplication
public class CadastroPessoasApplication {

	public static void main(String[] args) {
		SpringApplication.run(CadastroPessoasApplication.class, args);
	}

}
