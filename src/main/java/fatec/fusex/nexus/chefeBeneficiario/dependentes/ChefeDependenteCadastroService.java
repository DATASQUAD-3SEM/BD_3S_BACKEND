package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.validation.ValidacaoCadastro;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExterno;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Cadastro de dependente pelo chefe (T31-BE2). Tudo numa transacao. Prec CP e CPF unicos entre as pessoas;
 * menor com vigencia determinada e um responsavel (titular ou pessoa externa); maior sem responsavel, com conta
 * propria pelo CPF; responsavel externo: conta reaproveitada se ja for responsavel externo, senao criada.
 */
@Service
public class ChefeDependenteCadastroService {

    private static final int MAIORIDADE = 18;

    private final UsuarioLogado usuarioLogado;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final DependenteRepository dependenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ResponsavelExternoRepository responsavelExternoRepository;
    private final PasswordEncoder passwordEncoder;

    public ChefeDependenteCadastroService(UsuarioLogado usuarioLogado,
                                          BeneficiarioRepository beneficiarioRepository,
                                          MedicoFusexRepository medicoRepository,
                                          DependenteRepository dependenteRepository,
                                          UsuarioRepository usuarioRepository,
                                          ResponsavelExternoRepository responsavelExternoRepository,
                                          PasswordEncoder passwordEncoder) {
        this.usuarioLogado = usuarioLogado;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoRepository = medicoRepository;
        this.dependenteRepository = dependenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.responsavelExternoRepository = responsavelExternoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DependenteItemResponse criar(Long titularId, CriarDependenteRequest req) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        Beneficiario titular = beneficiarioRepository.findById(titularId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
        LocalDate hoje = Beneficiario.hoje();

        String nome = ValidacaoCadastro.obrigatorio(req.nome(), "nome", ValidacaoCadastro.NOME_MAXIMO);
        if (req.dataNascimento() == null || req.dataNascimento().isAfter(hoje)) {
            throw new DadosInvalidosException("dataNascimento", "Informe uma data de nascimento valida");
        }
        String cpf = ValidacaoCadastro.normalizarCpf(req.cpf());
        String precCp = ValidacaoCadastro.normalizarPrecCp(req.precCp());
        boolean menor = req.dataNascimento().plusYears(MAIORIDADE).isAfter(hoje);

        boolean determinada = "DETERMINADA".equals(req.tipoVigencia());
        if (menor && (!determinada || req.vigenciaFim() == null)) {
            throw new DadosInvalidosException("vigenciaFim", "Menor de idade precisa de vigencia determinada");
        }
        if (determinada && req.vigenciaFim() == null) {
            throw new DadosInvalidosException("vigenciaFim", "Informe a data final da vigencia");
        }
        LocalDate vigenciaFim = determinada ? req.vigenciaFim() : null;

        if (beneficiarioRepository.existsByPrecCp(precCp) || medicoRepository.existsByPrecCp(precCp)
                || dependenteRepository.findByPrecCp(precCp).isPresent()) {
            throw new DadosInvalidosException("precCp", "Prec CP ja cadastrado");
        }
        if (dependenteRepository.existsByCpf(cpf)) {
            throw new DadosInvalidosException("cpf", "CPF ja cadastrado");
        }

        Usuario contaDoDependente = null;
        Usuario responsavel = null;
        TipoDependente tipo;
        List<String> nomesResponsaveis;
        if (menor) {
            if (req.responsavel() == null || req.responsavel().tipo() == null) {
                throw new DadosInvalidosException("responsavel", "Informe o responsavel legal do menor");
            }
            if ("TITULAR".equals(req.responsavel().tipo())) {
                tipo = TipoDependente.MENOR_TITULAR;
            } else {
                tipo = TipoDependente.MENOR_RESPONSAVEL;
                responsavel = responsavelExterno(req.responsavel(), cpf, req.senha());
            }
        } else {
            if (req.responsavel() != null) {
                throw new DadosInvalidosException("responsavel", "Maior de idade nao aceita responsavel legal");
            }
            tipo = TipoDependente.MAIOR;
            contaDoDependente = contaDoMaior(cpf, req.senha());
        }

        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setUsuario(contaDoDependente);
        d.setResponsavel(responsavel);
        d.setNome(nome);
        d.setDataNascimento(req.dataNascimento());
        d.setCpf(cpf);
        d.setTelefone(apenasDigitos(req.telefone()));
        d.setPrecCp(precCp);
        d.setVinculo(req.vinculo());
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        dependenteRepository.save(d);

        nomesResponsaveis = tipo == TipoDependente.MENOR_TITULAR ? List.of(titular.getNome())
                : tipo == TipoDependente.MENOR_RESPONSAVEL
                        ? responsavelExternoRepository.findById(responsavel.getIdUsuario())
                                .map(r -> List.of(r.getNome())).orElse(List.of())
                        : List.of();
        List<ResponsavelDependenteResponse> responsaveis = nomesResponsaveis.isEmpty() ? List.of()
                : List.of(new ResponsavelDependenteResponse(
                        tipo == TipoDependente.MENOR_TITULAR ? titular.getUsuarioId() : responsavel.getIdUsuario(),
                        nomesResponsaveis.get(0),
                        tipo == TipoDependente.MENOR_TITULAR ? "TITULAR" : "EXTERNO"));
        return DependenteItemResponse.de(d, hoje, responsaveis);
    }

    /**
     * Maior de idade (Jira, T31-BE2): conta nova com perfil BENEFICIARIO; se o CPF já é de um responsável externo,
     * reaproveita a conta, troca o perfil para BENEFICIARIO e mantém os vínculos de responsável. Outro CPF: 422.
     * Na reutilização, a senha atual da conta é mantida (a senha informada não é usada).
     */
    private Usuario contaDoMaior(String cpf, String senha) {
        Usuario existente = usuarioRepository.findByLogin(cpf).orElse(null);
        if (existente == null) {
            return novaConta(cpf, PerfilUsuario.BENEFICIARIO, senha);
        }
        if (existente.getPerfil() != PerfilUsuario.RESPONSAVEL_EXTERNO) {
            throw new DadosInvalidosException("cpf", "CPF ja possui conta no sistema");
        }
        existente.setPerfil(PerfilUsuario.BENEFICIARIO);
        return usuarioRepository.save(existente);
    }

    /**
     * Pessoa externa como responsavel legal (Jira, T31-BE2). CPF novo: conta RESPONSAVEL_EXTERNO com a senha do
     * cadastro. CPF de conta existente: reaproveita se for responsável externo ou dependente maior vitalício
     * (o maior ganha o vínculo de ResponsavelExterno e segue como BENEFICIARIO). Dependente com vigência
     * determinada, titular e demais perfis: 422.
     */
    private Usuario responsavelExterno(CriarDependenteRequest.ResponsavelRequest r, String cpfDoMenor, String senha) {
        String cpfResp = ValidacaoCadastro.normalizarCpf(r.cpf());
        if (cpfResp.equals(cpfDoMenor)) {
            throw new DadosInvalidosException("responsavel", "O responsavel nao pode ter o mesmo CPF do menor");
        }
        Usuario existente = usuarioRepository.findByLogin(cpfResp).orElse(null);
        if (existente == null) {
            Usuario novo = novaConta(cpfResp, PerfilUsuario.RESPONSAVEL_EXTERNO, senha);
            ResponsavelExterno externo = new ResponsavelExterno();
            externo.setUsuarioId(novo.getIdUsuario());
            externo.setNome(ValidacaoCadastro.obrigatorio(r.nome(), "responsavel", ValidacaoCadastro.NOME_MAXIMO));
            externo.setTelefone(apenasDigitos(r.telefone()));
            responsavelExternoRepository.save(externo);
            return novo;
        }
        if (existente.getPerfil() == PerfilUsuario.RESPONSAVEL_EXTERNO) {
            return existente;
        }
        boolean ehTitular = beneficiarioRepository.existsById(existente.getIdUsuario());
        Dependente maior = ehTitular ? null
                : dependenteRepository.findByUsuarioIdUsuario(existente.getIdUsuario()).orElse(null);
        if (maior == null || maior.getVigenciaFim() != null) {
            throw new DadosInvalidosException("responsavel",
                    "Este CPF pertence a outra pessoa do sistema e nao pode ser responsavel legal");
        }
        if (!responsavelExternoRepository.existsById(existente.getIdUsuario())) {
            ResponsavelExterno externo = new ResponsavelExterno();
            externo.setUsuarioId(existente.getIdUsuario());
            externo.setNome(ValidacaoCadastro.obrigatorio(r.nome(), "responsavel", ValidacaoCadastro.NOME_MAXIMO));
            externo.setTelefone(apenasDigitos(r.telefone()));
            responsavelExternoRepository.save(externo);
        }
        return existente;
    }

    /**
     * Consulta, sem gravar nada, se o CPF ja tem conta e se ela pode ser reaproveitada (regra 13, comentario de
     * revisao): mesma classificacao usada em contaDoMaior e responsavelExterno.
     */
    @Transactional(readOnly = true)
    public ContaPorCpfResponse consultarConta(String cpfInformado) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        String cpf = ValidacaoCadastro.normalizarCpf(cpfInformado);
        Usuario existente = usuarioRepository.findByLogin(cpf).orElse(null);
        if (existente == null) {
            return new ContaPorCpfResponse("NOVA", null);
        }
        if (existente.getPerfil() == PerfilUsuario.RESPONSAVEL_EXTERNO) {
            String nome = responsavelExternoRepository.findById(existente.getIdUsuario())
                    .map(ResponsavelExterno::getNome).orElse(null);
            return new ContaPorCpfResponse("RESPONSAVEL_EXTERNO", nome);
        }
        if (!beneficiarioRepository.existsById(existente.getIdUsuario())) {
            Dependente maior = dependenteRepository.findByUsuarioIdUsuario(existente.getIdUsuario()).orElse(null);
            if (maior != null && maior.getVigenciaFim() == null) {
                return new ContaPorCpfResponse("DEPENDENTE_MAIOR_VITALICIO", maior.getNome());
            }
        }
        return new ContaPorCpfResponse("OUTRA", null);
    }

    private Usuario novaConta(String login, PerfilUsuario perfil, String senha) {
        if (usuarioRepository.findByLogin(login).isPresent()) {
            throw new DadosInvalidosException("cpf", "CPF ja possui conta no sistema");
        }
        ValidacaoCadastro.validarSenha(senha, "senha");
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    /** T31-BE3: edita nome, telefone, vinculo, Prec CP e vigencia. Responsavel, CPF e nascimento nao mudam. */
    @Transactional
    public DependenteItemResponse editar(Long dependenteId, EditarDependenteRequest req) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        Dependente d = dependenteRepository.findById(dependenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dependente nao encontrado"));
        LocalDate hoje = Beneficiario.hoje();

        if (req.nome() != null) {
            d.setNome(ValidacaoCadastro.obrigatorio(req.nome(), "nome", ValidacaoCadastro.NOME_MAXIMO));
        }
        if (req.telefone() != null) {
            d.setTelefone(apenasDigitos(req.telefone()));
        }
        if (req.vinculo() != null) {
            d.setVinculo(req.vinculo());
        }
        if (req.precCp() != null) {
            String precCp = ValidacaoCadastro.normalizarPrecCp(req.precCp());
            boolean ocupado = beneficiarioRepository.existsByPrecCp(precCp) || medicoRepository.existsByPrecCp(precCp)
                    || dependenteRepository.findByPrecCp(precCp)
                            .filter(outro -> !outro.getIdDependente().equals(dependenteId)).isPresent();
            if (!precCp.equals(d.getPrecCp()) && ocupado) {
                throw new DadosInvalidosException("precCp", "Prec CP ja cadastrado");
            }
            d.setPrecCp(precCp);
        }

        String tipoVigencia = req.tipoVigencia() != null ? req.tipoVigencia()
                : (d.getVigenciaFim() == null ? "VITALICIA" : "DETERMINADA");
        LocalDate fim = req.tipoVigencia() == null && req.vigenciaFim() == null ? d.getVigenciaFim() : req.vigenciaFim();
        boolean determinada = "DETERMINADA".equals(tipoVigencia);
        boolean menor = d.getTipo() != TipoDependente.MAIOR;
        if (determinada && fim == null) {
            throw new DadosInvalidosException("vigenciaFim", "Informe a data final da vigencia");
        }
        if (menor && !determinada) {
            throw new DadosInvalidosException("vigenciaFim", "Menor de idade precisa de vigencia determinada");
        }
        d.setVigenciaFim(determinada ? fim : null);

        dependenteRepository.save(d);
        return DependenteItemResponse.de(d, hoje, responsaveisDe(d));
    }

    private List<ResponsavelDependenteResponse> responsaveisDe(Dependente d) {
        Beneficiario titular = d.getTitular();
        if (d.getTipo() == TipoDependente.MENOR_TITULAR) {
            return List.of(new ResponsavelDependenteResponse(titular.getUsuarioId(), titular.getNome(), "TITULAR"));
        }
        if (d.getTipo() == TipoDependente.MENOR_RESPONSAVEL && d.getResponsavel() != null) {
            return responsavelExternoRepository.findById(d.getResponsavel().getIdUsuario())
                    .map(r -> List.of(new ResponsavelDependenteResponse(r.getUsuarioId(), r.getNome(), "EXTERNO")))
                    .orElse(List.of());
        }
        return List.of();
    }

    private static String apenasDigitos(String valor) {
        return valor == null || valor.isBlank() ? null : valor.replaceAll("[^0-9]", "");
    }
}
