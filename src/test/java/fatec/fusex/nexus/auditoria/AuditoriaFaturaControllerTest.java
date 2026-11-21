package fatec.fusex.nexus.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.OrigemContrato;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.espelho.StatusEspelho;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaFaturaControllerTest {

    private static final String LOGIN_AUDITORIA = "auditoria.teste";
    private static final String LOGIN_RASTREABILIDADE = "rastreabilidade.teste";
    private static final String LOGIN_SETOR = "setor.teste";
    private static final String LOGIN_BENEFICIARIO = "55544433322";
    private static final String COD = "41001200";
    private static final String BASE = "/auditoria/faturas";

    @Autowired private MockMvc mockMvc;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaProcedimentoRepository procedimentoRepository;
    @Autowired private EspelhoRepository espelhoRepository;
    @Autowired private EspelhoItemRepository espelhoItemRepository;

    private Ocs ocs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Auditoria");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-AUDITORIA-1");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_RASTREABILIDADE, PerfilUsuario.RASTREABILIDADE);
        novoUsuario(LOGIN_AUDITORIA, PerfilUsuario.AUDITORIA);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Auditoria");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void auditoriaListaSoFaturasEmAuditoria() throws Exception {
        novoEspelho(SituacaoFatura.EM_AUDITORIA);
        novoEspelho(SituacaoFatura.EM_LISURA);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_AUDITORIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].situacao").value("EM_AUDITORIA"));
    }

    @Test
    void outroPerfilNaoListaAuditoria() throws Exception {
        novoEspelho(SituacaoFatura.EM_AUDITORIA);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isForbidden());
    }

    private Espelho novoEspelho(SituacaoFatura situacao) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 10, 9, 0));
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura("0" + (++sequencia) + "0/2026");
        espelho.setProtocoladaPor(setor);
        espelho = espelhoRepository.save(espelho);

        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), COD));
        item.setQuantidadeSessoes(2);
        item.setValorSessao(new BigDecimal("100.00"));
        item.setOrigemContrato(OrigemContrato.CONFERIDO);
        item.setValorContratoSessao(new BigDecimal("100.00"));
        espelhoItemRepository.save(item);
        return espelho;
    }

    private Guia novaGuia() {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia = preGuiaRepository.save(preGuia);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", ++sequencia));
        guia.setArquivoUrl("guia.pdf");
        guia.setDataInsercao(LocalDateTime.of(2026, 9, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia.setSituacaoExecucao(SituacaoExecucao.REALIZADA);
        guia.setDataEnvio(LocalDateTime.of(2026, 10, 1, 10, 0));
        guia.setEnviadaPor(setor);
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento novoProcedimento(Guia guia, String codDgp) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp(codDgp);
        procedimento.setDescricaoProcedimento("Exame de auditoria");
        procedimento.setQuantidadeAutorizada(2);
        return procedimentoRepository.save(procedimento);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}
