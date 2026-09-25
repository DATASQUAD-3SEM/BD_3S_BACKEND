package fatec.fusex.nexus.preguia;

import java.util.List;

import org.springframework.stereotype.Service;

import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

/**
 * SCRUM-22 - Validacao do vinculo PreGuia <-> Beneficiario.
 *
 * Nesta Sprint nao existe login: o "beneficiario logado" e passado por parametro.
 * Sprint 2 (US7/US8): trocar o parametro por @AuthenticationPrincipal / JWT.
 *
 * O vinculo em si (coluna beneficiario_id) ja existe na entidade PreGuia.
 * O que este service garante e que ninguem acessa uma PreGuia que nao e sua.
 */
@Service
public class PreGuiaService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;

    public PreGuiaService(PreGuiaRepository preGuiaRepository, BeneficiarioRepository beneficiarioRepository) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
    }

    /**
     * Lista todas as pre-guias de um beneficiario.
     * Valida que o beneficiario existe antes de consultar (senao devolveria lista vazia por engano).
     */
    public List<PreGuia> listarDoBeneficiario(Long beneficiarioId) {
        if (!beneficiarioRepository.existsById(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Beneficiario nao encontrado: id " + beneficiarioId);
        }
        return preGuiaRepository.findByBeneficiarioId(beneficiarioId);
    }

    /**
     * Busca uma pre-guia por id E confirma que ela pertence ao beneficiario informado.
     * Se a pre-guia existir mas for de outro beneficiario, devolvemos 404 (mesma resposta
     * de "nao encontrada") — assim nao revelamos para o atacante que a pre-guia existe.
     */
    public PreGuia buscarDoBeneficiario(Long preGuiaId, Long beneficiarioId) {
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));

        if (!preGuia.getBeneficiario().getId().equals(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId);
        }

        return preGuia;
    }
}