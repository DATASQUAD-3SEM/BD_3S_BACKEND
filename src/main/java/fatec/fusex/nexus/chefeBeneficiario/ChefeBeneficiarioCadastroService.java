package fatec.fusex.nexus.chefeBeneficiario;

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
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
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

    private static final int NOME_MAXIMO = 150;
    private static final int SENHA_MINIMO = 8;
    private static final int SENHA_MAXIMO = 72; // limite do BCrypt

    // Mesmo padrao do PATCH /beneficiarios/me/telefone: celular com DDD, com ou sem mascara
    private static final String PADRAO_TELEFONE =
            "(?:[1-9][1-9]9[0-9]{8}"
                    + "|\\([1-9][1-9]\\) ?9[0-9]{4}-?[0-9]{4}"
                    + "|[1-9][1-9] 9[0-9]{4}-?[0-9]{4})";

    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeBeneficiarioCadastroService(UsuarioRepository usuarioRepository,
                                            BeneficiarioRepository beneficiarioRepository,
                                            MedicoFusexRepository medicoRepository,
                                            PasswordEncoder passwordEncoder,
                                            SimulacaoLogin simulacaoLogin) {
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.simulacaoLogin = simulacaoLogin;
    }

    // Ordem: perfil (403) -> formato (422) -> duplicidade (409).
    // Usuario e beneficiario sao gravados na mesma transacao: se um falhar, nenhum dos dois fica no banco.
    @Transactional
    public BeneficiarioCadastradoResponse cadastrar(CadastrarBeneficiarioRequest request) {
        validarChefe();

        if (request == null) {
            throw new DadosInvalidosException("nome", "Campo obrigatorio");
        }
        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        LocalDate dataNascimento = validarDataNascimento(request.dataNascimento());
        String telefone = normalizarTelefone(request.telefone());
        validarSenha(request.senha());

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

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao Chefe do FUSEX");
        }
    }

    private static String obrigatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException(campo, "Campo obrigatorio");
        }
        String limpo = valor.trim();
        if (limpo.length() > maximo) {
            throw new DadosInvalidosException(campo, "Maximo de " + maximo + " caracteres");
        }
        return limpo;
    }

    // Aceita com ou sem mascara (000.000.000-00) e confere os 2 digitos verificadores
    static String normalizarCpf(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("cpf", "Campo obrigatorio");
        }
        String cpf = informado.trim().replace(".", "").replace("-", "");
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1 || !digitosConferem(cpf)) {
            throw new DadosInvalidosException("cpf", "CPF invalido");
        }
        return cpf;
    }

    private static boolean digitosConferem(String cpf) {
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
                && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digitoVerificador(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    // 11 digitos, com ou sem pontos e hifen (mesmo formato do Prec CP no resto do projeto)
    private static String normalizarPrecCp(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("precCp", "Campo obrigatorio");
        }
        String precCp = informado.trim().replace(".", "").replace("-", "");
        if (!precCp.matches("[0-9]{11}")) {
            throw new DadosInvalidosException("precCp", "Prec CP invalido: informe 11 digitos");
        }
        return precCp;
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

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMO) {
            throw new DadosInvalidosException("senha", "A senha deve ter no minimo " + SENHA_MINIMO + " caracteres");
        }
        if (senha.length() > SENHA_MAXIMO) {
            throw new DadosInvalidosException("senha", "A senha deve ter no maximo " + SENHA_MAXIMO + " caracteres");
        }
    }
}
