package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PreGuiaRepositoryTest {

    @Autowired
    private PreGuiaRepository preGuiaRepository;

    @Autowired
    private EntityManager em;

    @Test
    void salvaPreGuiaEBuscaPeloBeneficiario() {
        Usuario usuario = new Usuario();
        usuario.setLogin("98765432100");
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        em.persist(usuario);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario);
        beneficiario.setNome("Beltrana");
        beneficiario.setPrecCp("9998887776");
        beneficiario.setDataNascimento(LocalDate.of(1990, 1, 1));
        beneficiario.setTelefone("12987654321");
        em.persist(beneficiario);

        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio Teste");
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CTR-TESTE-001");
        em.persist(ocs);

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuiaRepository.saveAndFlush(preGuia);

        List<PreGuia> doBeneficiario =
                preGuiaRepository.findByBeneficiarioUsuarioId(usuario.getIdUsuario());

        assertThat(doBeneficiario).hasSize(1);
        assertThat(doBeneficiario.get(0).getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
    }
}