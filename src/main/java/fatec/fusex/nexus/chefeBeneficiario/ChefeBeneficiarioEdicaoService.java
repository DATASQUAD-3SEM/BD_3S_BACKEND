package fatec.fusex.nexus.chefeBeneficiario;

import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.NOME_MAXIMO;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.normalizarCpf;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.normalizarPrecCp;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.normalizarTelefone;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.obrigatorio;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.validarDataNascimento;
import static fatec.fusex.nexus.chefeBeneficiario.ChefeBeneficiarioCadastroService.validarSenha;

import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioDetalheResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.EditarBeneficiarioRequest;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T7-BE3: GET e PUT /chefe/beneficiarios/{id}. Usa as mesmas regras de formato do cadastro (T7-BE2).
 * Idade e faixa etaria nunca sao gravadas: saem da data de nascimento.
 */
@Service
public class ChefeBeneficiarioEdicaoService {

    private final ChefeBeneficiarioCadastroService cadastroService;
    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;

    public ChefeBeneficiarioEdicaoService(ChefeBeneficiarioCadastroService cadastroService,
                                          BeneficiarioRepository beneficiarioRepository,
                                          UsuarioRepository usuarioRepository,
                                          MedicoFusexRepository medicoRepository,
                                          PasswordEncoder passwordEncoder) {
        this.cadastroService = cadastroService;
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public BeneficiarioDetalheResponse buscar(Long id) {
        cadastroService.validarChefe();
        return BeneficiarioDetalheResponse.de(buscarBeneficiario(id));
    }

    // Ordem: perfil (403) -> existe (404) -> formato (422) -> duplicidade com OUTRO usuario (409)
    @Transactional
    public BeneficiarioDetalheResponse editar(Long id, EditarBeneficiarioRequest request) {
        cadastroService.validarChefe();
        Beneficiario beneficiario = buscarBeneficiario(id);

        if (request == null) {
            throw new DadosInvalidosException("nome", "Campo obrigatorio");
        }
        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        LocalDate dataNascimento = validarDataNascimento(request.dataNascimento());
        String telefone = normalizarTelefone(request.telefone());
        if (request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }
        boolean trocarSenha = request.novaSenha() != null && !request.novaSenha().isEmpty();
        if (trocarSenha) {
            validarSenha(request.novaSenha(), "novaSenha");
        }

        // Manter o proprio CPF ou Prec CP nao e duplicidade
        boolean cpfDeOutro = usuarioRepository.findByLogin(cpf)
                .filter(u -> !u.getIdUsuario().equals(id))
                .isPresent();
        if (cpfDeOutro) {
            throw new ConflitoException("cpf", "CPF ja cadastrado");
        }
        if (beneficiarioRepository.existsByPrecCpAndUsuarioIdNot(precCp, id) || medicoRepository.existsByPrecCp(precCp)) {
            throw new ConflitoException("precCp", "Prec CP ja cadastrado");
        }

        Usuario usuario = beneficiario.getUsuario();
        usuario.setLogin(cpf); // o CPF e o login: trocar o CPF troca o login
        usuario.setAtivo(request.ativo());
        if (trocarSenha) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }

        beneficiario.setNome(nome);
        beneficiario.setPrecCp(precCp);
        beneficiario.setDataNascimento(dataNascimento);
        beneficiario.setTelefone(telefone);

        try {
            beneficiarioRepository.saveAndFlush(beneficiario);
        } catch (DataIntegrityViolationException e) {
            // Duas edicoes ao mesmo tempo: as checagens acima passaram, o UNIQUE do banco barrou
            throw new ConflitoException("CPF ou Prec CP ja cadastrado");
        }

        return BeneficiarioDetalheResponse.de(beneficiario);
    }

    private Beneficiario buscarBeneficiario(Long id) {
        return beneficiarioRepository.findWithUsuarioByUsuarioId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }
}
