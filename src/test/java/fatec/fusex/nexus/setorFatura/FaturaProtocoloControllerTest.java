package fatec.fusex.nexus.setorFatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.espelho.StatusEspelho;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FaturaProtocoloControllerTest {

    private static final String LOGIN_SETOR = "setor.protocolo";
    private static final String LOGIN_OCS = "ocs.protocolo";
    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final String RESPONSAVEL = "Ana Silva";
    private static final String URL_PROXIMO = "/setor-fatura/faturas/{id}/proximo-numero";
    private static final String URL_PROTOCOLO = "/setor-fatura/faturas/{id}/protocolo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OcsRepository ocsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspelhoRepository espelhoRepository;

    private Ocs ocs;
    private Usuario setor;
    private int ano;

    @BeforeEach
    void cenario() {
        ocs = novaOcs("Laboratorio Protocolo", "CTR-PROT-1");
        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_OCS, PerfilUsuario.OCS);
        ano = LocalDate.now(Beneficiario.FUSO).getYear();
    }

    @Test
    void sugestaoEOProximoNumeroDaOcsNoAno() throws Exception {
        novoEspelho(ocs, SituacaoFatura.PROTOCOLADA, numero(3));
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(get(URL_PROXIMO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").value(numero(4)));
    }

    @Test
    void sugestaoComecaEmUmQuandoNaoHaFaturaNoAno() throws Exception {
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(get(URL_PROXIMO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").value(numero(1)));
    }

    @Test
    void sugestaoIgnoraFaturasDeOutraOcsEDeOutroAno() throws Exception {
        Ocs outra = novaOcs("Clinica Outra", "CTR-PROT-2");
        novoEspelho(outra, SituacaoFatura.PROTOCOLADA, numero(7));
        novoEspelho(ocs, SituacaoFatura.PROTOCOLADA, String.format("009/%d", ano - 1));
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(get(URL_PROXIMO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").value(numero(1)));
    }

    @Test
    void protocoloGravaNumeroSituacaoEEventoComOResponsavel() throws Exception {
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), RESPONSAVEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PROTOCOLADA"))
                .andExpect(jsonPath("$.numeroFatura").value(numero(1)))
                .andExpect(jsonPath("$.eventos", hasSize(1)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("PROTOCOLADA"))
                .andExpect(jsonPath("$.eventos[0].responsavel").value(RESPONSAVEL))
                .andExpect(jsonPath("$.eventos[0].usuario").value(LOGIN_SETOR));

        Espelho gravado = espelhoRepository.findById(recebido.getIdEspelho()).orElseThrow();
        assertThat(gravado.getSituacaoFatura()).isEqualTo(SituacaoFatura.PROTOCOLADA);
        assertThat(gravado.getNumeroFatura()).isEqualTo(numero(1));
        assertThat(gravado.getProtocoladaPor().getLogin()).isEqualTo(LOGIN_SETOR);
    }

    @Test
    void formatoInvalidoRecebe400ENadaEGravado() throws Exception {
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("2026/1", RESPONSAVEL)))
                .andExpect(status().isBadRequest());

        assertThat(espelhoRepository.findById(recebido.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.RECEBIDO);
    }

    @Test
    void numeroRepetidoNaMesmaOcsRecebe409ENadaEGravado() throws Exception {
        novoEspelho(ocs, SituacaoFatura.PROTOCOLADA, numero(1));
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), RESPONSAVEL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma fatura desta OCS com este número."));

        assertThat(espelhoRepository.findById(recebido.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.RECEBIDO);
    }

    @Test
    void mesmoNumeroEmOutraOcsEAceito() throws Exception {
        Ocs outra = novaOcs("Clinica Outra", "CTR-PROT-2");
        novoEspelho(outra, SituacaoFatura.PROTOCOLADA, numero(1));
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), RESPONSAVEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PROTOCOLADA"));
    }

    @Test
    void responsavelVazioRecebe422() throws Exception {
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), "  ")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(espelhoRepository.findById(recebido.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.RECEBIDO);
    }

    @Test
    void segundoProtocoloNoMesmoEspelhoRecebe409() throws Exception {
        Espelho protocolado = novoEspelho(ocs, SituacaoFatura.PROTOCOLADA, numero(1));

        mockMvc.perform(post(URL_PROTOCOLO, protocolado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(2), RESPONSAVEL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este espelho já tem nº de fatura."));
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        Espelho recebido = novoEspelho(ocs, SituacaoFatura.RECEBIDO, null);

        mockMvc.perform(get(URL_PROXIMO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(URL_PROTOCOLO, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), RESPONSAVEL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void faturaInexistenteOuEmConstrucaoRecebe404() throws Exception {
        Espelho emConstrucao = novoEspelho(ocs, null, null);

        mockMvc.perform(get(URL_PROXIMO, emConstrucao.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(URL_PROTOCOLO, 999999L)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(numero(1), RESPONSAVEL)))
                .andExpect(status().isNotFound());
    }

    private String numero(int sequencia) {
        return String.format("%03d/%d", sequencia, ano);
    }

    private String corpo(String numeroFatura, String responsavel) {
        return "{\"numeroFatura\":\"" + numeroFatura + "\",\"responsavel\":\"" + responsavel + "\"}";
    }

    private Espelho novoEspelho(Ocs dona, SituacaoFatura situacao, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(dona);
        espelho.setStatus(situacao == null ? StatusEspelho.EM_CONSTRUCAO : StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(situacao == null ? null : ENVIO);
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(numero == null ? null : setor);
        return espelhoRepository.save(espelho);
    }

    private Ocs novaOcs(String nome, String contrato) {
        Ocs nova = new Ocs();
        nova.setNome(nome);
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum(contrato);
        return ocsRepository.save(nova);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}