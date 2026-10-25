package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaMedicoRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaMedicoResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da criacao da pre-guia pelo medico (T2-BE4): validacoes de beneficiario, OCS e procedimentos do catalogo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicoPreGuiaServiceTest {

    private static final String LOGIN = "11144477735";

    @Mock private MedicoFusexService medicoService;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private OcsRepository ocsRepository;
    @Mock private ProcedimentoExameRepository procedimentoRepository;
    @Mock private PreGuiaRepository preGuiaRepository;
    @Mock private PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    @Mock private PreGuiaHistoricoRepository historicoRepository;

    @InjectMocks
    private MedicoPreGuiaService service;

    private MedicoFusex medico;
    private Beneficiario beneficiario;
    private Ocs ocs;

    @BeforeEach
    void dados() {
        Usuario usuarioMedico = new Usuario();
        usuarioMedico.setLogin(LOGIN);
        usuarioMedico.setAtivo(true);
        medico = new MedicoFusex();
        medico.setUsuario(usuarioMedico);

        Usuario usuarioBenef = new Usuario();
        usuarioBenef.setAtivo(true);
        beneficiario = new Beneficiario();
        beneficiario.setUsuarioId(5L);
        beneficiario.setUsuario(usuarioBenef);

        ocs = new Ocs();
        ocs.setIdOcs(10L);
        ocs.setAtivo(true);

        when(medicoService.validarAcessoMedico(LOGIN)).thenReturn(medico);
        when(beneficiarioRepository.findById(5L)).thenReturn(Optional.of(beneficiario));
        when(ocsRepository.findById(10L)).thenReturn(Optional.of(ocs));
        when(preGuiaRepository.save(any(PreGuia.class))).thenAnswer(inv -> {
            PreGuia p = inv.getArgument(0);
            p.setIdPreGuia(7L);
            return p;
        });
    }

    private ProcedimentoExame procedimento(long id, String codigoTuss) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setIdProcedimentoExame(id);
        p.setCodigoTuss(codigoTuss);
        return p;
    }

    private CriarPreGuiaMedicoRequest request(List<String> procedimentos) {
        return new CriarPreGuiaMedicoRequest(5L, 10L, procedimentos);
    }

    private void assertCampo422(CriarPreGuiaMedicoRequest req, String campo) {
        assertThatThrownBy(() -> service.criar(LOGIN, req))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo(campo);
        verify(preGuiaRepository, never()).save(any());
    }

    @Test
    void criar_sucesso_gravaPreGuiaPendenteSemEncaminhamentoComMedicoProcedimentosEHistorico() {
        when(procedimentoRepository.findByCodigoTussIn(anyCollection()))
                .thenReturn(List.of(procedimento(1L, "40304361"), procedimento(2L, "40302040")));

        PreGuiaMedicoResponse resposta = service.criar(LOGIN, request(List.of("40304361", " 40302040 ")));

        ArgumentCaptor<PreGuia> preGuia = ArgumentCaptor.forClass(PreGuia.class);
        verify(preGuiaRepository).save(preGuia.capture());
        assertThat(preGuia.getValue().getMedicoFusex()).isSameAs(medico);
        assertThat(preGuia.getValue().getBeneficiario()).isSameAs(beneficiario);
        assertThat(preGuia.getValue().getOcs()).isSameAs(ocs);
        assertThat(preGuia.getValue().getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
        assertThat(preGuia.getValue().getEncaminhamentoUrl()).isNull();

        verify(preGuiaProcedimentoRepository, times(2)).save(any(PreGuiaProcedimento.class));

        ArgumentCaptor<PreGuiaHistorico> historico = ArgumentCaptor.forClass(PreGuiaHistorico.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
        assertThat(historico.getValue().getUsuario()).isSameAs(medico.getUsuario());

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.numero()).isEqualTo("0007");
        assertThat(resposta.status()).isEqualTo("PENDENTE");
        assertThat(resposta.dataCriacao()).isNotNull();
    }

    @Test
    void criar_procedimentoDeOutraOcs_eAceito() {
        // A busca e no catalogo inteiro: o servico nem consulta a relacao OCS x procedimento
        when(procedimentoRepository.findByCodigoTussIn(anyCollection()))
                .thenReturn(List.of(procedimento(3L, "40301630")));

        PreGuiaMedicoResponse resposta = service.criar(LOGIN, request(List.of("40301630")));

        assertThat(resposta.status()).isEqualTo("PENDENTE");
        verify(preGuiaProcedimentoRepository).save(any(PreGuiaProcedimento.class));
    }

    @Test
    void criar_listaVazia_lanca422EmProcedimentos() {
        assertCampo422(request(List.of()), "procedimentos");
    }

    @Test
    void criar_semListaDeProcedimentos_lanca422EmProcedimentos() {
        assertCampo422(request(null), "procedimentos");
    }

    @Test
    void criar_procedimentoRepetido_lanca422EmProcedimentos() {
        // " 40304361" e "40304361" sao o mesmo codigo depois do trim
        assertCampo422(request(List.of("40304361", " 40304361")), "procedimentos");
        verify(procedimentoRepository, never()).findByCodigoTussIn(anyCollection());
    }

    @Test
    void criar_codigoEmBranco_lanca422EmProcedimentos() {
        assertCampo422(request(Arrays.asList("40304361", null)), "procedimentos");
    }

    @Test
    void criar_codigoInexistente_lanca422EmProcedimentosCitandoOCodigo() {
        when(procedimentoRepository.findByCodigoTussIn(anyCollection()))
                .thenReturn(List.of(procedimento(1L, "40304361")));

        assertThatThrownBy(() -> service.criar(LOGIN, request(List.of("40304361", "99999999"))))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("99999999");
        verify(preGuiaRepository, never()).save(any());
    }

    @Test
    void criar_beneficiarioInativo_lanca422EmBeneficiarioId() {
        beneficiario.getUsuario().setAtivo(false);

        assertCampo422(request(List.of("40304361")), "beneficiarioId");
    }

    @Test
    void criar_beneficiarioInexistente_lanca422EmBeneficiarioId() {
        assertCampo422(new CriarPreGuiaMedicoRequest(99L, 10L, List.of("40304361")), "beneficiarioId");
        assertCampo422(new CriarPreGuiaMedicoRequest(null, 10L, List.of("40304361")), "beneficiarioId");
    }

    @Test
    void criar_ocsInativa_lanca422EmOcsId() {
        ocs.setAtivo(false);

        assertCampo422(request(List.of("40304361")), "ocsId");
    }

    @Test
    void criar_ocsInexistente_lanca422EmOcsId() {
        assertCampo422(new CriarPreGuiaMedicoRequest(5L, 99L, List.of("40304361")), "ocsId");
        assertCampo422(new CriarPreGuiaMedicoRequest(5L, null, List.of("40304361")), "ocsId");
    }

    @Test
    void criar_perfilErrado_lanca403SemValidarNemGravar() {
        when(medicoService.validarAcessoMedico("52998224725"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        assertThatThrownBy(() -> service.criar("52998224725", request(List.of())))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(beneficiarioRepository, ocsRepository, procedimentoRepository, preGuiaRepository);
    }
}
