package fatec.fusex.nexus.guia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuiaRepositoryTest {

    private final GuiaRepository repository;

    private final EntityManager em;

    private static final PageRequest ATE_50 = PageRequest.of(0, 50);

    GuiaRepositoryTest(GuiaRepository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    @Test
    void buscarConcluidas_porNome_ignoraCaixa() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610701", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "antonio", null, ATE_50);

        assertThat(achadas).extracting(Guia::getNumero).containsExactly("202610701");
    }

    @Test
    void buscarConcluidas_porPrecCp_encontraMesmoComTermoDeNomeQueNaoBate() {
        Ocs ocs = novaOcs("Laboratorio Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610655", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "zzz", "45872213300", ATE_50);

        assertThat(achadas).extracting(Guia::getNumero).containsExactly("202610655");
    }

    @Test
    void buscarConcluidas_porNumeroDaGuia() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610701", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "2026107", null, ATE_50);

        assertThat(achadas).extracting(Guia::getNumero).containsExactly("202610701");
    }

    @Test
    void buscarConcluidas_guiaDePreGuiaNaoConcluida_ficaDeFora() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610999", antonio, ocs, PreGuiaStatus.EM_ANALISE, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "antonio", "45872213300", ATE_50);

        assertThat(achadas).isEmpty();
    }

    @Test
    void buscarConcluidas_precCpNulo_naoUsaOCriterioDePrecCp() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610701", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "zzz", null, ATE_50);

        assertThat(achadas).isEmpty();
    }

    @Test
    void buscarConcluidas_ordenaPorDataDeInsercaoCrescente() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610800", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 22, 9, 0));
        novaGuia("202610700", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 9, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "antonio", null, ATE_50);

        assertThat(achadas).extracting(Guia::getNumero).containsExactly("202610700", "202610800");
    }

    @Test
    void buscarConcluidas_respeitaOLimiteDePagina() {
        Ocs ocs = novaOcs("Hospital Exemplo");
        Beneficiario antonio = novoBeneficiario("11111111111", "Antonio Carlos Ferreira", "45872213300");
        novaGuia("202610700", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 9, 1, 9, 0));
        novaGuia("202610800", antonio, ocs, PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarConcluidasParaRecepcao(
                PreGuiaStatus.CONCLUIDA, "antonio", null, PageRequest.of(0, 1));

        assertThat(achadas).hasSize(1);
    }


    private Ocs novaOcs(String nome) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CT-" + nome.hashCode());
        em.persist(ocs);
        return ocs;
    }

    private Beneficiario novoBeneficiario(String login, String nome, String precCp) {
        Usuario usuario = novoUsuario(login, PerfilUsuario.BENEFICIARIO);

        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        em.persist(b);
        return b;
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        em.persist(usuario);
        return usuario;
    }

    private void novaGuia(String numero, Beneficiario beneficiario, Ocs ocs,
                          PreGuiaStatus status, LocalDateTime insercao) {
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(status);
        em.persist(preGuia);

        Usuario inseridaPor = novoUsuario("recepcao-" + numero, PerfilUsuario.SETOR_GUIAS);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(numero);
        guia.setArquivoUrl("/arquivos/guia-" + numero + ".pdf");
        guia.setDataInsercao(insercao);
        guia.setUsuario(inseridaPor);
        em.persist(guia);
    }
}