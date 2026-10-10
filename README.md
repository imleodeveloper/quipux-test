# Cadastro de Pessoas

Teste Desenvolvedor Fullstack. API REST em **Java 21 + Spring Boot 4** com interface web em **HTML, CSS e JavaScript puros**.

## O que o sistema faz

- Cadastra, lista, busca por ID e exclui pessoas (documento, nome, sobrenome, e-mail).
- Consulta a **nacionalidade provável** de uma pessoa na API pública [nationalize.io](https://nationalize.io) e **salva** o resultado.
- Exige **login** para usar qualquer API (sessão por cookie `HttpOnly`).
- Importa pessoas por arquivo ou texto colado, em **JSON** ou **TXT**.
- Tem pesquisa, paginação e formatação inteligente nos campos.

## Requisitos

| Item | Versão |
|---|---|
| JDK | 21 ou superior |
| MySQL | 8+ (opcional, existe perfil sem banco externo) |
| Internet | só para a consulta de nacionalidade |

Não precisa instalar Maven nem Node: o projeto traz o Maven Wrapper (`mvnw`) e o front-end não tem build.

## Como rodar

Na pasta do projeto (onde está o `pom.xml`).

### Passo 0 (obrigatório): criar o arquivo `.env`

As senhas não ficam no código. Copie o modelo e escolha a senha do login:

```bash
cp .env.example .env
```

No Windows (CMD): `copy .env.example .env`. Depois abra o `.env` e troque `APP_PASSWORD=change-me` pela senha que quiser. Sem `APP_PASSWORD` a aplicação **não sobe** (de propósito: não existe senha padrão no código).

| Variável | Para que serve | Padrão |
|---|---|---|
| `APP_USER` | usuário do login | `admin` |
| `APP_PASSWORD` | senha do login | sem padrão (obrigatória) |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | endereço do MySQL | `localhost`, `3306`, `cadastro_pessoas` |
| `DB_USER`, `DB_PASSWORD` | usuário e senha do MySQL | `root` e senha vazia |

O `.env` está no `.gitignore` e **não vai para o Git**. O `.env.example` (sem segredos) vai.

### Opção A, sem instalar nada: banco H2 em memória

Os dados somem ao parar o servidor.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

No Windows (CMD ou PowerShell): `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"`

### Opção B, com MySQL (padrão)

1. Deixe o MySQL rodando em `localhost:3306` (Laragon, XAMPP, Docker etc.).
2. Se o seu MySQL tiver outro usuário ou senha, ajuste `DB_USER` e `DB_PASSWORD` no `.env`.
3. Rode:
   ```bash
   ./mvnw spring-boot:run
   ```

O banco `cadastro_pessoas` e a tabela `pessoa` são criados sozinhos na primeira execução (`createDatabaseIfNotExist=true` e `ddl-auto=update`).

### Acessar

Abra **http://localhost:8080/**. Você será levado à tela de login.

Entre com o `APP_USER` e o `APP_PASSWORD` que você definiu no `.env`.

## Testes

```bash
./mvnw test
```

São testes automatizados executados em H2 (não precisam de MySQL, de internet nem do `.env`: usam credenciais próprias de teste). Cobrem:

- Cadastro, listagem, busca e exclusão, com todas as validações e códigos de erro.
- Login, logout, bloqueio sem sessão, CSRF e redirecionamento para o login.
- Consulta de nacionalidade usando uma API falsa local, inclusive a gravação no banco.

## Endpoints

Todos exigem sessão ativa, exceto `/login`, `/login.html` e `/api/me`. Sem login a resposta é **401**.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/login` | Form `username` + `password`. Cria a sessão |
| POST | `/logout` | Encerra a sessão |
| GET | `/api/me` | Estado da sessão (e emite o cookie CSRF) |
| POST | `/registrarName` | Cadastra uma pessoa |
| GET | `/list` | Lista as pessoas |
| GET | `/list/{id}` | Busca uma pessoa pelo ID |
| DELETE | `/list/{id}` | Exclui uma pessoa pelo ID |
| GET | `/findNacionalityByPerson/{id}` | Consulta a nationalize.io pelo primeiro nome, devolve e salva o nome do país |

O parâmetro escolhido para identificar a pessoa é o **ID** numérico.

Exemplo de corpo do `POST /registrarName`:

```json
{
  "userName": "Maria",
  "userSurname": "Silva",
  "userDocument": "12345678901",
  "userEmail": "maria@gmail.com"
}
```

Exemplo de resposta de `GET /findNacionalityByPerson/1`:

```json
{ "pessoa": "Maria Silva", "codigoPais": "BR", "nacionalidade": "Brasil", "probabilidade": 0.61 }
```

### Chamando a API fora da tela (curl)

A autenticação é por cookie e há proteção CSRF, então é preciso guardar os cookies e enviar o token:

```bash
# 1) pega o cookie XSRF-TOKEN
curl -c cj.txt -b cj.txt http://localhost:8080/api/me
# 2) login (troque TOKEN pelo valor do cookie XSRF-TOKEN no cj.txt)
curl -c cj.txt -b cj.txt -X POST -H "X-XSRF-TOKEN: TOKEN" -d "username=SEU_USUARIO&password=SUA_SENHA" http://localhost:8080/login
# 3) após o login o token muda: chame /api/me de novo e use o novo valor
curl -c cj.txt -b cj.txt http://localhost:8080/api/me
# 4) agora as APIs funcionam (GET não precisa do token; POST e DELETE precisam)
curl -b cj.txt http://localhost:8080/list
```

## Validações

| Onde | Regra | Erro |
|---|---|---|
| POST cadastro | nome, sobrenome, documento e e-mail obrigatórios | 400 com os campos |
| POST cadastro | e-mail em formato válido | 400 |
| POST cadastro | documento com 11 dígitos numéricos (CPF) | 400 |
| POST cadastro | documento e e-mail únicos (e-mail sem diferenciar maiúsculas) | 409 |
| GET, DELETE e nacionalidade | `{id}` precisa ser inteiro | 400 |
| GET, DELETE e nacionalidade | pessoa precisa existir | 404 |
| Nacionalidade | API externa falhou ou sem resultado | 502 |

A API gratuita da nationalize.io limita a cerca de 100 consultas por dia por IP. Ao estourar, aparece uma mensagem amigável, e o resultado de cada primeiro nome fica em memória para não repetir a consulta.

## Segurança

- **Sessão por cookie:** o login cria a sessão no servidor e o navegador recebe só o `JSESSIONID`, com `HttpOnly` e `SameSite=Strict`. Nada fica em `localStorage`.
- **CSRF:** como o cookie é enviado automaticamente, o CSRF está ativo. O front lê o cookie `XSRF-TOKEN` e o devolve no header `X-XSRF-TOKEN` em todas as chamadas.
- **Sem segredos no código:** usuário e senha do login e do banco vêm do `.env` (que não vai para o Git). A senha do login é transformada em hash BCrypt na subida. Para um sistema real, o ideal seria uma tabela de usuários.

## Importação (JSON ou TXT)

Botão **Importar** na lista. O modal mostra o exemplo de cada formato e aceita um arquivo `.json`/`.txt` ou o conteúdo colado.

**JSON**: lista de objetos.

```json
[
  { "userName": "Maria", "userSurname": "Silva", "userDocument": "123.456.789-09", "userEmail": "maria@gmail.com" },
  { "userName": "João", "userSurname": "Souza", "userDocument": "98765432100", "userEmail": "joao@outlook.com" }
]
```

**TXT**: uma pessoa por linha, `Nome;Sobrenome;Documento;E-mail`. Linhas com `#` são ignoradas.

```
Maria;Silva;123.456.789-09;maria@gmail.com
João;Souza;98765432100;joao@outlook.com
```

Cada pessoa é cadastrada pela mesma API e passa pelas mesmas validações. O resultado mostra quantas entraram e o motivo de cada erro.

## Front-end

Feito com **HTML5, CSS3 e JavaScript puro (vanilla)**, sem framework (não usa React, Vue nem Angular) e sem etapa de build. Os dois arquivos ficam em `src/main/resources/static/` e o próprio Spring Boot os serve.

- `login.html`: tela de login.
- `index.html`: sistema (cadastro, lista, pesquisa, paginação, importação, nacionalidade, exclusão).
- `vendor/bootstrap-icons/`: ícones da biblioteca [Bootstrap Icons](https://icons.getbootstrap.com/) v1.11.3 (licença MIT), incluída no projeto para funcionar offline.

Como foi feito:

- **Comunicação com a API:** `fetch` nativo, com uma função `api()` que envia o cookie de sessão e o header CSRF e leva ao login se vier 401.
- **Layout:** CSS próprio (variáveis, Flexbox e Grid). Fundo branco, gradiente azul só nos botões. Responsivo: em telas pequenas a tabela vira cartões.
- **Campos com rótulo flutuante:** só CSS (`:placeholder-shown` e `:focus`).
- **Formatação inteligente:** máscara de CPF `000.000.000-00`, nome e sobrenome com inicial maiúscula (mantendo "de", "da", "do"), e sugestão de domínio de e-mail ao digitar `@` (lista com mouse ou setas e Enter).
- **Pesquisa e paginação:** feitas no navegador, por ID, nome, documento, e-mail e nacionalidade, com tamanho de página de 5, 10, 20 ou 50.
- **Modais, avisos e importação:** componentes feitos à mão, sem biblioteca.

## Estrutura

```
src/main/java/br/com/quipux/cadastropessoas/
  config/SecurityConfig.java        login por sessão, CSRF, rotas públicas
  controller/                       PessoaController (APIs), SessaoController (/api/me)
  service/                          PessoaService, NacionalidadeService (nationalize.io)
  repository/PessoaRepository.java  Spring Data JPA
  exception/                        erros de negócio e GlobalExceptionHandler
  Pessoa.java                       entidade
src/main/resources/
  application.properties            configurações (segredos vêm do .env)
  application-h2.properties         perfil sem MySQL
  static/                           login.html, index.html, vendor/
src/test/                           testes automatizados (H2)
```

## Problemas comuns

| Sintoma | Causa e solução |
|---|---|
| `Communications link failure` ao subir | MySQL desligado ou senha errada. Ligue o MySQL ou use a Opção A (perfil `h2`) |
| `Port 8080 was already in use` | Há outro servidor na porta 8080. Feche-o ou rode em outra porta: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| `Could not resolve placeholder 'APP_PASSWORD'` ao subir | Falta o `.env` ou a linha `APP_PASSWORD`. Faça o Passo 0 |
| Nacionalidade mostra "Limite de consultas atingido" | Limite diário da API gratuita. Tente de novo mais tarde |
| 403 ao chamar POST ou DELETE por curl | Falta o header `X-XSRF-TOKEN`. Veja "Chamando a API fora da tela" |
