package fatec.fusex.nexus.chefeFusex;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeFusex.dto.CadastrarMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoCadastradoResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class ChefeMedicoCadastroService {

    private static final int SENHA_MINIMO = 8;
    private static final int SENHA_MAXIMO = 72; // limite do BCrypt
    static final int NOME_MAXIMO = 150;
    static final int PATENTE_MAXIMO = 50;

    // CRM: 4 a 7 digitos + UF, com ou sem separador ("123456-SP", "123456/sp", "123456 SP")
    private static final Pattern CRM = Pattern.compile("^(\\d{4,7})[-/ ]?([A-Z]{2})$");
    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeMedicoCadastroService(UsuarioRepository usuarioRepository,
                                      MedicoFusexRepository medicoRepository,
                                      BeneficiarioRepository beneficiarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      SimulacaoLogin simulacaoLogin) {
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.simulacaoLogin = simulacaoLogin;
    }

    // T8-BE2: POST /chefe/medicos. Ordem: perfil (403) -> formato (422) -> duplicidade (409).
    // Usuario e medico sao gravados na mesma transacao: se um falhar, nenhum dos dois fica no banco.
    @Transactional
    public MedicoCadastradoResponse cadastrar(CadastrarMedicoRequest request) {
        validarChefe();

        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String patente = obrigatorio(request.patente(), "patente", PATENTE_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        String crm = normalizarCrm(request.crm());
        validarSenha(request.senha());

        // O CPF e o login: nao pode existir em nenhum usuario (beneficiario, medico ou outro)
        if (usuarioRepository.findByLogin(cpf).isPresent()) {
            throw new ConflitoException("cpf", "CPF ja cadastrado");
        }
        // Prec CP identifica o militar: nao pode repetir entre medicos nem entre beneficiarios
        if (medicoRepository.existsByPrecCp(precCp) || beneficiarioRepository.existsByPrecCp(precCp)) {
            throw new ConflitoException("precCp", "Prec CP ja cadastrado");
        }
        if (medicoRepository.existsByCrm(crm)) {
            throw new ConflitoException("crm", "CRM ja cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(cpf);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(PerfilUsuario.MEDICO);
        usuario.setAtivo(true);

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario);
        medico.setNome(nome);
        medico.setPatente(patente);
        medico.setPrecCp(precCp);
        medico.setCrm(crm);

        try {
            usuarioRepository.save(usuario);
            medicoRepository.saveAndFlush(medico);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros iguais ao mesmo tempo: as checagens acima passaram, o UNIQUE do banco barrou
            throw new ConflitoException("CPF, Prec CP ou CRM ja cadastrado");
        }

        return new MedicoCadastradoResponse(medico.getUsuarioId());
    }

    // Usado tambem pela edicao (T8-BE3)
    void validarChefe() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao chefe do FUSEX");
        }
    }

    // As regras de formato abaixo valem para o cadastro e para a edicao (T8-BE3), por isso nao sao private
    static String obrigatorio(String valor, String campo, int maximo) {
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

    // Mesmo formato do PrecCp do beneficiario: 11 digitos, com ou sem pontos e hifen
    static String normalizarPrecCp(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("precCp", "Campo obrigatorio");
        }
        String precCp = informado.trim().replace(".", "").replace("-", "");
        if (!precCp.matches("[0-9]{11}")) {
            throw new DadosInvalidosException("precCp", "Prec CP invalido: informe 11 digitos");
        }
        return precCp;
    }

    // Grava sempre no formato "123456-SP"
    static String normalizarCrm(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("crm", "Campo obrigatorio");
        }
        Matcher m = CRM.matcher(informado.trim().toUpperCase());
        if (!m.matches() || !UFS.contains(m.group(2))) {
            throw new DadosInvalidosException("crm", "CRM invalido: use numero e UF, ex.: 123456-SP");
        }
        return m.group(1) + "-" + m.group(2);
    }

    private static void validarSenha(String senha) {
        validarSenha(senha, "senha");
    }

    static void validarSenha(String senha, String campo) {
        if (senha == null || senha.length() < SENHA_MINIMO) {
            throw new DadosInvalidosException(campo, "A senha deve ter no minimo " + SENHA_MINIMO + " caracteres");
        }
        if (senha.length() > SENHA_MAXIMO) {
            throw new DadosInvalidosException(campo, "A senha deve ter no maximo " + SENHA_MAXIMO + " caracteres");
        }
    }
}
