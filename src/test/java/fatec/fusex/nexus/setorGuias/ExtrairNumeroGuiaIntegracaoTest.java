package fatec.fusex.nexus.setorGuias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guiaAssinatura.PdfsDeTeste;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T21-BE1 (SCRUM-191): POST /setor-guias/extrair-numero-guia com preGuiaId, sem transacao de teste envolvendo a
 * requisicao (como na aplicacao real, com open-in-view desligado). O titular da pre-guia e carregado sob demanda e
 * precisa ser lido dentro da transacao do servico. O @AfterEach apaga tudo o que o teste criou.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtrairNumeroGuiaIntegracaoTest {

    private static final String ROTA = "/setor-guias/extrair-numero-guia";
    private static final String LOGIN_SETOR = "setor.leitura.integracao";
    private static final String LOGIN_TITULAR = "72100000001";
    private static final String PREC_CP_TITULAR = "72100000022";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;
    @Autowired
    private OcsRepository ocsRepository;
    @Autowired
    private PreGuiaRepository preGuiaRepository;

    private Long ocsId;
    private Long titularId;
    private Long preGuiaTitularId;

    @BeforeEach
    void dados() {
        usuario(LOGIN_SETOR, PerfilUsuario.SETOR_GUIAS);

        Beneficiario titular = new Beneficiario();
        titular.setUsuario(usuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO));
        titular.setNome("Titular Integracao");
        titular.setPrecCp(PREC_CP_TITULAR);
        titular.setDataNascimento(LocalDate.of(1968, 3, 15));
        titular.setTelefone("12987654321");
        titular = beneficiarioRepository.saveAndFlush(titular);
        titularId = titular.getUsuarioId();

        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio Integracao");
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CTR-INTEGRACAO-1");
        ocs = ocsRepository.saveAndFlush(ocs);
        ocsId = ocs.getIdOcs();

        preGuiaTitularId = preGuia(titular, ocs).getIdPreGuia();
    }

    @AfterEach
    void limpar() {
        preGuiaRepository.findById(preGuiaTitularId).ifPresent(preGuiaRepository::delete);
        ocsRepository.deleteById(ocsId);
        beneficiarioRepository.findById(titularId).ifPresent(beneficiarioRepository::delete);
        usuarioRepository.findByLogin(LOGIN_TITULAR).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(LOGIN_SETOR).ifPresent(usuarioRepository::delete);
    }

    @Test
    void preGuiaDoTitular_comOPrecCpDele_confere() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Nr: 202610678",
                "Titular: (PACIENTE)", "Prec CP: " + PREC_CP_TITULAR);

        ler(preGuiaTitularId, conteudo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610678"))
                .andExpect(jsonPath("$.precCpLido").value(PREC_CP_TITULAR))
                .andExpect(jsonPath("$.precCpConfere").value(true))
                .andExpect(jsonPath("$.pacienteNome").value("Titular Integracao"))
                .andExpect(jsonPath("$.precCpPaciente").value(PREC_CP_TITULAR))
                .andExpect(jsonPath("$.pacienteDependente").value(false));
    }

    @Test
    void preGuiaDoTitular_comOPrecCpDeOutraPessoa_naoConfere() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Nr: 202610678",
                "Titular: (PACIENTE)", "Prec CP: 20847163908");

        ler(preGuiaTitularId, conteudo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precCpLido").value("20847163908"))
                .andExpect(jsonPath("$.precCpConfere").value(false));
    }

    @Test
    void preGuiaInexistente_retorna404() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Nr: 202610678");

        ler(Long.MAX_VALUE, conteudo).andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions ler(Long preGuiaId, byte[] conteudo) throws Exception {
        return mockMvc.perform(multipart(HttpMethod.POST, ROTA)
                .file(new MockMultipartFile("pdf", "guia.pdf", "application/pdf", conteudo))
                .param("preGuiaId", String.valueOf(preGuiaId))
                .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR));
    }

    private PreGuia preGuia(Beneficiario titular, Ocs ocs) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.PENDENTE);
        preGuia.setBeneficiario(titular);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento-integracao.pdf");
        preGuia.setDataCriacao(LocalDateTime.of(2026, 10, 1, 9, 0));
        return preGuiaRepository.saveAndFlush(preGuia);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
