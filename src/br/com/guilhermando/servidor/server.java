package br.com.guilhermando.servidor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.auth0.jwt.interfaces.DecodedJWT;
import br.com.guilhermando.db.DB;
import br.com.guilhermando.protocolo.ErroProtocolo;
import br.com.guilhermando.protocolo.Generos;

public class server extends Application {

    private static ObservableList<String> usuariosOnline = FXCollections.observableArrayList();
    
    private ServerSocket echoServer = null;

    public static void registrarLogin(String usuario) {
        Platform.runLater(() -> {
            if (!usuariosOnline.contains(usuario)) {
                usuariosOnline.add(usuario);
            }
        });
    }

    public static void registrarLogout(String usuario) {
        Platform.runLater(() -> {
            usuariosOnline.remove(usuario);
        });
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFlix - Painel do Servidor");

        Label lblTitulo = new Label("SERVIDOR");
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        Label lblPorta = new Label("Porta:");
        lblPorta.setStyle("-fx-text-fill: white;");

        TextField txtPorta = new TextField("2000");
        txtPorta.setPrefWidth(80);
        
        Button btnIniciar = new Button("Iniciar Servidor");
        btnIniciar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
        
        Label lblStatus = new Label("Status: Parado");
        lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 

        HBox boxConfig = new HBox(10);
        boxConfig.setAlignment(Pos.CENTER);
        boxConfig.getChildren().addAll(lblPorta, txtPorta, btnIniciar);

        Label lblLista = new Label("Usuários Conectados:");
        lblLista.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        ListView<String> listView = new ListView<>(usuariosOnline);
        listView.setPrefHeight(300);
        listView.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: white;");

        btnIniciar.setOnAction(e -> {
            String portaStr = txtPorta.getText();
            try {
                int porta = Integer.parseInt(portaStr);
                
                new Thread(() -> rodarServidorSocket(porta, lblStatus)).start();
                
                btnIniciar.setDisable(true);
                txtPorta.setDisable(true);
                
            } catch (NumberFormatException ex) {
                lblStatus.setText("Erro: Porta inválida");
            }
        });

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2c3e50;");
        layout.getChildren().addAll(lblTitulo, boxConfig, lblStatus, lblLista, listView);

        Scene scene = new Scene(layout, 400, 550);
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(e -> {
            try {
                if (echoServer != null && !echoServer.isClosed()) echoServer.close();
            } catch (IOException ex) {}
            System.exit(0);
        });
        
        primaryStage.show();
    }

    public void rodarServidorSocket(int porta, Label lblStatus) {
        DB.initializeDatabase();

        try {
            echoServer = new ServerSocket(porta);
            System.out.println("Servidor rodando na porta " + porta);
            
            Platform.runLater(() -> {
                lblStatus.setText("Status: Rodando na porta " + porta);
                lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> lblStatus.setText("Erro ao iniciar: " + e.getMessage()));
            return;
        }

        while (!echoServer.isClosed()) {
            try {
                Socket clientSocket = echoServer.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            } catch (IOException e) {
                if (!echoServer.isClosed()) e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private static final Gson gson = new Gson();
        private String usuarioLogadoNestaThread = null;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                try (
                    BufferedReader is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                    PrintWriter os = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)
                ) {
                    String jsonRequest;
                    while ((jsonRequest = is.readLine()) != null) {
                        System.out.println("[JSON RECEBIDO]: " + jsonRequest);
                        Map<String, Object> request = gson.fromJson(jsonRequest, new TypeToken<Map<String, Object>>(){}.getType());
                        String operacao = (String) request.get("operacao");
                        
                        if (operacao == null) { sendError(os, ErroProtocolo.OPERACAO_INVALIDA); continue; }

                        if ("LOGIN".equals(operacao)) {
                            String user = (String) request.get("usuario");
                            String pass = (String) request.get("senha");
                            if (user == null || pass == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); continue; }
                            Map<String, String> userData = DB.validateLogin(user, pass);
                            if (userData != null) {
                                String token = JwtManager.createToken(userData);
                                sendSuccessLogin(os, token);
                                usuarioLogadoNestaThread = user;
                                server.registrarLogin(user);
                            } else { sendError(os, ErroProtocolo.SEM_PERMISSAO); }
                            continue;
                        }

                        if ("CRIAR_USUARIO".equals(operacao)) {
                             Map<String, String> dU = null; try { dU = (Map<String, String>) request.get("usuario"); } catch (Exception e) {}
                             if (dU == null || dU.get("nome") == null || dU.get("senha") == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); continue; }
                             if (DB.userExists(dU.get("nome"))) { sendError(os, ErroProtocolo.RECURSO_JA_EXISTE); } else { DB.addUser(dU.get("nome"), dU.get("senha")); sendSuccessCreated(os); }
                             continue;
                        }

                        String token = (String) request.get("token");
                        if (token == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); continue; }
                        DecodedJWT claims = JwtManager.validateTokenAndGetClaims(token);
                        if (claims == null) { sendError(os, ErroProtocolo.TOKEN_INVALIDO); continue; }
                        String usuarioDoToken = claims.getClaim("usuario").asString();
                        if (!DB.userExists(usuarioDoToken)) { sendError(os, ErroProtocolo.TOKEN_INVALIDO); continue; }
                        String funcaoDoToken = claims.getClaim("funcao").asString();

                        switch (operacao) {
                            case "LOGOUT": 
                                sendSuccess(os);
                                server.registrarLogout(usuarioLogadoNestaThread);
                                usuarioLogadoNestaThread = null;
                                break;

                            case "DELETE_ACCOUNT": 
                            case "EXCLUIR_PROPRIO_USUARIO":
                                if(DB.deleteUser(usuarioDoToken)){ 
                                    sendSuccess(os); 
                                    server.registrarLogout(usuarioLogadoNestaThread);
                                    usuarioLogadoNestaThread = null;
                                } else { sendError(os, ErroProtocolo.FALHA_INTERNA); }
                                break;

                            case "LISTAR_PROPRIO_USUARIO": sendSuccessListarProprioUsuario(os, usuarioDoToken); break;
                            case "LISTAR_USUARIOS": sendSuccessListarUsuarios(os, DB.getAllUsers()); break;
                            case "EDITAR_PROPRIO_USUARIO":
                                Map<String, String> uObj = null; try { uObj = (Map<String, String>) request.get("usuario"); } catch (Exception e) {}
                                if (uObj == null || uObj.get("senha") == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                if ("admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                if(DB.updatePassword(usuarioDoToken, uObj.get("senha"))){ sendSuccess(os); } else { sendError(os, ErroProtocolo.FALHA_INTERNA); }
                                break;
                            case "CRIAR_FILME":{
                                if (!"admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                Map<String, Object> fReq = (Map<String, Object>) request.get("filme");
                                if (fReq == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                String tit = (String) fReq.get("titulo"); String dir = (String) fReq.get("diretor"); String ano = (String) fReq.get("ano");
                                List<String> gList; try { gList = (List<String>) fReq.get("genero"); } catch (Exception e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                if (tit == null || ano == null || dir == null || gList == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                String[] gArr = gList.toArray(new String[0]);
                                if (!Generos.validarListaGeneros(gArr)) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                Map<String, String> fDb = new HashMap<>(); fDb.put("titulo", tit); fDb.put("ano", ano); fDb.put("diretor", dir); fDb.put("genero", String.join(",", gList)); fDb.put("sinopse", (String)fReq.get("sinopse"));
                                if (DB.addFilme(fDb)) { sendSuccessCreated(os); } else { sendError(os, ErroProtocolo.RECURSO_JA_EXISTE); }
                                break;
                            }
                            case "LISTAR_FILMES":{
                                String fJson = DB.getAllFilmes();
                                List<Map<String, Object>> fList = gson.fromJson(fJson, new TypeToken<List<Map<String, Object>>>(){}.getType());
                                for (Map<String, Object> f : fList) {
                                    Object gRaw = f.get("genero");
                                    if (gRaw instanceof String) { f.put("genero", java.util.Arrays.asList(((String)gRaw).split(","))); }
                                }
                                sendSuccessListarFilmes(os, fList);
                                break;
                            }
                            case "BUSCAR_FILME_ID": {
                                String idStr = (String) request.get("id_filme");
                                if (idStr == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                int idF; try { idF = Integer.parseInt(idStr); } catch (NumberFormatException e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                Map<String, Object> f = DB.getFilmeById(idF);
                                if (f != null) {
                                    Object gRaw = f.remove("genero"); if (gRaw instanceof String) { f.put("genero", java.util.Arrays.asList(((String)gRaw).split(","))); }
                                    sendSuccessBuscarFilme(os, f, DB.getReviewsByMovieId(idF));
                                } else { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); }
                                break;
                            }
                            case "EDITAR_FILME":{
                                if (!"admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; } 
                                Map<String, Object> filmeRequest; try { filmeRequest = (Map<String, Object>) request.get("filme"); } catch (Exception e) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                if (filmeRequest == null || filmeRequest.get("id") == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                int idFilme; try { idFilme = Integer.parseInt((String) filmeRequest.get("id")); } catch (Exception e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                Map<String, Object> filmeAtual = DB.getFilmeById(idFilme);
                                if (filmeAtual == null) { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); break; }
                                if (filmeRequest.containsKey("titulo")) filmeAtual.put("titulo", (String) filmeRequest.get("titulo"));
                                if (filmeRequest.containsKey("ano")) filmeAtual.put("ano", (String) filmeRequest.get("ano"));
                                if (filmeRequest.containsKey("diretor")) filmeAtual.put("diretor", (String) filmeRequest.get("diretor"));
                                if (filmeRequest.containsKey("sinopse")) filmeAtual.put("sinopse", (String) filmeRequest.get("sinopse"));
                                if (filmeRequest.containsKey("genero")) {
                                    List<String> generosList = (List<String>) filmeRequest.get("genero");
                                    String[] generosArray = generosList.toArray(new String[0]);
                                    if (!Generos.validarListaGeneros(generosArray)) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                    filmeAtual.put("genero", String.join(",", generosList));
                                }
                                if (DB.updateFilme(filmeAtual)) { sendSuccess(os); } else { sendError(os, ErroProtocolo.FALHA_INTERNA); }
                                break;
                            }
                            case "EXCLUIR_FILME":{
                                if (!"admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                String idFilmeStr = (String) request.get("id"); 
                                if (idFilmeStr == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                try { if (DB.deleteFilme(Integer.parseInt(idFilmeStr))) sendSuccess(os); else sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); } catch (NumberFormatException e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); }
                                break;
                            }
                            case "CRIAR_REVIEW": {
                                if ("admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                
                                Map<String, Object> reviewRequest; 
                                try { reviewRequest = (Map<String, Object>) request.get("review"); } catch (Exception e) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                
                                if (reviewRequest == null || reviewRequest.get("id_filme") == null || reviewRequest.get("nota") == null || reviewRequest.get("titulo") == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                
                                int idFilme; int nota; String titulo = (String) reviewRequest.get("titulo"); String comentario = null;
                                try {
                                    idFilme = Integer.parseInt((String) reviewRequest.get("id_filme"));
                                    nota = Integer.parseInt((String) reviewRequest.get("nota"));
                                    if (reviewRequest.containsKey("descricao")) { comentario = (String) reviewRequest.get("descricao"); }
                                } catch (Exception e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                
                                if (nota < 1 || nota > 5) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
                                if (!DB.filmeExistsById(idFilme)) { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); break; }
                                
                                Integer idUsuario = DB.getUserIdByUsername(usuarioDoToken);
                                if (idUsuario == null) { sendError(os, ErroProtocolo.FALHA_INTERNA); break; }
                                
                                if (DB.addReview(idFilme, idUsuario, titulo, nota, comentario)) { 
                                    sendSuccessCreated(os); 
                                } else { 
                                    sendError(os, ErroProtocolo.FALHA_INTERNA); 
                                }
                                break;
                            }
                            case "EDITAR_REVIEW": {
                                Map<String, Object> reviewRequest; 
                                try { 
                                    reviewRequest = (Map<String, Object>) request.get("review"); 
                                } catch (Exception e) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
    
                                if (reviewRequest == null || reviewRequest.get("id") == null || reviewRequest.get("nota") == null) { 
                                    sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; 
                                }
    
                                int idReview; 
                                int nota; 
                                String comentario = null;
                                String titulo = null; 
    
                                try {
                                    idReview = Integer.parseInt((String) reviewRequest.get("id"));
                                    nota = Integer.parseInt((String) reviewRequest.get("nota"));
                                    
                                    if (reviewRequest.containsKey("descricao")) { 
                                        comentario = (String) reviewRequest.get("descricao"); 
                                    }
                                    
                                    if (reviewRequest.containsKey("titulo")) {
                                        titulo = (String) reviewRequest.get("titulo");
                                    }
                                    
                                } catch (Exception e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
    
                                if (nota < 1 || nota > 5) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); break; }
    
                                Integer idUsuario = DB.getUserIdByUsername(usuarioDoToken);
                                if (idUsuario == null) { sendError(os, ErroProtocolo.FALHA_INTERNA); break; }
    
                                if (!"admin".equals(funcaoDoToken) && !DB.isReviewOwner(idReview, idUsuario)) { 
                                    sendError(os, ErroProtocolo.SEM_PERMISSAO); break; 
                                }
    
                                if (DB.updateReviewById(idReview, titulo, nota, comentario)) { 
                                    sendSuccess(os); 
                                } else { 
                                    sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); 
                                }
                                break;
                            }
                            case "LISTAR_REVIEWS_USUARIO":
                                Integer idUserBusca = DB.getUserIdByUsername(usuarioDoToken);
                                if (idUserBusca == null) { sendError(os, ErroProtocolo.FALHA_INTERNA); } else { List<Map<String, String>> reviewsList = DB.getReviewsByUserId(idUserBusca); sendSuccessListarReviews(os, reviewsList); }
                                break;
                            case "EXCLUIR_REVIEW": {
                                String idReviewStr = (String) request.get("id"); 
                                if (idReviewStr == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                try {
                                    int idReview = Integer.parseInt(idReviewStr);
                                    Integer idUsuario = DB.getUserIdByUsername(usuarioDoToken);
                                    if (idUsuario == null) { sendError(os, ErroProtocolo.FALHA_INTERNA); break; }
                                    boolean ehAdmin = "admin".equals(funcaoDoToken);
                                    boolean ehDono  = DB.isReviewOwner(idReview, idUsuario);
                                    if (!ehAdmin && !ehDono) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                    if (DB.deleteReview(idReview)) { sendSuccess(os); } else { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); }
                                } catch (NumberFormatException e) { sendError(os, ErroProtocolo.CAMPOS_INVALIDOS); }
                                break;
                            }
                            case "ADMIN_EXCLUIR_USUARIO":
                                if (!"admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                String idStr = (String) request.get("usuario_alvo"); 
                                if (idStr == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                if (idStr.equalsIgnoreCase("admin")) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; } 
                                if (!DB.userExists(idStr)) { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); break; }
                                if(DB.deleteUser(idStr)){ sendSuccess(os); } else { sendError(os, ErroProtocolo.FALHA_INTERNA); }
                                break;

                            case "ADMIN_EDITAR_USUARIO":
                                if (!"admin".equals(funcaoDoToken)) { sendError(os, ErroProtocolo.SEM_PERMISSAO); break; }
                                String uAlvoEd = (String) request.get("usuario_alvo");
                                String nSenha = (String) request.get("nova_senha");
                                if (uAlvoEd == null || nSenha == null) { sendError(os, ErroProtocolo.CHAVES_FALTANTES); break; }
                                if (!DB.userExists(uAlvoEd)) { sendError(os, ErroProtocolo.RECURSO_INEXISTENTE); break; }
                                if(DB.updatePassword(uAlvoEd, nSenha)){ sendSuccess(os); } else { sendError(os, ErroProtocolo.FALHA_INTERNA); }
                                break;

                            default: sendError(os, ErroProtocolo.OPERACAO_INVALIDA); break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Cliente desconectado.");
            } finally {
                if (usuarioLogadoNestaThread != null) { server.registrarLogout(usuarioLogadoNestaThread); }
                try { clientSocket.close(); } catch (IOException e) {}
            }
        }

        private static void sendError(PrintWriter os, ErroProtocolo erro) { Map<String, Object> r = new HashMap<>(); r.put("status", erro.getStatusCode()); r.put("mensagem", erro.getMensagem()); os.println(gson.toJson(r)); }
        private static void sendSuccess(PrintWriter os) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: Operacao realizada com sucesso"); os.println(gson.toJson(r)); }
        private static void sendSuccess(PrintWriter os, Object dados) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: Operacao realizada com sucesso"); r.put("dados", dados); os.println(gson.toJson(r)); }
        private static void sendSuccessListarUsuarios(PrintWriter os, Object usuarios) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: Operacao realizada com sucesso"); r.put("usuarios", usuarios); os.println(gson.toJson(r)); }
        private static void sendSuccessListarFilmes(PrintWriter os, Object filmes) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: Operacao realizada com sucesso"); r.put("filmes", filmes); os.println(gson.toJson(r)); }
        private static void sendSuccessLogin(PrintWriter os, String token) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: operacao realizada com sucesso"); r.put("token", token); os.println(gson.toJson(r)); }
        private static void sendSuccessCreated(PrintWriter os) { Map<String, Object> r = new HashMap<>(); r.put("status", "201"); r.put("mensagem", "Sucesso: Recurso cadastrado"); os.println(gson.toJson(r)); }
        private static void sendSuccessListarProprioUsuario(PrintWriter os, String u) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "OK"); r.put("usuario", u); os.println(gson.toJson(r)); }
        private static void sendSuccessListarReviews(PrintWriter os, Object reviews) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "Sucesso: Operação realizada com sucesso"); r.put("reviews", reviews); os.println(gson.toJson(r)); }
        private static void sendSuccessBuscarFilme(PrintWriter os, Map<String, Object> f, List<Map<String, String>> rev) { Map<String, Object> r = new HashMap<>(); r.put("status", "200"); r.put("mensagem", "OK"); r.put("filme", f); r.put("reviews", rev); os.println(gson.toJson(r)); }
    }
}