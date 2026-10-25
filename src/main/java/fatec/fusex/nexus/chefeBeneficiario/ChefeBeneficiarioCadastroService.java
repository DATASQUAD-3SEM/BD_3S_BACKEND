package fatec.fusex.nexus.chefeBeneficiario;

import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.NOME_MAXIMO;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarCpf;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarPrecCp;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.obrigatorio;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.validarSenha;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioCadastradoResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.CadastrarBeneficiarioRequest;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T7-BE2: POST /chefe/beneficiarios.
 * Idade e faixa etaria nao sao gravadas: saem sempre da data de nascimento (modelo logico 3.2).
 */
@Service
public class ChefeBeneficiarioCadastroService {

    // Mesmo padrao do PATCH /beneficiarios/me/telefone: celular com DDD, com ou sem mascara
    private static final String PADRAO_TELEFONE =
            "(?:[1-9][1-9]9[0-9]{8}"
                    + "|\\([1-9][1-9]\\) ?9[0-9]{4}-?[0-9]{4}"
                    + "|[1-9][1-9] 9[0-9]{4}-?[0-9]{4})";

    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;

    public ChefeBeneficiarioCadastroService(UsuarioRepository usuarioRepository,
                                            BeneficiarioRepository beneficiarioRepository,
                                            MedicoFusexRepository medicoRepository,
                                            PasswordEncoder passwordEncoder,
                                            UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> formato (422) -> duplicidade (409).
    // Usuario e beneficiario sao gravados na mesma transacao: se um falhar, nenhum dos dois fica no banco.
    @Transactional
    public BeneficiarioCadastradoResponse cadastrar(CadastrarBeneficiarioRequest request) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

        if (request == null) {
            throw new DadosInvalidosException("nome", "Campo obrigatorio");
        }
        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        LocalDate dataNascimento = validarDataNascimento(request.dataNascimento());
        String telefone = normalizarTelefone(request.telefone());
        validarSenha(request.senha(), "senha");

        // O CPF e o login: nao pode existir em nenhum usuario (beneficiario, medico ou outro)
        if (usuarioRepository.findByLogin(cpf).isPresent()) {
            throw new ConflitoException("cpf", "CPF ja cadastrado");
        }
        // Prec CP identifica o militar: nao pode repetir entre beneficiarios nem entre medicos
        if (beneficiarioRepository.existsByPrecCp(precCp) || medicoRepository.existsByPrecCp(precCp)) {
            throw new ConflitoException("precCp", "Prec CP ja cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(cpf);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(true);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario);
        beneficiario.setNome(nome);
        beneficiario.setPrecCp(precCp);
        beneficiario.setDataNascimento(dataNascimento);
        beneficiario.setTelefone(telefone);

        try {
            usuarioRepository.save(usuario);
            beneficiarioRepository.saveAndFlush(beneficiario);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros iguais ao mesmo tempo: as checagens acima passaram, o UNIQUE do banco barrou
            throw new ConflitoException("CPF ou Prec CP ja cadastrado");
        }

        return new BeneficiarioCadastradoResponse(beneficiario.getUsuarioId());
    }

    // Formato aaaa-mm-dd e sempre no passado (hoje no fuso de Sao Paulo, como o calculo da idade)
    static LocalDate validarDataNascimento(String informada) {
        if (informada == null || informada.isBlank()) {
            throw new DadosInvalidosException("dataNascimento", "Campo obrigatorio");
        }
        LocalDate data;
        try {
            data = LocalDate.parse(informada.trim());
        } catch (DateTimeParseException e) {
            throw new DadosInvalidosException("dataNascimento", "Data invalida: use aaaa-mm-dd");
        }
        if (!data.isBefore(Beneficiario.hoje())) {
            throw new DadosInvalidosException("dataNascimento", "A data de nascimento deve estar no passado");
        }
        return data;
    }

    // Grava so os digitos (coluna de 11)
    static String normalizarTelefone(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("telefone", "Campo obrigatorio");
        }
        String telefone = informado.trim();
        if (!telefone.matches(PADRAO_TELEFONE)) {
            throw new DadosInvalidosException("telefone", "Informe um telefone celular valido com DDD");
        }
        return telefone.replaceAll("\\D", "");
    }

}
