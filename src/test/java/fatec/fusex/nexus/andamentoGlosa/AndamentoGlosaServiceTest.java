package fatec.fusex.nexus.andamentoGlosa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse;
import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse.ItemAndamento;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcsRepository;
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
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** T26-BE4: andamento da glosa por Guia, a partir das rodadas da OCS e dos eventos da fatura. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AndamentoGlosaServiceTest {

    // Envio numa quarta as 09:00: prazo na segunda seguinte as 23:59:59.
    private static final LocalDateTime ENVIO_1 = LocalDateTime.of(2026, 11, 11, 9, 0);
    private static final LocalDateTime DENTRO_DO_PRAZO = LocalDateTime.of(2026, 11, 12, 9, 0);
    private static final LocalDateTime APOS_O_PRAZO = LocalDateTime.of(2026, 11, 17, 0, 0);

    @Autowired private AndamentoGlosaService service;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaProcedimentoRepository procedimentoRepository;
    @Autowired private EspelhoRepository espelhoRepository;
    @Autowired private EspelhoItemRepository espelhoItemRepository;
    @Autowired private EspelhoItemRodadaOcsRepository rodadaRepository;
    @Autowired private FaturaEventoRepository eventoRepository;

    private Ocs ocs;
    private Usuario setor;
    private Beneficiario joao;
    private Espelho fatura;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Andamento");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-ANDAMENTO-1");
        ocs = ocsRepository.save(nova);
        setor = novoUsuario("rastreabilidade.andamento", PerfilUsuario.RASTREABILIDADE);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(novoUsuario("77766655544", PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Joao Andamento");
        beneficiario.setPrecCp("77766655544");
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);

        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 2, 9, 0));
        espelho.setSituacaoFatura(SituacaoFatura.EM_AUDITORIA);
        espelho.setNumeroFatura("010/2026");
        espelho.setProtocoladaPor(setor);
        fatura = espelhoRepository.save(espelho);
    }

    @Test
    void guiaAceitaNaPrimeiraRodada() {
        EspelhoItem item = item("80.00");
        FaturaEvento envio = evento(TipoFaturaEvento.ENVIADA_OCS, ENVIO_1, "Ana Silva");
        FaturaEvento ok = evento(TipoFaturaEvento.CIENTE_OCS, ENVIO_1.plusDays(1), "Bruno Lima");
        rodada(item, envio, "80.00", "OK", null, ok);

        AndamentoGlosaResponse andamento = unico(DENTRO_DO_PRAZO);

        assertThat(andamento.situacaoFinal()).isEqualTo(AndamentoGlosaService.ACEITA_PELA_OCS);
        assertThat(andamento.seloFinal()).isEqualTo("Aceita pela OCS");
        assertThat(andamento.itens()).extracting(ItemAndamento::texto).containsExactly(
                "Lisura concluída e glosa enviada à OCS pela rastreabilidade — responsável: Ana Silva. "
                        + "Prazo de 3 dias úteis, até 16/11/2026.",
                "OCS de acordo (Ok) — responsável: Bruno Lima.");
    }

    @Test
    void guiaContestadaEAceitaNaSegundaRodada() {
        EspelhoItem item = item("90.00");
        FaturaEvento envio1 = evento(TipoFaturaEvento.ENVIADA_OCS, ENVIO_1, "Ana Silva");
        FaturaEvento contestada = evento(TipoFaturaEvento.CONTESTADA_OCS, ENVIO_1.plusDays(1), "Bruno Lima");
        FaturaEvento envio2 = evento(TipoFaturaEvento.ENVIADA_OCS, ENVIO_1.plusDays(2), "Carla Souza");
        FaturaEvento ok = evento(TipoFaturaEvento.CIENTE_OCS, ENVIO_1.plusDays(5), "Bruno Lima");
        rodada(item, envio1, "80.00", "CONTESTADA", "Valor diferente do contrato vigente", contestada);
        rodada(item, envio2, "90.00", "OK", null, ok);

        AndamentoGlosaResponse andamento = unico(DENTRO_DO_PRAZO.plusDays(10));

        assertThat(andamento.situacaoFinal()).isEqualTo(AndamentoGlosaService.ACEITA_PELA_OCS);
        assertThat(andamento.itens()).extracting(ItemAndamento::texto).containsExactly(
                "Lisura concluída e glosa enviada à OCS pela rastreabilidade — responsável: Ana Silva. "
                        + "Prazo de 3 dias úteis, até 16/11/2026.",
                "Contestada pela OCS — responsável: Bruno Lima. Motivo: \"Valor diferente do contrato vigente\"",
                "Revista pela rastreabilidade e reenviada à OCS — TC - Abdome superior: contrato alterado de "
                        + "R$ 80,00 para R$ 90,00; responsável: Carla Souza. Prazo até 18/11/2026.",
                "OCS de acordo (Ok) — responsável: Bruno Lima.");
    }

    @Test
    void contestacaoAcatadaQuandoARevisaoRetiraAGlosa() {
        EspelhoItem item = item("100.00");
        FaturaEvento envio = evento(TipoFaturaEvento.ENVIADA_OCS, ENVIO_1, "Ana Silva");
        FaturaEvento contestada = evento(TipoFaturaEvento.CONTESTADA_OCS, ENVIO_1.plusDays(1), "Bruno Lima");
        evento(TipoFaturaEvento.ENVIADA_AUDITORIA, ENVIO_1.plusDays(2), "Carla Souza");
        rodada(item, envio, "80.00", "CONTESTADA", "Valor diferente do contrato vigente", contestada);

        AndamentoGlosaResponse andamento = unico(APOS_O_PRAZO);

        assertThat(andamento.situacaoFinal()).isEqualTo(AndamentoGlosaService.CONTESTACAO_ACATADA);
        assertThat(andamento.seloFinal()).isEqualTo("Contestação acatada");
        assertThat(andamento.itens().get(andamento.itens().size() - 1).texto()).isEqualTo(
                "Revista pela rastreabilidade — TC - Abdome superior: glosa retirada (contrato igual ao informado, "
                        + "R$ 100,00). Responsável: Carla Souza.");
    }

    @Test
    void rodadaSemRespostaComPrazoVencido() {
        EspelhoItem item = item("80.00");
        FaturaEvento envio = evento(TipoFaturaEvento.ENVIADA_OCS, ENVIO_1, "Ana Silva");
        rodada(item, envio, "80.00", null, null, null);

        AndamentoGlosaResponse vencida = unico(APOS_O_PRAZO);
        assertThat(vencida.situacaoFinal()).isEqualTo(AndamentoGlosaService.PRAZO_VENCIDO);
        assertThat(vencida.itens()).extracting(ItemAndamento::texto).last().isEqualTo(
                "Prazo vencido sem resposta da OCS. A glosa segue como lisurada pela rastreabilidade.");

        AndamentoGlosaResponse noPrazo = unico(DENTRO_DO_PRAZO);
        assertThat(noPrazo.situacaoFinal()).isEqualTo(AndamentoGlosaService.EM_ANDAMENTO);
        assertThat(noPrazo.itens()).hasSize(1);
    }

    @Test
    void guiaQueNuncaFoiAOcsNaoTemAndamento() {
        EspelhoItem item = item("100.00");

        assertThat(service.daFatura(fatura.getIdEspelho(), DENTRO_DO_PRAZO)).isEmpty();
        Long guiaId = item.getGuiaProcedimento().getGuia().getIdGuia();
        assertThat(service.daGuia(fatura.getIdEspelho(), guiaId).itens()).isEmpty();
    }

    // ---------- fixtures ----------

    private AndamentoGlosaResponse unico(LocalDateTime agora) {
        List<AndamentoGlosaResponse> andamentos = service.daFatura(fatura.getIdEspelho(), agora);
        assertThat(andamentos).hasSize(1);
        return andamentos.get(0);
    }

    /** Linha com informado de R$ 100,00 por sessao e o contrato atual informado. */
    private EspelhoItem item(String contratoAtual) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(fatura);
        item.setGuiaProcedimento(novoProcedimento(novaGuia()));
        item.setQuantidadeSessoes(1);
        item.setValorSessao(new BigDecimal("100.00"));
        item.setValorContratoSessao(new BigDecimal(contratoAtual));
        item.setOrigemContrato(OrigemContrato.MANUAL);
        return espelhoItemRepository.save(item);
    }

    private void rodada(EspelhoItem item, FaturaEvento envio, String contrato, String decisao, String motivo,
                        FaturaEvento decisaoEvento) {
        EspelhoItemRodadaOcs rodada = new EspelhoItemRodadaOcs();
        rodada.setEspelhoItem(item);
        rodada.setEnvioEvento(envio);
        rodada.setValorContratoSessao(new BigDecimal(contrato));
        rodada.setOrigemContrato(OrigemContrato.MANUAL);
        rodada.setDecisaoOcs(decisao);
        rodada.setMotivoContestacaoOcs(motivo);
        rodada.setDecisaoEvento(decisaoEvento);
        rodadaRepository.save(rodada);
    }

    private FaturaEvento evento(TipoFaturaEvento tipo, LocalDateTime data, String responsavel) {
        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(fatura);
        evento.setTipo(tipo);
        evento.setUsuario(setor);
        evento.setResponsavel(responsavel);
        evento.setDataEvento(data);
        return eventoRepository.save(evento);
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
        guia.setNumero(String.format("%09d", 800000 + ++sequencia));
        guia.setArquivoUrl("guia.pdf");
        guia.setDataInsercao(LocalDateTime.of(2026, 9, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia.setSituacaoExecucao(SituacaoExecucao.REALIZADA);
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento novoProcedimento(Guia guia) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp("41001101");
        procedimento.setDescricaoProcedimento("TC - Abdome superior");
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
