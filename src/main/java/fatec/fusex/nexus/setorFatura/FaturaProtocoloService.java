package fatec.fusex.nexus.setorFatura;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.setorFatura.dto.ProtocolarFaturaRequest;
import fatec.fusex.nexus.setorFatura.dto.ProximoNumeroFaturaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class FaturaProtocoloService {

    private static final Pattern FORMATO_NUMERO = Pattern.compile("\\d{3}/\\d{4}");
    private static final String MENSAGEM_FORMATO = "Informe o nº da fatura no formato XXX/XXXX (ex.: 002/2026).";
    private static final String MENSAGEM_NUMERO_REPETIDO = "Já existe uma fatura desta OCS com este número.";
    private static final String MENSAGEM_JA_PROTOCOLADO = "Este espelho já tem nº de fatura.";

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final FaturaTransicaoService transicaoService;
    private final FaturaConsultaService consultaService;

    public FaturaProtocoloService(UsuarioLogado usuarioLogado,
                                  EspelhoRepository espelhoRepository,
                                  FaturaTransicaoService transicaoService,
                                  FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.transicaoService = transicaoService;
        this.consultaService = consultaService;
    }

    @Transactional(readOnly = true)
    public ProximoNumeroFaturaResponse sugerirProximoNumero(Long id) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);

        Espelho espelho = espelhoRepository.findById(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));

        int ano = LocalDate.now(Beneficiario.FUSO).getYear();
        int maiorNumero = espelhoRepository
                .buscarNumerosFaturaDaOcsPorAno(espelho.getOcs().getIdOcs(), "%/" + ano)
                .stream()
                .mapToInt(numero -> Integer.parseInt(numero.substring(0, 3)))
                .max()
                .orElse(0);

        return new ProximoNumeroFaturaResponse(String.format("%03d/%d", maiorNumero + 1, ano));
    }

    @Transactional
    public FaturaDetalheResponse protocolar(Long id, ProtocolarFaturaRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);
        String numero = validarNumero(request.numeroFatura());

        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (espelho.getSituacaoFatura() != SituacaoFatura.RECEBIDO) {
            throw new ConflitoException(MENSAGEM_JA_PROTOCOLADO);
        }
        if (espelhoRepository.existsByOcsIdOcsAndNumeroFatura(espelho.getOcs().getIdOcs(), numero)) {
            throw new ConflitoException("numeroFatura", MENSAGEM_NUMERO_REPETIDO);
        }

        transicaoService.registrar(id, TipoFaturaEvento.PROTOCOLADA, usuario, request.responsavel());

        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(usuario);
        try {
            espelhoRepository.saveAndFlush(espelho);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("numeroFatura", MENSAGEM_NUMERO_REPETIDO);
        }

        return consultaService.detalhar(id);
    }

    private String validarNumero(String numeroFatura) {
        String numero = numeroFatura == null ? "" : numeroFatura.trim();
        if (!FORMATO_NUMERO.matcher(numero).matches()) {
            throw new RegraDeNegocioException(MENSAGEM_FORMATO);
        }
        return numero;
    }
}