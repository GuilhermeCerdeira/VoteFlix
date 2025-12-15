package br.com.guilhermando.protocolo;

/**
 * Define todos os códigos de erro padronizados para a comunicação
 * entre o cliente e o servidor.
 */
public enum ErroProtocolo {

    OPERACAO_INVALIDA("400", "Erro: Operação não encontrada ou inválida"),
    
    CHAVES_FALTANTES("422", "Erro: Chaves faltantes ou invalidas"),
    
    TOKEN_INVALIDO("401", "Erro: Token inválido"),

    SEM_PERMISSAO("403", "Erro: Sem permissão"),

    RECURSO_INEXISTENTE("404", "Erro: Recurso inexistente"),

    CAMPOS_INVALIDOS("405", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres"),
    
    RECURSO_JA_EXISTE("409", "Erro: Recurso ja existe"), 

    FALHA_INTERNA("500", "Erro: Falha interna do servidor");

    

    private final String statusCode;
    private final String mensagem;


    ErroProtocolo(String statusCode, String mensagem) {
        this.statusCode = statusCode;
        this.mensagem = mensagem;
    }

    public String getStatusCode() {
        return this.statusCode;
    }

    public String getMensagem() {
        return this.mensagem;
    }
}