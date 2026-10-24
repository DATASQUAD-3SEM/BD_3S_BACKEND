package fatec.fusex.nexus.preGuia;

import java.util.List;

import fatec.fusex.nexus.common.exception.AccessDeniedException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.preGuia.dto.CorrecaoPreGuiaResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsService;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;

CorrecaoPreGuiaResponse, ConflitoException e AccessDeniedException

@Service
public class PreGuiaService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsService ocsService;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsService ocsService,
                          ArmazenamentoArquivo armazenamentoArquivo) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsService = ocsService;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    public List<PreGuia> listarDoBeneficiario(Long beneficiarioId) {
        if (!beneficiarioRepository.existsById(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Beneficiario nao encontrado: id " + beneficiarioId);
        }
        return preGuiaRepository.findByBeneficiarioUsuarioId(beneficiarioId);
    }

    public PreGuia buscarDoBeneficiario(Long preGuiaId, Long beneficiarioId) {
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));

        if (!preGuia.getBeneficiario().getUsuarioId().equals(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId);
        }
        return preGuia;
    }

    @Transactional
    public PreGuiaResponse criar(String cpf, String precCp, Long ocsId, MultipartFile arquivo) {
        validarArquivoObrigatorio(arquivo);

        Ocs ocs = ocsService.buscarPorId(ocsId);
        Beneficiario beneficiario = identificarBeneficiario(cpf, precCp);

        String encaminhamentoUrl = armazenamentoArquivo.salvar(arquivo, "encaminhamentos");
        try {
            PreGuia preGuia = new PreGuia();
            preGuia.setBeneficiario(beneficiario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);

            preGuiaRepository.save(preGuia);

            return PreGuiaResponse.de(preGuia);
        } catch (RuntimeException e) {
            try {
                armazenamentoArquivo.remover(encaminhamentoUrl);
            } catch (RuntimeException limpezaFalhou) {
                // nao deixa a falha da limpeza esconder a causa original
            }
            throw e;
        }
    }

    private Beneficiario identificarBeneficiario(String cpf, String precCp) {
        Beneficiario beneficiario = beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado: cpf " + cpf));

        if (!beneficiario.getPrecCp().equals(precCp)) {
            throw new RegraDeNegocioException("CPF e Prec-CP nao correspondem ao mesmo beneficiario");
        }
        return beneficiario;
    }

    private void validarArquivoObrigatorio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("E necessario anexar o encaminhamento medico para criar a pre-guia");
        }
    }

    public fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse buscarDadosCorrecao(Long preGuiaId, String cpfLogado) {
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pré-guia não encontrada: id " + preGuiaId));

        if (!preGuia.getBeneficiario().getCpf().equals(cpfLogado)) {
            throw new RecursoNaoEncontradoException("Pré-guia não encontrada: id " + preGuiaId);
        }

        if (preGuia.getOrigem() == OrigemPreGuia.MEDICO) {
            throw new org.springframework.security.access.AccessDeniedException("Pré-guia criada por médico não pode ser alterada pelo beneficiário");
        }

        if (preGuia.getStatus() != StatusPreGuia.ATENCAO) {
            throw new fatec.fusex.nexus.common.exception.ConflitoException("A pré-guia não está em Atenção");
        }

        return new fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse(
                preGuia.getNumero(),
                preGuia.getStatus().name(),
                new fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse.BeneficiarioResumo(
                        preGuia.getBeneficiario().getNome(), preGuia.getBeneficiario().getPrecCp()),
                preGuia.getDataEmissao(),
                new fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse.ObservacaoResumo(
                        preGuia.getObservacao(), preGuia.getDataRegistroObservacao()),
                new fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse.EncaminhamentoResumo(
                        preGuia.getEncaminhamentoNome(), preGuia.getEncaminhamentoTipo(), preGuia.getDataEnvioEncaminhamento()),
                new fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse.OcsResumo(
                        preGuia.getOcs().getId(), preGuia.getOcs().getNome())
        );
    }
}