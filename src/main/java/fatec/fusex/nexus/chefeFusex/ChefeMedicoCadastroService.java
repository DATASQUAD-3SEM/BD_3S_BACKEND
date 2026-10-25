package fatec.fusex.nexus.chefeFusex;

import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.NOME_MAXIMO;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarCpf;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarPrecCp;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.obrigatorio;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.validarSenha;

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
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class ChefeMedicoCadastroService {

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
    private final UsuarioLogado usuarioLogado;

    public ChefeMedicoCadastroService(UsuarioRepository usuarioRepository,
                                      MedicoFusexRepository medicoRepository,
                                      BeneficiarioRepository beneficiarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
    }

    // T8-BE2: POST /chefe/medicos. Ordem: perfil (403) -> formato (422) -> duplicidade (409).
    // Usuario e medico sao gravados na mesma transacao: se um falhar, nenhum dos dois fica no banco.
    @Transactional
    public MedicoCadastradoResponse cadastrar(CadastrarMedicoRequest request) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String patente = obrigatorio(request.patente(), "patente", PATENTE_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        String crm = normalizarCrm(request.crm());
        validarSenha(request.senha(), "senha");

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

}
