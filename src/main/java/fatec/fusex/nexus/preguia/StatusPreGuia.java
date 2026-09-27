package fatec.fusex.nexus.preguia;

//Status possíveis da pré-guia ao longo do fluxo.

public enum StatusPreGuia {

    //Beneficiário ainda está montando a pré-guia.

    RASCUNHO,

    //Pré-guia enviada e aguardando análise.

    PENDENTE,

    //Pré-guia em processo de análise.

    EM_ANALISE,


    //Pré-guia aprovada.

    APROVADA
}