package fatec.fusex.nexus.beneficiario;


public enum FaixaEtaria {

    MENOR_DE_18("Menor de 18", 0, 17),
    DE_18_A_30("18 a 30", 18, 30),
    DE_31_A_40("31 a 40", 31, 40),
    DE_41_A_50("41 a 50", 41, 50),
    DE_51_A_60("51 a 60", 51, 60),
    DE_61_A_70("61 a 70", 61, 70),
    ACIMA_DE_70("Acima de 70", 71, Integer.MAX_VALUE);

    private final String rotulo;
    private final int idadeMinima;
    private final int idadeMaxima;

    FaixaEtaria(String rotulo, int idadeMinima, int idadeMaxima) {
        this.rotulo = rotulo;
        this.idadeMinima = idadeMinima;
        this.idadeMaxima = idadeMaxima;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static FaixaEtaria de(int idade) {
        if (idade < 0) {
            throw new IllegalArgumentException("Idade negativa: data de nascimento no futuro?");
        }
        for (FaixaEtaria faixa : values()) {
            if (idade >= faixa.idadeMinima && idade <= faixa.idadeMaxima) {
                return faixa;
            }
        }
        throw new IllegalStateException("Nenhuma faixa etaria para a idade " + idade);
    }
}
