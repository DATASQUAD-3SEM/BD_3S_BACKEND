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
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
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
    private final UsuarioLogado usuarioLogado;

    public ConcluirGuiaService(ConcluirGuiaRepository repository,
                               UsuarioLogado usuarioLogado) {
        this.repository = repository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404) -> status (409) -> Guia ja criada (409) -> rascunho (409)
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
        if (repository.numeroUsadoEmGuia(rascunho.getNumero())) {
            throw new ConflitoException("numeroGuia", "Numero de Guia ja usado em outra Guia");
        }

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(rascunho.getNumero());
        guia.setArquivoUrl(rascunho.getArquivoUrl());
        guia.setDataInsercao(LocalDateTime.now()); // a validade conta a partir do "Concluir"
        guia.setUsuario(setor);

        try {
            repository.saveAndFlush(guia);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE do banco (pre_guia_id ou numero) barrou uma conclusao concorrente
            throw new ConflitoException("A Guia desta pre-guia ja foi criada");
        }

        return new GuiaConcluidaResponse(guia.getIdGuia(), guia.getNumero(), guia.validadeAte(), ASSINATURAS_PENDENTES);
    }
}
