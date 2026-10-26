package fatec.fusex.nexus.common.validation;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Regras de formato compartilhadas pelos cadastros e edicoes feitos pelo Chefe (beneficiarios e medicos).
 *
 * Todos os metodos lancam {@link DadosInvalidosException} (422) apontando o campo com problema,
 * e devolvem o valor ja normalizado para gravacao.
 */
public final class ValidacaoCadastro {

    public static final int NOME_MAXIMO = 150;
    private static final int SENHA_MINIMO = 8;
    private static final int SENHA_MAXIMO = 72; // limite do BCrypt

    private ValidacaoCadastro() {
    }

    /** Exige texto preenchido e com no maximo {@code maximo} caracteres; devolve o valor sem espacos nas pontas. */
    public static String obrigatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException(campo, "Campo obrigatorio");
        }
        String limpo = valor.trim();
        if (limpo.length() > maximo) {
            throw new DadosInvalidosException(campo, "Maximo de " + maximo + " caracteres");
        }
        return limpo;
    }

    /** Aceita CPF com ou sem mascara (000.000.000-00), confere os 2 digitos verificadores e devolve so os digitos. */
    public static String normalizarCpf(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("cpf", "Campo obrigatorio");
        }
        String cpf = informado.trim().replace(".", "").replace("-", "");
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1 || !digitosConferem(cpf)) {
            throw new DadosInvalidosException("cpf", "CPF invalido");
        }
        return cpf;
    }

    /** Aceita Prec CP com 11 digitos, com ou sem pontos e hifen, e devolve so os digitos. */
    public static String normalizarPrecCp(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new DadosInvalidosException("precCp", "Campo obrigatorio");
        }
        String precCp = informado.trim().replace(".", "").replace("-", "");
        if (!precCp.matches("[0-9]{11}")) {
            throw new DadosInvalidosException("precCp", "Prec CP invalido: informe 11 digitos");
        }
        return precCp;
    }

    /** Exige senha entre 8 e 72 caracteres; {@code campo} e o nome devolvido no erro (senha ou novaSenha). */
    public static void validarSenha(String senha, String campo) {
        if (senha == null || senha.length() < SENHA_MINIMO) {
            throw new DadosInvalidosException(campo, "A senha deve ter no minimo " + SENHA_MINIMO + " caracteres");
        }
        if (senha.length() > SENHA_MAXIMO) {
            throw new DadosInvalidosException(campo, "A senha deve ter no maximo " + SENHA_MAXIMO + " caracteres");
        }
    }

    private static boolean digitosConferem(String cpf) {
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
                && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    // Calculo padrao do digito verificador do CPF (modulo 11) sobre os primeiros "quantidade" digitos
    private static int digitoVerificador(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
