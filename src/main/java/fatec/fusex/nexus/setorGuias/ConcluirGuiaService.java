package fatec.fusex.nexus.setorGuias;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimento;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.setorGuias.dto.GuiaConcluidaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T4-BE6: POST /setor-guias/pre-guias/{id}/guia/concluir, sem corpo.
 * Cria a Guia com o PDF do rascunho (T4-BE5) e a deixa pendente das assinaturas MEDICO e CHEFE.
 * O status da pre-guia continua Em analise ate as duas assinaturas (T5-BE3 conclui).
 */
@Service
public class ConcluirGuiaService {

    // Recem-criada, a Guia ainda nao tem nenhuma assinatura
    private static final List<String> ASSINATURAS_PENDENTES =
            List.of(AssinaturaGuiaPapel.MEDICO.name(), AssinaturaGuiaPapel.CHEFE.name());

    private final ConcluirGuiaRepository repository;
    private final GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository;
    private final GuiaProcedimentoRepository guiaProcedimentoRepository;
    private final UsuarioLogado usuarioLogado;

    public ConcluirGuiaService(ConcluirGuiaRepository repository,
                               GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository,
                               GuiaProcedimentoRepository guiaProcedimentoRepository,
                               UsuarioLogado usuarioLogado) {
        this.repository = repository;
        this.guiaRascunhoProcedimentoRepository = guiaRascunhoProcedimentoRepository;
        this.guiaProcedimentoRepository = guiaProcedimentoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404) -> status (409) -> Guia ja criada (409) -> rascunho (409)
    //        -> validade do rascunho (409) -> numero repetido (409) -> exames (409)
    // T22-BE2: os exames do rascunho viram GuiaProcedimento na mesma transacao: tudo ou nada.
    @Transactional
    public GuiaConcluidaResponse concluir(Long id) {
        Usuario setor = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

        // Trava a pre-guia: se dois cliques chegarem juntos, so um cria a Guia
        PreGuia preGuia = repository.buscarPreGuiaParaConcluir(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        if (preGuia.getStatus() != PreGuiaStatus.EM_ANALISE) {
            throw new ConflitoException("A pre-guia nao esta Em analise: ela ja foi alterada");
        }
        if (repository.temGuia(id)) {
            throw new ConflitoException("A Guia desta pre-guia ja foi criada");
        }
        GuiaRascunho rascunho = repository.buscarRascunho(id)
                .orElseThrow(() -> new ConflitoException("Guia nao anexada"));
        // T16-BE3: rascunho gravado antes da T16-BE2 nao tem validade conferida: o setor anexa a Guia de novo
        if (rascunho.getValidadeAte() == null) {
            throw new ConflitoException("Guia sem validade conferida");
        }
        if (repository.numeroUsadoEmGuia(rascunho.getNumero())) {
            throw new ConflitoException("numeroGuia", "Numero de Guia ja usado em outra Guia");
        }
        List<GuiaRascunhoProcedimento> exames = guiaRascunhoProcedimentoRepository.listarDaPreGuia(id);
        if (exames.isEmpty()) {
            throw new ConflitoException("Exames da Guia não informados");
        }

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(rascunho.getNumero());
        guia.setArquivoUrl(rascunho.getArquivoUrl());
        guia.setDataInsercao(LocalDateTime.now());
        // T16-BE3: a validade e a data impressa em NAO RECEBER APOS (conferida no rascunho), nao a insercao + 2 meses
        guia.setDataValidadeAte(rascunho.getValidadeAte());
        guia.setUsuario(setor);

        try {
            repository.saveAndFlush(guia);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE do banco (pre_guia_id ou numero) barrou uma conclusao concorrente
            throw new ConflitoException("A Guia desta pre-guia ja foi criada");
        }

        List<GuiaProcedimento> procedimentos = exames.stream()
                .map(e -> {
                    GuiaProcedimento procedimento = new GuiaProcedimento();
                    procedimento.setGuia(guia);
                    procedimento.setCodDgp(e.getCodDgp());
                    procedimento.setDescricaoProcedimento(e.getDescricaoProcedimento());
                    procedimento.setQuantidadeAutorizada(e.getQuantidadeAutorizada());
                    return procedimento;
                })
                .toList();
        guiaProcedimentoRepository.saveAllAndFlush(procedimentos);

        return new GuiaConcluidaResponse(guia.getIdGuia(), guia.getNumero(), guia.getDataValidadeAte(), ASSINATURAS_PENDENTES);
    }
}
