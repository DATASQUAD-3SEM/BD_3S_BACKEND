package fatec.fusex.nexus.preguia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.procedimento.ProcedimentoExame;
import fatec.fusex.nexus.procedimento.ProcedimentoExameRepository;

/** Confere se os dados de exemplo (db/seed) carregaram e se a pre-guia grava com OCS e procedimentos. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PreGuiaRepositoryTest {

    @Autowired
    private PreGuiaRepository preGuiaRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;
    @Autowired
    private OcsRepository ocsRepository;
    @Autowired
    private ProcedimentoExameRepository procedimentoRepository;

    @Test
    void seedFoiCarregado() {
        assertThat(ocsRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(procedimentoRepository.count()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void salvaPreGuiaComOcsEProcedimentos() {
        Beneficiario b = new Beneficiario();
        b.setCpf("98765432100");
        b.setPrecCp("9998887776");
        b.setNome("Beltrana");
        b.setSenha("hash-de-teste");
        beneficiarioRepository.save(b);

        Ocs ocs = ocsRepository.findAll().get(0);
        List<ProcedimentoExame> procedimentos = procedimentoRepository.findByCodigoTuss("40304361");

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(b);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuia.getProcedimentos().addAll(procedimentos);
        preGuiaRepository.saveAndFlush(preGuia);

        List<PreGuia> doBeneficiario = preGuiaRepository.findByBeneficiarioId(b.getId());
        assertThat(doBeneficiario).hasSize(1);
        assertThat(doBeneficiario.get(0).getStatus()).isEqualTo(StatusPreGuia.RASCUNHO);
        assertThat(doBeneficiario.get(0).getProcedimentos()).hasSize(1);
    }
}
