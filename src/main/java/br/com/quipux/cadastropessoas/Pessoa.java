package br.com.quipux.cadastropessoas;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A registered person. Document and e-mail are unique (also enforced by database constraints).
 * The validation messages are shown to end users, so they stay in Portuguese.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "userDocument"), @UniqueConstraint(columnNames = "userEmail")})
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank (message = "O nome é obrigatório!")
    private String userName;
    @NotBlank (message = "O sobrenome é obrigatório!")
    private String userSurname;

    // Brazilian CPF without punctuation: exactly 11 digits (the UI sends it already stripped)
    @NotBlank (message = "O documento é obrigatório!")
    @Pattern (regexp = "\\d{11}", message = "O documento deve conter 11 dígitos numéricos (CPF)")
    private String userDocument;
    @NotBlank (message = "O e-mail é obrigatório!")
    @Email  (message = "Informe um e-mail válido")
    private String userEmail;

    // Filled only by the nationality lookup. READ_ONLY keeps clients from sending it when registering.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nacionalidade;

    public Pessoa (){

    }

    public Long getUserId (){
        return userId;
    }

    public String getUserName (){
        return userName;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public String getUserSurname (){
        return userSurname;
    }

    public void setUserSurname(String userSurname){
        this.userSurname = userSurname;
    }

    public String getUserDocument (){
        return userDocument;
    }

    public void setUserDocument (String userDocument){
        this.userDocument = userDocument;
    }

    public String getNacionalidade (){
        return nacionalidade;
    }

    public void setNacionalidade (String nacionalidade){
        this.nacionalidade = nacionalidade;
    }

    public String getUserEmail (){
        return userEmail;
    }

    public void setUserEmail (String userEmail){
        this.userEmail = userEmail;
    }

}
