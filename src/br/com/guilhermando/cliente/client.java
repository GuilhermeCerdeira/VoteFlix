package br.com.guilhermando.cliente;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class client {
    public static final String ANSI_RESET = "\u001b[0m";
    public static final String ANSI_RED = "\u001b[31m";
    public static final String ANSI_GREEN = "\u001b[32m";
    private static final Gson gson = new Gson();
    private static String currentToken = null;
    private static String currentUserRole = null;

    public static void main(String[] args) {
        Socket clientSocket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.print("Qual o IP do servidor? ");
            String serverIP = teclado.readLine();
            System.out.print("Qual a Porta do servidor? ");
            int serverPort = Integer.parseInt(teclado.readLine());

            System.out.println("Tentando conectar com host " + serverIP + " na porta " + serverPort);
            clientSocket = new Socket(serverIP, serverPort);

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true); 
            

            boolean keepRunning = true;
            while (keepRunning) {
                if (currentToken == null) {
                    System.out.println("\n--- MENU PRINCIPAL ---");
                    System.out.println("1. Fazer Login");
                    System.out.println("2. Cadastrar Novo Usuario");
                    System.out.println("3. Sair");
                    System.out.print("Escolha uma opcao: ");
                    String escolha = teclado.readLine().toUpperCase();

                    if ("1".equals(escolha)) {
                        realizarLogin(in, out, teclado);
                    } else if ("2".equals(escolha)) {
                        cadastrarUsuario(in, out, teclado);
                    } else if ("3".equals(escolha)) {
                        System.out.println("Saindo...");
                        keepRunning = false;
                    } else {
                        System.out.println("Opcao invalida.");
                    }
                } else {
                    keepRunning = menuUsuarioLogado(in, out, teclado);
                }
            }
        } catch (Exception e) {
            System.err.println("Conexão falhou:");
        } finally {
            try { 
                if (clientSocket != null) clientSocket.close(); 
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket:");
            }
            System.out.println("Conexao encerrada.");
        }
    }

    private static void realizarLogin(BufferedReader in, PrintWriter out, BufferedReader teclado) throws IOException {
        System.out.print("Digite o usuário: ");
        String usuario = teclado.readLine();
        System.out.print("Digite a senha: ");
        String senha = teclado.readLine();

        Map<String, String> request = new HashMap<>();
        request.put("operacao", "LOGIN");
        request.put("usuario", usuario);
        request.put("senha", senha);
        enviarJson(out, request);

        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");

        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            currentToken = (String) response.get("token");
            System.out.println("Token recebido e armazenado!");

            
            if (usuario.equalsIgnoreCase("admin")) {
                System.out.println(ANSI_GREEN + "Acesso de administrador assumido." + ANSI_RESET);
                currentUserRole = "admin";
            } else {
                currentUserRole = "user";
            }

        } else {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
        }
    }

    private static void adminCriarFilme(BufferedReader in, PrintWriter out, BufferedReader teclado, Map<String, Object> baseRequest) throws IOException {
        System.out.println("\n--- CADASTRAR NOVO FILME ---");
        Map<String, Object> filme = new HashMap<>();
        
        System.out.print("Título (3-30 chars): "); 
        String titulo = teclado.readLine();
        System.out.print("Diretor (3-30 chars): "); 
        String diretor = teclado.readLine();
        System.out.print("Ano (ex: 2023): "); 
        String ano = teclado.readLine();
        
        System.out.print("Gêneros (separados por vírgula, ex: Acao,Drama): \n");
        String generosInput = teclado.readLine();
        List<String> generosList = Arrays.stream(generosInput.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        System.out.print("Sinopse (opcional): "); 
        String sinopse = teclado.readLine();

        filme.put("titulo", titulo);
        filme.put("diretor", diretor);
        filme.put("ano", ano);
        filme.put("genero", generosList); 
        filme.put("sinopse", sinopse);

        Map<String, Object> request = new HashMap<>();
        request.put("token", baseRequest.get("token"));
        request.put("operacao", "CRIAR_FILME");
        request.put("filme", filme);
        
        enviarJson(out, request);
        tratarRespostaSimples(in);
    }

    public static void adminEditarFilme(BufferedReader in, PrintWriter out, BufferedReader teclado, Map<String, Object> baseRequest) throws IOException {
        System.out.println("\n--- ATUALIZAR DADOS DO FILME ---");
        System.out.print("ID do filme a ser atualizado: ");
        String idFilme = teclado.readLine();

        Map<String, Object> filmeAtualizado = new HashMap<>();
        filmeAtualizado.put("id", idFilme);

        System.out.print("Novo Título (deixe vazio para manter o atual): ");
        String novoTitulo = teclado.readLine();
        if (!novoTitulo.isEmpty()) filmeAtualizado.put("titulo", novoTitulo);

        System.out.print("Novo Diretor (deixe vazio para manter o atual): ");
        String novoDiretor = teclado.readLine();
        if (!novoDiretor.isEmpty()) filmeAtualizado.put("diretor", novoDiretor);

        System.out.print("Novo Ano (deixe vazio para manter o atual): ");
        String novoAno = teclado.readLine();
        if (!novoAno.isEmpty()) filmeAtualizado.put("ano", novoAno);

        System.out.println("Novos Gêneros (separados por vírgula, deixe vazio para manter os atuais): ");
        String generosRaw = teclado.readLine();
        if (!generosRaw.isEmpty()) {
            List<String> generosList = Arrays.asList(generosRaw.split(","));
            filmeAtualizado.put("genero", generosList);
        }

        System.out.print("Nova Sinopse (deixe vazio para manter a atual): ");
        String novaSinopse = teclado.readLine();
        if (!novaSinopse.isEmpty()) filmeAtualizado.put("sinopse", novaSinopse);

        Map<String, Object> request = new HashMap<>();
        request.put("token", baseRequest.get("token"));
        request.put("operacao", "EDITAR_FILME");
        request.put("filme", filmeAtualizado);

        enviarJson(out, request);
        tratarRespostaSimples(in);
    }

    private static void listarFilmes(BufferedReader in, PrintWriter out, BufferedReader teclado, Map<String, Object> baseRequest) throws IOException {
        
        baseRequest.put("operacao", "LISTAR_FILMES");
        enviarJson(out, baseRequest);
        
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");

        if (!"200".equals(status)) {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if ("401".equals(status)) resetSession();
            return;
        }

        System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
        
        List<Map<String, Object>> allFilmes;
        if (response.containsKey("filmes")) {
            allFilmes = (List<Map<String, Object>>) response.get("filmes");
        } else {
            allFilmes = (List<Map<String, Object>>) response.get("dados");
        }
        
        if (allFilmes == null || allFilmes.isEmpty()) {
            System.out.println("\n[AVISO] Não há filmes cadastrados no momento.");
            return;
        }

        System.out.println("\n--- FILMES CADASTRADOS ---");
        for (Map<String, Object> f : allFilmes) {
            System.out.println("----------------------------------------");
            
            Object idObj = f.get("id");
            if (idObj instanceof Double) {
                System.out.printf("ID: %d\n", ((Double)idObj).intValue());
            } else {
                System.out.printf("ID: %s\n", idObj.toString());
            }
            
            System.out.printf("Título: %s (%s)\n", f.get("titulo"), f.get("ano"));
            System.out.printf("Diretor: %s\n", f.get("diretor"));
            System.out.printf("Nota: %s\n", f.get("nota"));
            
            if (f.containsKey("qtd_avaliacoes")) {
                System.out.printf("Qtd. Avaliações: %s\n", f.get("qtd_avaliacoes"));
            }

            List<?> generosList = (List<?>) f.get("genero");
            if (generosList != null) {
                String listaBonita = generosList.toString().replace("[", "").replace("]", "");
                System.out.println("Gêneros: " + listaBonita);
            } else {
                System.out.println("Gêneros: (Sem dados)");
            }
            
            String sinopse = (String) f.get("sinopse");
            if (sinopse != null && !sinopse.isEmpty()) {
                System.out.println("Sinopse: " + sinopse);
            }
        }
        System.out.println("----------------------------------------");
    }

    private static void cadastrarUsuario(BufferedReader in, PrintWriter out, BufferedReader teclado) throws IOException {
        System.out.print("Digite o novo usuario: ");
        String usuario = teclado.readLine();
        System.out.print("Digite a nova senha: ");
        String senha = teclado.readLine();

        Map<String, Object> response = ClientService.enviarCadastro(out, in, usuario, senha);

        if (response != null) {
            String status = (String) response.get("status");
            String mensagem = (String) response.get("mensagem");
            
            if ("201".equals(status)) {
                System.out.println(ANSI_GREEN + "Servidor: " + mensagem + ANSI_RESET);
            } else {
                System.out.println(ANSI_RED + "Servidor: " + mensagem + ANSI_RESET);
            }
        }
    }

    private static boolean tratarRespostaSimples(BufferedReader in) throws IOException {
    Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
    String status = (String) response.get("status");
    System.out.println("Servidor: [" + status + "] " + response.get("mensagem"));


    if ("401".equals(status)) {
         System.out.println("Sessao invalida. Deslogando...");
         currentToken = null; currentUserRole = null;
         return false;
    }
    return "200".equals(status) || "201".equals(status);
}

private static boolean menuUsuarioLogado(BufferedReader in, PrintWriter out, BufferedReader teclado) throws IOException {
    System.out.println("\n--- MENU DO USUARIO ---");
    System.out.println("1. Ver meus dados");
    System.out.println("2. Alterar minha senha");
    System.out.println("3. Listar filmes");
    System.out.println("4. Criar uma review para um filme");
    System.out.println("5. Editar uma review existente");
    System.out.println("6. Listar minhas reviews");
    System.out.println("7. Ver detalhes de um filme (ID)");
    System.out.println("8. Deletar minha conta");

    if ("admin".equals(currentUserRole)) {
        System.out.println("\n--- PAINEL ADMIN ---");
        System.out.println("A1. Deletar um usuario");
        System.out.println("A2. Listar todos os usuarios");
        System.out.println("A3. Atualizar senha de um usuario");
        System.out.println("A4. Cadastrar um novo filme");
        System.out.println("A5. Editar um filme");
        System.out.println("A6. Excluir um filme");
        System.out.println("A7. Excluir Review");
    }

    System.out.println("\n10. Sair (Logout)");
    System.out.print("Escolha uma opcao: ");
    String escolha = teclado.readLine().toUpperCase();
    
    Map<String, Object> request = new HashMap<>();
    request.put("token", currentToken);

    switch (escolha) {
        case "1":
            request.put("operacao", "LISTAR_PROPRIO_USUARIO");
            enviarJson(out, request);
            tratarRespostaDados(in);
            break;
        case "2":
            System.out.print("Digite sua NOVA senha: ");
            String newPassword = teclado.readLine();
            request.put("operacao", "EDITAR_PROPRIO_USUARIO");
            request.put("nova_senha", newPassword);
            enviarJson(out, request);
            tratarResposta(in, null);
            break;
        case "3":
            listarFilmes(in, out, teclado, request);
            break;
        case "4":
            System.out.print("Digite o ID do filme que deseja avaliar: ");
            String idFilme = teclado.readLine();
            
            System.out.print("Digite um Título para sua review: ");
            String tituloReview = teclado.readLine();
            
            System.out.print("Digite sua nota para o filme (1 a 5): ");
            String notaStr = teclado.readLine();
            
            System.out.print("Escreva sua review (descricao): ");
            String descricao = teclado.readLine();
            
            request.put("operacao", "CRIAR_REVIEW");
            Map<String, Object> review = new HashMap<>();
            review.put("id_filme", idFilme);
            review.put("titulo", tituloReview);
            review.put("nota", notaStr);
            review.put("descricao", descricao);
            
            request.put("review", review);
            enviarJson(out, request);
            tratarRespostaSimples(in);
            break;

        case "5":
            System.out.print("Digite o ID da REVIEW que deseja editar: "); 
            String idReviewEditar = teclado.readLine();
            
            System.out.print("Digite o NOVO título da review (opcional/mantido): ");
            String novoTitulo = teclado.readLine();
            
            System.out.print("Digite a NOVA nota (1-5): ");
            String novaNota = teclado.readLine();
            
            System.out.print("Digite a NOVA descrição (max 250 chars): ");
            String novaDescricao = teclado.readLine();
            
            request.put("operacao", "EDITAR_REVIEW");
            Map<String, Object> reviewEditada = new HashMap<>();
            reviewEditada.put("id", idReviewEditar);
            reviewEditada.put("titulo", novoTitulo);
            reviewEditada.put("nota", novaNota);
            reviewEditada.put("descricao", novaDescricao);
            
            request.put("review", reviewEditada);
            
            enviarJson(out, request);
            tratarRespostaSimples(in);
            break;
        case "6":
            request.put("operacao", "LISTAR_REVIEWS_USUARIO");
            enviarJson(out, request);
            listarMinhasReviews(in);
            break;

        case "7":
                System.out.print("Digite o ID do filme: ");
                String idBusca = teclado.readLine();
                request.put("operacao", "BUSCAR_FILME_ID");
                request.put("id_filme", idBusca);
                enviarJson(out, request);
                buscarFilmeId(in); 
                break;

        case "8":
                System.out.print("Insira o id da sua review de que deseja excluir: ");
                String idReviewExcluir = teclado.readLine();
                request.put("operacao", "EXCLUIR_REVIEW");
                request.put("id_review", idReviewExcluir);
                enviarJson(out, request);
                tratarRespostaSimples(in);
                break;

        case "9":
            System.out.print("Tem certeza que deseja deletar sua conta? (s/n): ");
            if ("s".equalsIgnoreCase(teclado.readLine())) {
                request.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
                enviarJson(out, request);
                if (tratarResposta(in, null)) {
                    resetSession();
                    return false; 
                }
            } else {
                System.out.println("Operacao cancelada.");
            }
            break;

        case "A1":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            System.out.print("Digite o NOME do usuario que você deseja deletar: ");
            String usuarioAlvo = teclado.readLine();
            request.put("operacao", "ADMIN_EXCLUIR_USUARIO");
            request.put("usuario_alvo", usuarioAlvo);
            enviarJson(out, request);
            tratarResposta(in, null);
            break;
        case "A2":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            request.put("operacao", "LISTAR_USUARIOS");
            enviarJson(out, request);
            tratarRespostaListaUsuarios(in);
            break;
        case "A3":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            System.out.print("Digite o NOME do usuario que você deseja atualizar: ");
            String usuarioParaAtualizar = teclado.readLine();
            System.out.print("Digite a NOVA senha para esse usuario: ");
            String novaSenhaParaAtualizar = teclado.readLine();
            request.put("operacao", "ADMIN_EDITAR_USUARIO");
            request.put("usuario_alvo", usuarioParaAtualizar);
            request.put("nova_senha", novaSenhaParaAtualizar);
            enviarJson(out, request);
            tratarResposta(in, null);
            break;
        case "A4":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            adminCriarFilme(in, out, teclado, request);
            break;
        
        case "A5":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            adminEditarFilme(in, out, teclado, request);
            break;

        case "A6":
            if (!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            System.out.print("Digite o ID do filme que deseja excluir: ");
            String idFilmeParaExcluir = teclado.readLine();
            request.put("operacao", "EXCLUIR_FILME");
            request.put("id", idFilmeParaExcluir);
            enviarJson(out, request);
            tratarResposta(in, null);
            break;
        case "A7":
            if(!"admin".equals(currentUserRole)) { System.out.println("Opcao invalida."); break; }
            System.out.print("Digite o ID da REVIEW que deseja excluir: ");
            String idReviewExcluirADM = teclado.readLine();
            request.put("operacao", "EXCLUIR_REVIEW");
            request.put("id_review", idReviewExcluirADM);
            enviarJson(out, request);
            tratarRespostaSimples(in);
            break;
        
        case "10":
            request.put("operacao", "LOGOUT");
            enviarJson(out, request);
            tratarResposta(in, null);
            resetSession();
            return false;

        default:
            System.out.println("Opcao invalida.");
            break;
    }
    
    return currentToken != null; 
}

    private static void enviarJson(PrintWriter out, Object requestMap) {
    String json = gson.toJson(requestMap);
    System.out.println("[JSON ENVIADO]: " + json);
    out.println(json);
}

    private static boolean tratarResposta(BufferedReader in, String tipoAcao) throws IOException {
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");
        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if (tipoAcao != null && tipoAcao.equals("LISTA_USUARIOS")) {
                List<String> usersList = (List<String>) response.get("response"); 
                System.out.println("-- Usuarios Cadastrados --");
                for (String u : usersList) {
                    System.out.println("- " + u);
                }
            }
        } else if (response.containsKey("dados")) {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            System.out.println("Dados: " + response.get("dados"));
        } 
        if ("401".equals(status)) resetSession();
        return "200".equals(status) || "201".equals(status);
    }
        
    private static void tratarRespostaListaUsuarios(BufferedReader in) throws IOException {
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");

        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            
            List<Map<String, Object>> usersList = (List<Map<String, Object>>) response.get("usuarios");
            
            System.out.println("-- Usuarios Cadastrados --");
            for (Map<String, Object> user : usersList) {
                String id = (String) user.get("id");
                String nome = (String) user.get("nome");
                System.out.println("- ID: " + id + ", Nome: " + nome);
            }
        } else {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if ("401".equals(status)) resetSession();
        }
    }

    private static void listarMinhasReviews(BufferedReader in) throws IOException {
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");

        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);

            List<Map<String, String>> reviewsList = (List<Map<String, String>>) response.get("reviews");

            if (reviewsList == null || reviewsList.isEmpty()) {
                System.out.println("\n[AVISO] Você ainda não fez nenhuma review.");
                return;
            }

            System.out.println("\n--- MINHAS REVIEWS ---");
            for (Map<String, String> r : reviewsList) {
                System.out.println("----------------------------------------");
                System.out.println("ID Review: " + r.get("id") + " | Filme ID: " + r.get("id_filme"));
                System.out.println("Data: " + r.get("data") + (Boolean.parseBoolean(r.get("editado")) ? " (Editado)" : ""));
                System.out.println("Título: " + r.get("titulo"));
                System.out.println("Nota: " + r.get("nota") + "/5");
                System.out.println("Comentário: " + r.get("descricao"));
            }
            System.out.println("----------------------------------------");
        } else {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if ("401".equals(status)) resetSession();
        }
    }

    private static void tratarRespostaDados(BufferedReader in) throws IOException {
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");
    
        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            
            if (response.containsKey("usuario")) {
                System.out.println("Usuario: " + response.get("usuario"));
            } else if (response.containsKey("dados")){
                    System.out.println("Dados: " + response.get("dados"));
            }
        } else {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if ("401".equals(status)) resetSession();
        }
    }

    private static void resetSession() {
        currentToken = null;
        currentUserRole = null;
    }

    private static void buscarFilmeId(BufferedReader in) throws IOException {
        Map<String, Object> response = gson.fromJson(in.readLine(), new TypeToken<Map<String, Object>>(){}.getType());
        String status = (String) response.get("status");
        String mensagem = (String) response.get("mensagem");

        if ("200".equals(status)) {
            System.out.println(ANSI_GREEN + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            
            Map<String, Object> f = (Map<String, Object>) response.get("filme");
            List<Map<String, String>> reviews = (List<Map<String, String>>) response.get("reviews");

            System.out.println("\n========================================");
            System.out.printf("FILME: %s (%s)\n", f.get("titulo"), f.get("ano"));
            System.out.println("========================================");
            System.out.printf("Diretor: %s\n", f.get("diretor"));
            System.out.printf("Nota Média: %s\n", f.get("nota"));
            System.out.printf("Qtd. Avaliações: %s\n", f.get("qtd_avaliacoes"));
            
            Object generosObj = f.get("genero");
            if (generosObj instanceof List) {
                System.out.println("Gêneros: " + generosObj.toString().replace("[", "").replace("]", ""));
            }
            System.out.println("Sinopse: " + f.get("sinopse"));
            System.out.println("----------------------------------------");
            
            if (reviews != null && !reviews.isEmpty()) {
                System.out.println("REVIEWS DOS USUÁRIOS:");
                for (Map<String, String> r : reviews) {
                    System.out.printf("[%s] %s deu nota %s/5\n", r.get("data"), r.get("nome_usuario"), r.get("nota"));
                    System.out.println("  \"" + r.get("titulo") + "\"");
                    System.out.println("  " + r.get("descricao"));
                    System.out.println("");
                }
            } else {
                System.out.println("Este filme ainda não possui reviews.");
            }
            System.out.println("========================================");

        } else {
            System.out.println(ANSI_RED + "Servidor: [" + status + "] " + mensagem + ANSI_RESET);
            if ("401".equals(status)) resetSession();
        }
    }
}