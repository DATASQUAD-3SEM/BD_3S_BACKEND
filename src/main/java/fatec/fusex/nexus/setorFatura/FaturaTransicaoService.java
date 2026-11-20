package fatec.fusex.nexus.setorFatura;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class FaturaTransicaoService {

    private static final int RESPONSAVEL_MINIMO = 3;
    private static final int RESPONSAVEL_MAXIMO = 100;
    public static final int PRAZO_OCS_DIAS_UTEIS = 3;

    private final EspelhoRepository espelhoRepository;
    private final FaturaEventoRepository eventoRepository;

    public FaturaTransicaoService(EspelhoRepository espelhoRepository, FaturaEventoRepository eventoRepository) {
        this.espelhoRepository = espelhoRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional
    public FaturaEvento registrar(Long idEspelho, TipoFaturaEvento tipo, Usuario usuario, String responsavel) {
        return registrar(idEspelho, tipo, usuario, responsavel, LocalDateTime.now(Beneficiario.FUSO));
    }

    FaturaEvento registrar(Long idEspelho, TipoFaturaEvento tipo, Usuario usuario, String responsavel,
                           LocalDateTime agora) {
        String nome = validarResponsavel(responsavel);

        Espelho espelho = espelhoRepository.buscarComLockPorId(idEspelho)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Espelho nao encontrado"));
        SituacaoFatura atual = espelho.getSituacaoFatura();
        if (atual == null) {
            throw new ConflitoException("Espelho ainda nao foi enviado pela OCS");
        }

        List<FaturaEvento> eventos = eventoRepository
                .findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(idEspelho);
        SituacaoFatura nova = novaSituacao(atual, tipo, eventos, agora)
                .orElseThrow(() -> new ConflitoException("Esta acao nao e permitida na situacao atual da fatura"));

        espelho.setSituacaoFatura(nova);

        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(espelho);
        evento.setTipo(tipo);
        evento.setUsuario(usuario);
        evento.setResponsavel(nome);
        evento.setDataEvento(agora);
        return eventoRepository.save(evento);
    }

    private Optional<SituacaoFatura> novaSituacao(SituacaoFatura atual, TipoFaturaEvento tipo,
                                                  List<FaturaEvento> eventos, LocalDateTime agora) {
        if (atual == SituacaoFatura.RECEBIDO && tipo == TipoFaturaEvento.PROTOCOLADA) {
            return Optional.of(SituacaoFatura.PROTOCOLADA);
        }
        if (atual == SituacaoFatura.PROTOCOLADA && tipo == TipoFaturaEvento.ENVIADA_LISURA) {
            return Optional.of(SituacaoFatura.EM_LISURA);
        }
        if (atual == SituacaoFatura.EM_LISURA && tipo == TipoFaturaEvento.ENVIADA_OCS) {
            return Optional.of(SituacaoFatura.AGUARDANDO_OCS);
        }
        if (atual == SituacaoFatura.EM_LISURA && tipo == TipoFaturaEvento.ENVIADA_AUDITORIA) {
            return Optional.of(SituacaoFatura.EM_AUDITORIA);
        }
        if (atual == SituacaoFatura.AGUARDANDO_OCS && tipo == TipoFaturaEvento.CIENTE_OCS) {
            return Optional.of(SituacaoFatura.AGUARDANDO_OCS);
        }
        if (atual == SituacaoFatura.AGUARDANDO_OCS && tipo == TipoFaturaEvento.CONTESTADA_OCS) {
            return Optional.of(SituacaoFatura.EM_LISURA);
        }
        if (atual == SituacaoFatura.AGUARDANDO_OCS && tipo == TipoFaturaEvento.ENVIADA_AUDITORIA
                && prazoLiberado(eventos, agora)) {
            return Optional.of(SituacaoFatura.EM_AUDITORIA);
        }
        if (atual == SituacaoFatura.EM_AUDITORIA && tipo == TipoFaturaEvento.VISUALIZADA) {
            return Optional.of(SituacaoFatura.AUDITADA);
        }
        return Optional.empty();
    }

    public static boolean prazoLiberado(List<FaturaEvento> eventos, LocalDateTime agora) {
        int ultimoEnvio = -1;
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).getTipo() == TipoFaturaEvento.ENVIADA_OCS) {
                ultimoEnvio = i;
            }
        }
        if (ultimoEnvio < 0) {
            return false;
        }

        boolean cienteNaRodada = eventos.subList(ultimoEnvio + 1, eventos.size()).stream()
                .anyMatch(e -> e.getTipo() == TipoFaturaEvento.CIENTE_OCS);
        LocalDateTime envio = eventos.get(ultimoEnvio).getDataEvento();
        return cienteNaRodada || !agora.isBefore(somarDiasUteis(envio, PRAZO_OCS_DIAS_UTEIS));
    }

    public static LocalDateTime somarDiasUteis(LocalDateTime inicio, int dias) {
        LocalDateTime resultado = inicio;
        int restantes = dias;
        while (restantes > 0) {
            resultado = resultado.plusDays(1);
            DayOfWeek dia = resultado.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                restantes--;
            }
        }
        return resultado;
    }

    private String validarResponsavel(String responsavel) {
        String nome = responsavel == null ? "" : responsavel.trim();
        if (nome.length() < RESPONSAVEL_MINIMO || nome.length() > RESPONSAVEL_MAXIMO) {
            throw new DadosInvalidosException("responsavel",
                    "Informe o nome do responsável (mínimo de 3 caracteres).");
        }
        return nome;
    }
}