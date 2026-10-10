package br.com.quipux.cadastropessoas.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.quipux.cadastropessoas.Pessoa;
import br.com.quipux.cadastropessoas.exception.NacionalidadeIndisponivelException;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Predicts a person's nationality from their first name using the public nationalize.io API,
 * and saves the most likely country on the person.
 */
@Service
public class NacionalidadeService {

    /** One candidate country returned by nationalize.io (ISO code + probability). */
    public record Country(@JsonProperty("country_id") String countryId, double probability) {}
    /** Response body of nationalize.io: {"name": "...", "country": [{"country_id": "BR", "probability": 0.6}]}. */
    public record NationalizeResponse(String name, List<Country> country) {}

    private final PessoaService pessoaService;
    private final RestClient restClient;
    // Caches successful answers by first name: the free API allows only ~100 calls per day
    private final Map<String, NationalizeResponse> cache = new ConcurrentHashMap<>();

    public NacionalidadeService(PessoaService pessoaService,
                                @Value("${app.nationalize.url}") String baseUrl) {
        this.pessoaService = pessoaService;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Looks up the nationality of the person, saves it and returns the details.
     *
     * @throws br.com.quipux.cadastropessoas.exception.PessoaNaoEncontradaException if the person does not exist
     * @throws NacionalidadeIndisponivelException if the external API fails, is rate limited or has no answer
     */
    public Map<String, Object> buscarNacionalidade(Long id) {
        Pessoa pessoa = pessoaService.buscarPorId(id);

        String chave = pessoa.getUserName().trim().toLowerCase();
        NationalizeResponse resposta = cache.get(chave);
        if (resposta == null) {
            try {
                resposta = restClient.get()
                    .uri(uri -> uri.queryParam("name", pessoa.getUserName()).build())
                    .retrieve()
                    .body(NationalizeResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                throw new NacionalidadeIndisponivelException(
                    "Limite de consultas da API nationalize.io atingido. Tente novamente mais tarde.");
            } catch (RestClientException e) {
                throw new NacionalidadeIndisponivelException("Falha ao consultar a API nationalize.io: " + e.getMessage());
            }
            if (resposta != null && resposta.country() != null && !resposta.country().isEmpty()) {
                cache.put(chave, resposta);
            }
        }

        if (resposta == null || resposta.country() == null || resposta.country().isEmpty()) {
            throw new NacionalidadeIndisponivelException(
                "Não foi possível prever a nacionalidade para o nome " + pessoa.getUserName());
        }

        // Pick the most probable country and turn its ISO code into a Portuguese country name
        Country melhor = resposta.country().stream()
            .max(Comparator.comparingDouble(Country::probability)).get();
        String nomePais = Locale.of("", melhor.countryId()).getDisplayCountry(Locale.of("pt", "BR"));

        pessoaService.salvarNacionalidade(pessoa, nomePais);

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("pessoa", pessoa.getUserName() + " " + pessoa.getUserSurname());
        corpo.put("codigoPais", melhor.countryId());
        corpo.put("nacionalidade", nomePais);
        corpo.put("probabilidade", melhor.probability());
        return corpo;
    }
}
