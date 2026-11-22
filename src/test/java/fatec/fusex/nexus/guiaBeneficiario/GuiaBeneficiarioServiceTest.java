package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuiaBeneficiarioServiceTest {

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
    void deveFazerRollbackQuandoFalharAoSalvarSessoes() {

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        when(usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO)).thenReturn(usuario);


        Guia guia = new Guia();
        guia.setIdGuia(10L);
        guia.setSituacaoExecucao(SituacaoExecucao.NAO_ENVIADA);
        guia.setDataValidadeAte(LocalDate.now().plusDays(10));


        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuarioId(1L);
        
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        
        guia.setPreGuia(preGuia); 

        when(guiaRepository.findByIdComLock(10L)).thenReturn(Optional.of(guia));

        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setQuantidadeAutorizada(1);
        when(procedimentoRepository.findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(10L)))
                .thenReturn(List.of(procedimento));


        doThrow(new RuntimeException("Erro simulado no banco ao salvar sessões"))
                .when(sessaoRepository).saveAll(any());


        Assertions.assertThrows(RuntimeException.class, () -> {
            service.enviar(10L);
        });


        verify(procedimentoRepository, times(1)).findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(10L));
        verify(sessaoRepository, times(1)).saveAll(any());
    }
}