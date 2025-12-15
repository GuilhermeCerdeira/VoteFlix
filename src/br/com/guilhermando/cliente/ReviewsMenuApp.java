package br.com.guilhermando.cliente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.control.Label; 
import javafx.scene.control.Button;
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
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReviewsMenuApp extends Application {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final String currentUserRole;
    private final String token;

    public ReviewsMenuApp(Socket socket, PrintWriter out, BufferedReader in, String role, String token) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.currentUserRole = role;
        this.token = token;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFlix - Reviews");

        Label lblTitulo = new Label("Minhas Avaliações");
        lblTitulo.getStyleClass().add("titulo-login");
        lblTitulo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 24px; -fx-font-weight: bold;");

        Button btnCriar = new Button("✍ Escrever nova Review");
        btnCriar.getStyleClass().add("botao-menu");
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        btnCriar.setOnAction(e -> abrirFormularioCriacao());

        Button btnEditar = new Button("✏ Editar uma Review");
        btnEditar.getStyleClass().add("botao-menu");
        btnEditar.setMaxWidth(Double.MAX_VALUE);
        btnEditar.setOnAction(e -> abrirFormularioEdicao());

        Button btnListar = new Button("📋 Ver minhas Reviews");
        btnListar.getStyleClass().add("botao-menu");
        btnListar.setMaxWidth(Double.MAX_VALUE);
        btnListar.setOnAction(e -> listarReviews());

        Button btnExcluir = new Button("Excluir uma Review");
        btnExcluir.getStyleClass().add("botao-perigo"); 
        btnExcluir.setMaxWidth(Double.MAX_VALUE);
        btnExcluir.setOnAction(e -> excluirReview());

        Button btnVoltar = new Button("⬅ Voltar ao Menu Principal");
        btnVoltar.getStyleClass().add("botao-secundario");
        btnVoltar.setOnAction(e -> {
            try {
                new UserMenuApp(socket, out, in, currentUserRole, token).start(primaryStage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        VBox cartao = new VBox(15);
        cartao.setPadding(new Insets(30));
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().add("login-card");
        cartao.setMaxWidth(400);
        cartao.getChildren().addAll(
            lblTitulo, 
            new Label(""), 
            btnListar, 
            btnCriar, 
            btnEditar, 
            btnExcluir,
            new Label(""), 
            btnVoltar
        );

        TextArea txtConsole = GuiLogger.createConsole();

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("fundo-login");
        root.setPadding(new Insets(20));
        root.getChildren().addAll(cartao, txtConsole);
        VBox.setVgrow(cartao, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 700);
        
        String cssPath = "style.css";
        java.net.URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void listarReviews() {
        new Thread(() -> {
            try {
                Map<String, Object> response = ClientService.listarMinhasReviews(out, in, token);
                Platform.runLater(() -> {
                    if (response != null && "200".equals(response.get("status"))) {
                        List<Map<String, Object>> reviews = (List<Map<String, Object>>) response.get("reviews");
                        
                        if (reviews == null || reviews.isEmpty()) {
                            mostrarAlerta(AlertType.INFORMATION, "Aviso", "Você ainda não fez nenhuma review.");
                            return;
                        }
                        
                        StringBuilder sb = new StringBuilder();
                        for (Map<String, Object> r : reviews) {
                            sb.append("ID Review: " + r.get("id") + " | Filme ID: " + r.get("id_filme") + "\n");
                            sb.append("Título: " + r.get("titulo") + "\n");
                            sb.append("Nota: " + r.get("nota") + "/5\n");
                            sb.append("Comentário: " + r.get("descricao") + "\n");
                            sb.append("Data: " + r.get("data") + "\n");
                            sb.append("----------------------------------------\n");
                        }
                        mostrarTextoLongo("Minhas Reviews", sb.toString());
                    } else {
                        mostrarAlerta(AlertType.ERROR, "Erro", (String) response.get("mensagem"));
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void abrirFormularioCriacao() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Nova Review");
        dialog.setHeaderText("Avalie um filme");

        ButtonType btnConfirmar = new ButtonType("Enviar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtIdFilme = new TextField();
        txtIdFilme.setPromptText("ID do Filme");
        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título da Avaliação");
        TextField txtNota = new TextField();
        txtNota.setPromptText("1 a 5");
        TextArea txtDescricao = new TextArea();
        txtDescricao.setPromptText("Escreva sua resenha");
        txtDescricao.setPrefHeight(100);

        grid.add(new Label("ID Filme:"), 0, 0);
        grid.add(txtIdFilme, 1, 0);
        grid.add(new Label("Título:"), 0, 1);
        grid.add(txtTitulo, 1, 1);
        grid.add(new Label("Nota (1-5):"), 0, 2);
        grid.add(txtNota, 1, 2);
        grid.add(new Label("Comentário:"), 0, 3);
        grid.add(txtDescricao, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                Map<String, String> res = new HashMap<>();
                res.put("id_filme", txtIdFilme.getText());
                res.put("titulo", txtTitulo.getText());
                res.put("nota", txtNota.getText());
                res.put("descricao", txtDescricao.getText());
                return res;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();

        result.ifPresent(dados -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.criarReview(out, in, token, 
                        dados.get("id_filme"), dados.get("titulo"), dados.get("nota"), dados.get("descricao"));
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Review criada com sucesso!");
                        } else {
                            mostrarAlerta(AlertType.ERROR, "Erro", (String) response.get("mensagem"));
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }

    private void abrirFormularioEdicao() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Editar Review");
        dialog.setHeaderText("Alterar avaliação existente");

        ButtonType btnConfirmar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtIdReview = new TextField();
        txtIdReview.setPromptText("ID da Review");
        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Novo Título");
        TextField txtNota = new TextField();
        txtNota.setPromptText("Nova Nota (1-5)");
        TextArea txtDescricao = new TextArea();
        txtDescricao.setPromptText("Novo Comentário");
        txtDescricao.setPrefHeight(100);

        grid.add(new Label("ID Review:"), 0, 0);
        grid.add(txtIdReview, 1, 0);
        grid.add(new Label("Novo Título:"), 0, 1);
        grid.add(txtTitulo, 1, 1);
        grid.add(new Label("Nova Nota:"), 0, 2);
        grid.add(txtNota, 1, 2);
        grid.add(new Label("Novo Texto:"), 0, 3);
        grid.add(txtDescricao, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                Map<String, String> res = new HashMap<>();
                res.put("id", txtIdReview.getText());
                res.put("titulo", txtTitulo.getText());
                res.put("nota", txtNota.getText());
                res.put("descricao", txtDescricao.getText());
                return res;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();

        result.ifPresent(dados -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.editarReview(out, in, token, 
                        dados.get("id"), dados.get("titulo"), dados.get("nota"), dados.get("descricao"));
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Review atualizada com sucesso!");
                        } else {
                            mostrarAlerta(AlertType.ERROR, "Erro", (String) response.get("mensagem"));
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
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
        dialog.setHeaderText("Deletar uma Review");
        dialog.setContentText("Digite o ID da sua Review para excluir:");

        dialog.showAndWait().ifPresent(idReview -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.excluirReview(out, in, token, idReview);
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            mostrarAlerta(AlertType.INFORMATION, "Sucesso", "Review deletada.");
                        } else {
                            mostrarAlerta(AlertType.ERROR, "Erro", (String) response.get("mensagem"));
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });
    }
}