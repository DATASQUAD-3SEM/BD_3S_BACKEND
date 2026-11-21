package fatec.fusex.nexus.guiaBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** T30-BE5 (SCRUM-262): o responsavel legal assina a sessao do menor com a propria assinatura. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuiaAssinaturaResponsavelTest {

    private static final long ID_GUIA = 77L;
    private static final long ID_SESSAO = 5L;

    @Autowired
    private GuiaBeneficiarioService service;

    @MockitoBean
    private UsuarioLogado usuarioLogado;

    @MockitoBean
    private GuiaRepository guiaRepository;

    @MockitoBean
    private GuiaProcedimentoRepository procedimentoRepository;

    @MockitoSpyBean
    private SessaoRepository sessaoRepository;

    @Test
    void responsavelAssinaSessaoDoMenorComSuaAssinatura() {
        Usuario responsavel = responsavel(2L, cadastrada(2L));
        Guia guia = guiaDoMenor(responsavel);
        Sessao sessao = sessaoSolicitada(guia);
        preparar(responsavel, guia, sessao);

        service.assinar(ID_GUIA, ID_SESSAO);

        assertThat(sessao.getStatus()).isEqualTo(SituacaoSessao.ASSINADA);
        assertThat(sessao.getAssinadoPor()).isSameAs(responsavel);
        assertThat(sessao.getAssinatura()).isSameAs(responsavel.getAssinatura());
        assertThat(guia.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.REALIZADA);
    }

    @Test
    void responsavelSemAssinaturaCadastradaRecebe422() {
        Usuario responsavel = responsavel(2L, null);
        Guia guia = guiaDoMenor(responsavel);
        Sessao sessao = sessaoSolicitada(guia);
        preparar(responsavel, guia, sessao);

        assertThatThrownBy(() -> service.assinar(ID_GUIA, ID_SESSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThat(sessao.getStatus()).isEqualTo(SituacaoSessao.SOLICITADA);
    }

    @Test
    void outroResponsavelNaoAssinaSessaoDoMenorAlheio() {
        Usuario outro = responsavel(9L, cadastrada(9L));
        Usuario responsavelDoMenor = responsavel(2L, cadastrada(2L));
        Guia guia = guiaDoMenor(responsavelDoMenor);
        Sessao sessao = sessaoSolicitada(guia);
        preparar(outro, guia, sessao);

        assertThatThrownBy(() -> service.assinar(ID_GUIA, ID_SESSAO))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(sessao.getStatus()).isEqualTo(SituacaoSessao.SOLICITADA);
    }

    private void preparar(Usuario usuario, Guia guia, Sessao sessao) {
        when(usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO))
                .thenReturn(usuario);
        when(guiaRepository.findByIdComLock(ID_GUIA)).thenReturn(Optional.of(guia));
        doReturn(List.of(sessao)).when(sessaoRepository).buscarComLockPorGuia(ID_GUIA);
    }

    private static Usuario responsavel(long id, Assinatura assinatura) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setPerfil(PerfilUsuario.RESPONSAVEL_EXTERNO);
        usuario.setAssinatura(assinatura);
        return usuario;
    }

    private static Assinatura cadastrada(long id) {
        Assinatura assinatura = new Assinatura();
        assinatura.setIdAssinatura(id);
        assinatura.setAssinaturaUrl("assinaturas/responsavel-" + id + ".png");
        return assinatura;
    }

    private static Guia guiaDoMenor(Usuario responsavel) {
        Beneficiario titular = new Beneficiario();
        titular.setUsuarioId(1L);
        Dependente menor = new Dependente();
        menor.setNome("Sofia Ferreira");
        menor.setTipo(TipoDependente.MENOR_RESPONSAVEL);
        menor.setResponsavel(responsavel);
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(titular);
        preGuia.setDependente(menor);
        fatec.fusex.nexus.ocs.Ocs ocs = new fatec.fusex.nexus.ocs.Ocs();
        ocs.setNome("Laboratorio Responsavel");
        preGuia.setOcs(ocs);
        Guia guia = new Guia();
        guia.setIdGuia(ID_GUIA);
        guia.setPreGuia(preGuia);
        guia.setSituacaoExecucao(SituacaoExecucao.EM_EXECUCAO);
        guia.setDataValidadeAte(java.time.LocalDate.now().plusDays(30));
        return guia;
    }

    private static Sessao sessaoSolicitada(Guia guia) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setIdGuiaProcedimento(3L);
        procedimento.setQuantidadeAutorizada(2);
        procedimento.setGuia(guia);
        Sessao sessao = new Sessao();
        sessao.setIdSessao(ID_SESSAO);
        sessao.setGuiaProcedimento(procedimento);
        sessao.setNumero(1);
        sessao.setStatus(SituacaoSessao.SOLICITADA);
        return sessao;
    }
}
