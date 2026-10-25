package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssinaturaGuiaUnicaRepositoryTest {

    private final AssinaturaGuiaRepository assinaturaRepository;

    private final EntityManager em;

    AssinaturaGuiaUnicaRepositoryTest(AssinaturaGuiaRepository assinaturaRepository, EntityManager em) {
        this.assinaturaRepository = assinaturaRepository;
        this.em = em;
    }

    @Test
    void mesmoPapelNaMesmaGuia_violaAConstraintUnica() {
        Guia guia = novaGuia("202610801");
        Usuario medico1 = novoUsuario("medico-a", PerfilUsuario.MEDICO);
        Usuario medico2 = novoUsuario("medico-b", PerfilUsuario.MEDICO);
        assine(guia, AssinaturaGuiaPapel.MEDICO, medico1);
        em.flush();

        assertThatThrownBy(() -> {
            assine(guia, AssinaturaGuiaPapel.MEDICO, medico2);
            em.flush();
        }).isInstanceOfAny(PersistenceException.class, DataIntegrityViolationException.class);
    }

    @Test
    void papeisDiferentesNaMesmaGuia_saoAceitos() {
        Guia guia = novaGuia("202610802");
        assine(guia, AssinaturaGuiaPapel.MEDICO, novoUsuario("medico-c", PerfilUsuario.MEDICO));
        assine(guia, AssinaturaGuiaPapel.CHEFE, novoUsuario("chefe-c", PerfilUsuario.CHEFE_FUSEX));
        em.flush();

        List<AssinaturaGuia> assinaturas = assinaturaRepository.buscarComLockPorGuia(guia.getIdGuia());

        assertThat(assinaturas).hasSize(2);
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

    private Guia novaGuia(String numero) {
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
        guia.setDataInsercao(LocalDateTime.now());
        guia.setUsuario(inseridaPor);
        em.persist(guia);
        return guia;
    }
}