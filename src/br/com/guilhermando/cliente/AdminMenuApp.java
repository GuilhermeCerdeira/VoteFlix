package br.com.guilhermando.cliente;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

public class AdminMenuApp extends Application {

    private static final Gson gson = new Gson();
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final String currentUserRole;
    private final String token;

    public AdminMenuApp(Socket socket, PrintWriter out, BufferedReader in, String role, String token) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.currentUserRole = role;
        this.token = token;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFlix - Admin");

        Label lblTitulo = new Label("Painel Admin");
        lblTitulo.getStyleClass().add("titulo-login");
        lblTitulo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 28px;");

        Label lblSub = new Label("Gerenciamento do Sistema");
        lblSub.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label lblSecaoUsers = new Label("USUÁRIOS");
        lblSecaoUsers.getStyleClass().add("subtitulo");

        Button btnListarUsers = new Button("👥  Listar Todos os Usuários");
        btnListarUsers.getStyleClass().add("botao-menu");
        btnListarUsers.setMaxWidth(Double.MAX_VALUE);
        btnListarUsers.setOnAction(e -> listarUsuarios());

        Button btnEditarUser = new Button("👌  Editar Dados de Usuário");
        btnEditarUser.getStyleClass().add("botao-menu");
        btnEditarUser.setMaxWidth(Double.MAX_VALUE);
        btnEditarUser.setOnAction(e -> editarUsuario());

        Button btnExcluirUser = new Button("Excluir Usuário");
        btnExcluirUser.getStyleClass().add("botao-perigo");
        btnExcluirUser.setMaxWidth(Double.MAX_VALUE);
        btnExcluirUser.setOnAction(e -> excluirUsuario());

        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 10, 0));

        Label lblSecaoContent = new Label("CONTEÚDO");
        lblSecaoContent.getStyleClass().add("subtitulo");

        Button btnCriarFilme = new Button("➕  Cadastrar Novo Filme");
        btnCriarFilme.getStyleClass().add("botao-menu");
        btnCriarFilme.setMaxWidth(Double.MAX_VALUE);
        btnCriarFilme.setOnAction(e -> abrirFormularioCriarFilme()); 

        Button btnEditarFilme = new Button("🍿 Editar Filme");
        btnEditarFilme.getStyleClass().add("botao-menu");
        btnEditarFilme.setMaxWidth(Double.MAX_VALUE);
        btnEditarFilme.setOnAction(e -> abrirFormularioEditarFilme()); 

        Button btnExcluirFilme = new Button("Excluir Filme");
        btnExcluirFilme.getStyleClass().add("botao-perigo");
        btnExcluirFilme.setMaxWidth(Double.MAX_VALUE);
        btnExcluirFilme.setOnAction(e -> excluirFilme());

        Button btnExcluirReview = new Button("Excluir Review");
        btnExcluirReview.getStyleClass().add("botao-perigo");
        btnExcluirReview.setMaxWidth(Double.MAX_VALUE);
        btnExcluirReview.setOnAction(e -> excluirReview());

        Label lblEspaco = new Label(""); 
        
        Button btnVoltar = new Button("⬅ Voltar ao Menu Principal");
        btnVoltar.getStyleClass().add("botao-secundario");
        btnVoltar.setOnAction(e -> {
            try {
                new UserMenuApp(socket, out, in, currentUserRole, token).start(primaryStage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        VBox cartaoAdmin = new VBox(10);
        cartaoAdmin.setPadding(new Insets(30));
        cartaoAdmin.setAlignment(Pos.TOP_LEFT);
        cartaoAdmin.getStyleClass().add("login-card");
        cartaoAdmin.setPrefWidth(400);

        cartaoAdmin.getChildren().addAll(
            lblTitulo, 
            lblSub, 
            new Label(""),
            
            lblSecaoUsers,
            btnListarUsers,
            btnEditarUser,
            btnExcluirUser,
            
            sep, 
            
            lblSecaoContent,
            btnCriarFilme,  
            btnEditarFilme,  
            btnExcluirFilme, 
            btnExcluirReview,
            
            lblEspaco,
            btnVoltar
        );

        javafx.scene.control.TextArea txtConsole = GuiLogger.createConsole();

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("fundo-login");
        root.setPadding(new Insets(20));
        root.getChildren().addAll(cartaoAdmin, txtConsole);
        VBox.setVgrow(cartaoAdmin, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 750);
        String cssPath = "style.css";
        java.net.URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void listarUsuarios() {
        new Thread(() -> {
            try {
                Map<String, Object> response = ClientService.adminListarUsuarios(out, in, token);
                Platform.runLater(() -> {
                    if (response != null && "200".equals(response.get("status"))) {
                        List<Map<String, Object>> usersList = (List<Map<String, Object>>) response.get("usuarios");
                        StringBuilder sb = new StringBuilder();
                        
                        for (Map<String, Object> u : usersList) {
                            Object idObj = u.get("id");
                            String id = (idObj instanceof Double) ? String.valueOf(((Double)idObj).intValue()) : idObj.toString();
                            sb.append("ID: " + id + " | Usuário: " + u.get("nome") + "\n");
                        }
                        mostrarTextoLongo("Lista de Usuários", sb.toString());
                    } else {
                        mostrarErro(response);
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    public static Map<String, Object> adminExcluirReview(PrintWriter out, BufferedReader in, String token, String idReview) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("operacao", "ADMIN_EXCLUIR_REVIEW");
        request.put("id_review", idReview);
        request.put("token", token);
        
        String jsonRequest = gson.toJson(request);
        GuiLogger.log("[SERVICE] Enviando:", jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        if (jsonResponse == null) return null;
        
        GuiLogger.log("[SERVICE] Recebeu:", jsonResponse);
        return gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>(){}.getType());
    }

    private void editarUsuario() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Editar Usuário");
        dialog.setHeaderText("Alterar a senha de um usuário");

        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNome = new TextField();
        txtNome.setPromptText("ID do usuário");
        TextField txtSenha = new TextField();
        txtSenha.setPromptText("Nova Senha");

        grid.add(new Label("ID Alvo:"), 0, 0);
        grid.add(txtNome, 1, 0);
        grid.add(new Label("Nova Senha:"), 0, 1);
        grid.add(txtSenha, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSalvar) {
                return new Pair<>(txtNome.getText(), txtSenha.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dados -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminEditarUsuario(out, in, token, dados.getKey(), dados.getValue());
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Senha do usuário atualizada.");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }

    private void excluirUsuario() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Excluir Usuário");
        dialog.setHeaderText("EXCLUIR USUÁRIO DO SISTEMA");
        dialog.setContentText("Digite o ID do usuário para excluir:");

        dialog.showAndWait().ifPresent(nome -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminDeletarUsuario(out, in, token, nome);
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Usuário " + nome + " deletado.");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }

    private void excluirFilme() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Excluir Filme");
        dialog.setHeaderText("Atenção: Isso apagará todas as reviews também.");
        dialog.setContentText("Digite o ID do filme para excluir:");

        dialog.showAndWait().ifPresent(idFilme -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminExcluirFilme(out, in, token, idFilme);
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Filme ID " + idFilme + " deletado.");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }

    private void mostrarErro(Map<String, Object> response) {
        String msg = response != null ? (String) response.get("mensagem") : "Erro de conexão";
        mostrarAlerta(AlertType.ERROR, "Erro", msg);
    }

    private void mostrarAlerta(AlertType tipo, String titulo, String msg) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarTextoLongo(String titulo, String conteudo) {
        TextArea textArea = new TextArea(conteudo);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(400, 300);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void excluirReview() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Excluir Review");
        dialog.setHeaderText("Moderação de Conteúdo");
        dialog.setContentText("Digite o ID da Review para excluir:");

        dialog.showAndWait().ifPresent(idReview -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminExcluirReview(out, in, token, idReview);
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Review ID " + idReview + " deletada.");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                }
            }).start();
        });
    }

    private void abrirFormularioCriarFilme() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Novo Filme");
        dialog.setHeaderText("Cadastrar filme no catálogo");

        ButtonType btnSalvar = new ButtonType("Cadastrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Ex: O Senhor dos Anéis");
        
        TextField txtDiretor = new TextField();
        txtDiretor.setPromptText("Ex: Peter Jackson");
        
        TextField txtAno = new TextField();
        txtAno.setPromptText("Ex: 2001");
        
        TextField txtGeneros = new TextField();
        txtGeneros.setPromptText("Ex: Ação,Fantasia,Aventura");
        
        TextArea txtSinopse = new TextArea();
        txtSinopse.setPromptText("Sinopse do filme...");
        txtSinopse.setPrefHeight(100);
        txtSinopse.setWrapText(true);

        grid.add(new Label("Título:"), 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Diretor:"), 0, 1);
        grid.add(txtDiretor, 1, 1);
        grid.add(new Label("Ano:"), 0, 2);
        grid.add(txtAno, 1, 2);
        grid.add(new Label("Gêneros:"), 0, 3);
        grid.add(txtGeneros, 1, 3);
        grid.add(new Label("Sinopse:"), 0, 4);
        grid.add(txtSinopse, 1, 4);
        
        Label lblDica = new Label("(Separe gêneros por vírgula)");
        lblDica.setStyle("-fx-font-size: 10px; -fx-text-fill: grey;");
        grid.add(lblDica, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSalvar) {
                Map<String, String> dados = new HashMap<>();
                dados.put("titulo", txtTitulo.getText());
                dados.put("diretor", txtDiretor.getText());
                dados.put("ano", txtAno.getText());
                dados.put("genero", txtGeneros.getText());
                dados.put("sinopse", txtSinopse.getText());
                return dados;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();

        result.ifPresent(dados -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminCriarFilme(
                        out, in, token,
                        dados.get("titulo"),
                        dados.get("diretor"),
                        dados.get("ano"),
                        dados.get("genero"),
                        dados.get("sinopse")
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && ("200".equals(response.get("status")) || "201".equals(response.get("status")))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Filme cadastrado com sucesso!");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }

    private void abrirFormularioEditarFilme() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Editar Filme");
        dialog.setHeaderText("Atualizar dados do catálogo");

        ButtonType btnSalvar = new ButtonType("Salvar Alterações", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtId = new TextField(); txtId.setPromptText("ID do Filme");
        TextField txtTitulo = new TextField(); txtTitulo.setPromptText("Novo Título (opcional)");
        TextField txtDiretor = new TextField(); txtDiretor.setPromptText("Novo Diretor (opcional)");
        TextField txtAno = new TextField(); txtAno.setPromptText("Novo Ano (opcional)");
        TextField txtGeneros = new TextField(); txtGeneros.setPromptText("Novos Gêneros (opcional)");
        TextArea txtSinopse = new TextArea(); txtSinopse.setPromptText("Nova Sinopse (opcional)"); txtSinopse.setPrefHeight(80);

        grid.add(new Label("ID do Filme (Obrigatório):"), 0, 0); grid.add(txtId, 1, 0);
        grid.add(new Label("Novo Título:"), 0, 1); grid.add(txtTitulo, 1, 1);
        grid.add(new Label("Novo Diretor:"), 0, 2); grid.add(txtDiretor, 1, 2);
        grid.add(new Label("Novo Ano:"), 0, 3); grid.add(txtAno, 1, 3);
        grid.add(new Label("Novos Gêneros:"), 0, 4); grid.add(txtGeneros, 1, 4);
        grid.add(new Label("Nova Sinopse:"), 0, 5); grid.add(txtSinopse, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSalvar) {
                Map<String, String> dados = new HashMap<>();
                dados.put("id", txtId.getText());
                dados.put("titulo", txtTitulo.getText());
                dados.put("diretor", txtDiretor.getText());
                dados.put("ano", txtAno.getText());
                dados.put("genero", txtGeneros.getText());
                dados.put("sinopse", txtSinopse.getText());
                return dados;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dados -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.adminEditarFilme(
                        out, in, token, 
                        dados.get("id"), dados.get("titulo"), dados.get("diretor"), 
                        dados.get("ano"), dados.get("genero"), dados.get("sinopse")
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Filme atualizado!");
                        } else {
                            mostrarErro(response);
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }
}