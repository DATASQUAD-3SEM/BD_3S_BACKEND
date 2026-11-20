package fatec.fusex.nexus.setorFatura;

import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.CIENTE_OCS;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.CONTESTADA_OCS;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.ENVIADA_AUDITORIA;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.ENVIADA_LISURA;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.ENVIADA_OCS;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.PROTOCOLADA;
import static fatec.fusex.nexus.setorFatura.TipoFaturaEvento.VISUALIZADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
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
@ActiveProfiles("test")
@Transactional
class FaturaTransicaoServiceTest {

    private static final LocalDateTime SEXTA = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final String RESPONSAVEL = "Ana Silva";

    @Autowired
    private FaturaTransicaoService service;

    @Autowired
    private EspelhoRepository espelhoRepository;

    @Autowired
    private FaturaEventoRepository eventoRepository;

    @Autowired
    private OcsRepository ocsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Ocs ocs;
    private Usuario setor;
    private Usuario ocsUsuario;
    private Long idEspelho;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Transicao");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-TRANS-1");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario("setor.transicao", PerfilUsuario.SETOR_FATURA);
        ocsUsuario = novoUsuario("ocs.transicao", PerfilUsuario.OCS);

        idEspelho = novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.RECEBIDO).getIdEspelho();
    }

    @Test
    void fluxoSemGlosaVaiDeRecebidaAteAuditada() {
        registrar(PROTOCOLADA, SEXTA);
        registrar(ENVIADA_LISURA, SEXTA.plusHours(1));
        registrar(ENVIADA_AUDITORIA, SEXTA.plusHours(2));
        registrar(VISUALIZADA, SEXTA.plusHours(3));

        assertThat(situacao()).isEqualTo(SituacaoFatura.AUDITADA);
        assertThat(eventos()).hasSize(4);
    }

    @Test
    void contestacaoVoltaParaLisuraEPermiteNovaRodada() {
        registrar(PROTOCOLADA, SEXTA);
        registrar(ENVIADA_LISURA, SEXTA.plusHours(1));
        registrar(ENVIADA_OCS, SEXTA.plusHours(2));
        registrarComoOcs(CONTESTADA_OCS, SEXTA.plusHours(3));
        assertThat(situacao()).isEqualTo(SituacaoFatura.EM_LISURA);

        registrar(ENVIADA_OCS, SEXTA.plusHours(4));
        registrarComoOcs(CIENTE_OCS, SEXTA.plusHours(5));
        registrar(ENVIADA_AUDITORIA, SEXTA.plusHours(6));

        assertThat(situacao()).isEqualTo(SituacaoFatura.EM_AUDITORIA);
    }

    @Test
    void cienteNaRodadaLiberaEnvioParaAuditoriaSemEsperarPrazo() {
        registrar(PROTOCOLADA, SEXTA);
        registrar(ENVIADA_LISURA, SEXTA.plusHours(1));
        registrar(ENVIADA_OCS, SEXTA.plusHours(2));
        registrarComoOcs(CIENTE_OCS, SEXTA.plusHours(3));
        registrar(ENVIADA_AUDITORIA, SEXTA.plusHours(4));

        assertThat(situacao()).isEqualTo(SituacaoFatura.EM_AUDITORIA);
    }

    @Test
    void envioParaAuditoriaSemCienteNemPrazoVencidoEhRecusado() {
        registrar(PROTOCOLADA, SEXTA);
        registrar(ENVIADA_LISURA, SEXTA.plusHours(1));
        registrar(ENVIADA_OCS, SEXTA.plusHours(2));
        LocalDateTime terca = LocalDateTime.of(2026, 11, 24, 13, 0);

        assertThatThrownBy(() -> service.registrar(idEspelho, ENVIADA_AUDITORIA, setor, RESPONSAVEL, terca))
                .isInstanceOf(ConflitoException.class);
        assertThat(situacao()).isEqualTo(SituacaoFatura.AGUARDANDO_OCS);
    }

    @Test
    void envioParaAuditoriaLiberadoAposTresDiasUteis() {
        registrar(PROTOCOLADA, SEXTA);
        registrar(ENVIADA_LISURA, SEXTA.plusHours(1));
        registrar(ENVIADA_OCS, SEXTA.plusHours(2));
        LocalDateTime quarta = LocalDateTime.of(2026, 11, 25, 14, 0);

        registrar(ENVIADA_AUDITORIA, quarta);

        assertThat(situacao()).isEqualTo(SituacaoFatura.EM_AUDITORIA);
    }

    @Test
    void saltoDeSituacaoEhRecusadoSemGravarEvento() {
        assertThatThrownBy(() -> registrar(ENVIADA_LISURA, SEXTA))
                .isInstanceOf(ConflitoException.class);

        assertThat(situacao()).isEqualTo(SituacaoFatura.RECEBIDO);
        assertThat(eventos()).isEmpty();
    }

    @Test
    void responsavelInvalidoEhRecusadoSemMudarNada() {
        assertThatThrownBy(() -> service.registrar(idEspelho, PROTOCOLADA, setor, "Al", SEXTA))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.registrar(idEspelho, PROTOCOLADA, setor, "   ", SEXTA))
                .isInstanceOf(DadosInvalidosException.class);

        assertThat(situacao()).isEqualTo(SituacaoFatura.RECEBIDO);
        assertThat(eventos()).isEmpty();
    }

    @Test
    void espelhoAindaEmConstrucaoNaoRecebeEvento() {
        Long emConstrucao = novoEspelho(StatusEspelho.EM_CONSTRUCAO, null).getIdEspelho();

        assertThatThrownBy(() -> service.registrar(emConstrucao, PROTOCOLADA, setor, RESPONSAVEL, SEXTA))
                .isInstanceOf(ConflitoException.class);
    }

    private void registrar(TipoFaturaEvento tipo, LocalDateTime quando) {
        service.registrar(idEspelho, tipo, setor, RESPONSAVEL, quando);
    }

    private void registrarComoOcs(TipoFaturaEvento tipo, LocalDateTime quando) {
        service.registrar(idEspelho, tipo, ocsUsuario, "Contato da OCS", quando);
    }

    private SituacaoFatura situacao() {
        return espelhoRepository.findById(idEspelho).orElseThrow().getSituacaoFatura();
    }

    private List<FaturaEvento> eventos() {
        return eventoRepository.findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(idEspelho);
    }

    private Espelho novoEspelho(StatusEspelho status, SituacaoFatura situacao) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(status);
        espelho.setCriadoPor(setor);
        espelho.setDataEnvio(SEXTA);
        espelho.setSituacaoFatura(situacao);
        return espelhoRepository.save(espelho);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}