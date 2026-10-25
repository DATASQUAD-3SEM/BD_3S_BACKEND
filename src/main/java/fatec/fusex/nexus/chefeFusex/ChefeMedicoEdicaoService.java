package fatec.fusex.nexus.chefeFusex;

import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.NOME_MAXIMO;
import static fatec.fusex.nexus.chefeFusex.ChefeMedicoCadastroService.PATENTE_MAXIMO;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarCpf;
import static fatec.fusex.nexus.chefeFusex.ChefeMedicoCadastroService.normalizarCrm;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.normalizarPrecCp;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.obrigatorio;
import static fatec.fusex.nexus.common.validation.ValidacaoCadastro.validarSenha;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeFusex.dto.EditarMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoChefeDetalheResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** T8-BE3: GET e PUT /chefe/medicos/{id}. Usa as mesmas regras de formato do cadastro (T8-BE2). */
@Service
public class ChefeMedicoEdicaoService {

    private final UsuarioLogado usuarioLogado;
    private final MedicoFusexRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ChefeMedicoEdicaoService(UsuarioLogado usuarioLogado,
                                    MedicoFusexRepository medicoRepository,
                                    UsuarioRepository usuarioRepository,
                                    BeneficiarioRepository beneficiarioRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioLogado = usuarioLogado;
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public MedicoChefeDetalheResponse buscar(Long id) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        return MedicoChefeDetalheResponse.de(buscarMedico(id));
    }

    // Ordem: perfil (403) -> existe (404) -> formato (422) -> duplicidade com OUTRO usuario (409)
    @Transactional
    public MedicoChefeDetalheResponse editar(Long id, EditarMedicoRequest request) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        MedicoFusex medico = buscarMedico(id);

        String nome = obrigatorio(request.nome(), "nome", NOME_MAXIMO);
        String patente = obrigatorio(request.patente(), "patente", PATENTE_MAXIMO);
        String cpf = normalizarCpf(request.cpf());
        String precCp = normalizarPrecCp(request.precCp());
        String crm = normalizarCrm(request.crm());
        if (request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }
        boolean trocarSenha = request.novaSenha() != null && !request.novaSenha().isEmpty();
        if (trocarSenha) {
            validarSenha(request.novaSenha(), "novaSenha");
        }

        // Manter o proprio CPF, Prec CP ou CRM nao e duplicidade
        boolean cpfDeOutro = usuarioRepository.findByLogin(cpf)
                .filter(u -> !u.getIdUsuario().equals(id))
                .isPresent();
        if (cpfDeOutro) {
            throw new ConflitoException("cpf", "CPF ja cadastrado");
        }
        if (medicoRepository.existsByPrecCpAndUsuarioIdNot(precCp, id) || beneficiarioRepository.existsByPrecCp(precCp)) {
            throw new ConflitoException("precCp", "Prec CP ja cadastrado");
        }
        if (medicoRepository.existsByCrmAndUsuarioIdNot(crm, id)) {
            throw new ConflitoException("crm", "CRM ja cadastrado");
        }

        Usuario usuario = medico.getUsuario();
        usuario.setLogin(cpf); // o CPF e o login: trocar o CPF troca o login
        usuario.setAtivo(request.ativo());
        if (trocarSenha) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }

        medico.setNome(nome);
        medico.setPatente(patente);
        medico.setPrecCp(precCp);
        medico.setCrm(crm);

        try {
            medicoRepository.saveAndFlush(medico);
        } catch (DataIntegrityViolationException e) {
            // Duas edicoes ao mesmo tempo: as checagens acima passaram, o UNIQUE do banco barrou
            throw new ConflitoException("CPF, Prec CP ou CRM ja cadastrado");
        }

        return MedicoChefeDetalheResponse.de(medico);
    }

    private MedicoFusex buscarMedico(Long id) {
        return medicoRepository.findWithUsuarioByUsuarioId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medico nao encontrado"));
    }
}
