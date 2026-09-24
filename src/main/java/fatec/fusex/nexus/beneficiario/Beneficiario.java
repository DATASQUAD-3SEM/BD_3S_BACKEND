package fatec.fusex.nexus.beneficiario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabela beneficiario. cpf e prec_cp sao unicos. A senha e sempre guardada com BCrypt. */
@Entity
@Table(name = "beneficiario")
public class Beneficiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    private Integer idade;

    @Column(name = "prec_cp", nullable = false, unique = true, length = 20)
    private String precCp;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 100)
    private String senha;

    public Beneficiario() {
    }

    public Long getId() { return id; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }

    public String getPrecCp() { return precCp; }
    public void setPrecCp(String precCp) { this.precCp = precCp; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
