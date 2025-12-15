package br.com.guilhermando.cliente;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ClientService {
    private static final Gson gson = new Gson();

    public static Map<String, Object> enviarCadastro(PrintWriter out, BufferedReader in, String usuario, String senha) throws IOException {
        Map<String, String> dadosUsuario = new HashMap<>();
        dadosUsuario.put("nome", usuario);
        dadosUsuario.put("senha", senha);

        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "CRIAR_USUARIO");
        request.put("usuario", dadosUsuario);
        
        String jsonRequest = gson.toJson(request);
        
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest); 
        
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse); 
        
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> enviarLogin(PrintWriter out, BufferedReader in, String usuario, String senha) throws IOException {
        Map<String, String> request = new HashMap<>();
        request.put("operacao", "LOGIN");
        request.put("usuario", usuario);
        request.put("senha", senha);

        String jsonRequest = gson.toJson(request);

        GuiLogger.log("[SERVICE] Enviando:", jsonRequest); 
        
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> buscarMeusDados(PrintWriter out, BufferedReader in, String token) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "LISTAR_PROPRIO_USUARIO");
        request.put("token", token);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> alterarSenha(PrintWriter out, BufferedReader in, String token, String novaSenha) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EDITAR_PROPRIO_USUARIO");
        request.put("token", token);

        Map<String, String> usuarioObj = new HashMap<>();
        usuarioObj.put("senha", novaSenha);
        request.put("usuario", usuarioObj);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> deletarConta(PrintWriter out, BufferedReader in, String token) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
        request.put("token", token);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> listarFilmes(PrintWriter out, BufferedReader in, String token) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "LISTAR_FILMES");
        request.put("token", token);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> buscarDetalhesFilme(PrintWriter out, BufferedReader in, String token, String idFilme) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "BUSCAR_FILME_ID");
        request.put("id_filme", idFilme);
        request.put("token", token);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;

        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> listarMinhasReviews(PrintWriter out, BufferedReader in, String token) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "LISTAR_REVIEWS_USUARIO");
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> criarReview(PrintWriter out, BufferedReader in, String token, String idFilme, String titulo, String nota, String descricao) throws IOException {
        Map<String, Object> dadosReview = new HashMap<>();
        dadosReview.put("id_filme", idFilme);
        dadosReview.put("titulo", titulo);
        dadosReview.put("nota", nota);
        dadosReview.put("descricao", descricao);

        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "CRIAR_REVIEW");
        request.put("token", token);
        request.put("review", dadosReview); // Aninha o mapa

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> editarReview(PrintWriter out, BufferedReader in, String token, String idReview, String titulo, String nota, String descricao) throws IOException {
        Map<String, Object> dadosReview = new HashMap<>();
        dadosReview.put("id", idReview);
        if (!titulo.isEmpty()) dadosReview.put("titulo", titulo);
        if (!nota.isEmpty()) dadosReview.put("nota", nota);
        if (!descricao.isEmpty()) dadosReview.put("descricao", descricao);

        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EDITAR_REVIEW");
        request.put("token", token);
        request.put("review", dadosReview);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminListarUsuarios(PrintWriter out, BufferedReader in, String token) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "LISTAR_USUARIOS");
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> excluirReview(PrintWriter out, BufferedReader in, String token, String idReview) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EXCLUIR_REVIEW");
        
        request.put("id", idReview); // AGORA ESTÁ CERTO: Chave "id"
        
        request.put("token", token);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminDeletarUsuario(PrintWriter out, BufferedReader in, String token, String idAlvo) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "ADMIN_EXCLUIR_USUARIO");
        request.put("id", idAlvo); // Usa ID
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminEditarUsuario(PrintWriter out, BufferedReader in, String token, String idAlvo, String novaSenha) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "ADMIN_EDITAR_USUARIO");
        request.put("token", token);
        request.put("id", idAlvo); // Usa ID

        // Protocolo: Aninhado
        Map<String, String> usuarioObj = new HashMap<>();
        usuarioObj.put("senha", novaSenha);
        request.put("usuario", usuarioObj);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminExcluirFilme(PrintWriter out, BufferedReader in, String token, String idFilme) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EXCLUIR_FILME");
        request.put("id", idFilme);
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminEditarFilme(PrintWriter out, BufferedReader in, String token, 
            String id, String titulo, String diretor, String ano, String generosRaw, String sinopse) throws IOException {
        
        Map<String, Object> filmeAtualizado = new HashMap<>();
        filmeAtualizado.put("id", id);
        if (!titulo.isEmpty()) filmeAtualizado.put("titulo", titulo);
        if (!diretor.isEmpty()) filmeAtualizado.put("diretor", diretor);
        if (!ano.isEmpty()) filmeAtualizado.put("ano", ano);
        if (!sinopse.isEmpty()) filmeAtualizado.put("sinopse", sinopse);
        
        if (!generosRaw.isEmpty()) {
            List<String> generosList = java.util.Arrays.asList(generosRaw.split(","));
            filmeAtualizado.put("genero", generosList);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("token", token);
        request.put("operacao", "EDITAR_FILME");
        request.put("filme", filmeAtualizado);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminExcluirReview(PrintWriter out, BufferedReader in, String token, String idReview) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "EXCLUIR_REVIEW"); 
        request.put("id", idReview);
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public static Map<String, Object> adminCriarFilme(PrintWriter out, BufferedReader in, String token, 
            String titulo, String diretor, String ano, String generosRaw, String sinopse) throws IOException {
        
        Map<String, Object> filme = new HashMap<>();
        filme.put("titulo", titulo);
        filme.put("diretor", diretor);
        filme.put("ano", ano);
        filme.put("sinopse", sinopse);

        if (generosRaw != null && !generosRaw.isEmpty()) {
            java.util.List<String> generosList = java.util.Arrays.asList(generosRaw.split(","));
            generosList.replaceAll(String::trim);
            filme.put("genero", generosList);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "CRIAR_FILME");
        request.put("token", token);
        request.put("filme", filme);

        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }
}