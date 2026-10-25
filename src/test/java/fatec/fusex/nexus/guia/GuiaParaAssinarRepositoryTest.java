package fatec.fusex.nexus.guia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da consulta de Guias pendentes de assinatura por papel (T5-BE1) e da sua ordenacao.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuiaParaAssinarRepositoryTest {

    private final GuiaRepository repository;

    private final EntityManager em;

    GuiaParaAssinarRepositoryTest(GuiaRepository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    @Test
    void buscarSemAssinatura_medico_incluiGuiaSemNenhumaAssinatura() {
        novaGuia("202610701", LocalDateTime.of(2026, 10, 1, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO);

        assertThat(numeros(achadas)).containsExactly("202610701");
    }

    @Test
    void buscarSemAssinatura_guiaComSoAssinaturaDeChefe_apareceParaMedicoENaoParaChefe() {
        Guia guia = novaGuia("202610702", LocalDateTime.of(2026, 10, 1, 9, 0));
        assine(guia, AssinaturaGuiaPapel.CHEFE, novoUsuario("chefe-202610702", PerfilUsuario.CHEFE_FUSEX));
        em.flush();

        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO)))
                .containsExactly("202610702");
        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.CHEFE)))
                .doesNotContain("202610702");
    }

    @Test
    void buscarSemAssinatura_guiaComSoAssinaturaDeMedico_apareceParaChefeENaoParaMedico() {
        Guia guia = novaGuia("202610703", LocalDateTime.of(2026, 10, 1, 9, 0));
        assine(guia, AssinaturaGuiaPapel.MEDICO, novoUsuario("medico-202610703", PerfilUsuario.MEDICO));
        em.flush();

        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.CHEFE)))
                .containsExactly("202610703");
        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO)))
                .doesNotContain("202610703");
    }

    @Test
    void buscarSemAssinatura_guiaComAsDuasAssinaturas_ficaForaDasDuasListas() {
        Guia concluida = novaGuia("202610704", LocalDateTime.of(2026, 10, 1, 9, 0));
        assine(concluida, AssinaturaGuiaPapel.MEDICO, novoUsuario("medico-202610704", PerfilUsuario.MEDICO));
        assine(concluida, AssinaturaGuiaPapel.CHEFE, novoUsuario("chefe-202610704", PerfilUsuario.CHEFE_FUSEX));
        em.flush();

        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO)))
                .doesNotContain("202610704");
        assertThat(numeros(repository.buscarSemAssinatura(AssinaturaGuiaPapel.CHEFE)))
                .doesNotContain("202610704");
    }

    @Test
    void buscarSemAssinatura_ordenaDaMaisAntigaParaAMaisRecente() {
        novaGuia("202610706", LocalDateTime.of(2026, 10, 6, 9, 0));
        novaGuia("202610705", LocalDateTime.of(2026, 10, 5, 9, 0));
        em.flush();

        List<Guia> achadas = repository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO);

        assertThat(numeros(achadas)).containsExactly("202610705", "202610706");
    }

    private static List<String> numeros(List<Guia> guias) {
        return guias.stream().map(Guia::getNumero).toList();
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        em.persist(usuario);
        return usuario;
    }

    private void assine(Guia guia, AssinaturaGuiaPapel papel, Usuario usuario) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setGuia(guia);
        assinatura.setPapel(papel);
        assinatura.setUsuario(usuario);
        em.persist(assinatura);
    }

    private Guia novaGuia(String numero, LocalDateTime insercao) {
        Ocs ocs = new Ocs();
        ocs.setNome("Ocs " + numero);
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CT-" + numero);
        em.persist(ocs);

        Usuario usuarioBeneficiario = novoUsuario("benef-" + numero, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioBeneficiario);
        beneficiario.setNome("Beneficiario " + numero);
        beneficiario.setPrecCp("99" + numero);
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        em.persist(beneficiario);

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        em.persist(preGuia);

        Usuario inseridaPor = novoUsuario("setor-" + numero, PerfilUsuario.SETOR_GUIAS);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(numero);
        guia.setArquivoUrl("/arquivos/guia-" + numero + ".pdf");
        guia.setDataInsercao(insercao);
        guia.setUsuario(inseridaPor);
        em.persist(guia);
        return guia;
    }
}