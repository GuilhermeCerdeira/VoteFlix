package br.com.guilhermando.cliente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label; 
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesMenuApp extends Application {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final String currentUserRole;
    private final String token;

    public MoviesMenuApp(Socket socket, PrintWriter out, BufferedReader in, String role, String token) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.currentUserRole = role;
        this.token = token;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFlix - Filmes");

        Label lblTitulo = new Label("Catálogo de Filmes");
        lblTitulo.getStyleClass().add("titulo-login");
        lblTitulo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 24px; -fx-font-weight: bold;");

        Button btnListar = new Button("📋 Listar Todos os Filmes");
        btnListar.getStyleClass().add("botao-menu");
        btnListar.setMaxWidth(Double.MAX_VALUE);
        
        btnListar.setOnAction(e -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.listarFilmes(out, in, token);
                    Platform.runLater(() -> exibirListaFilmes(response));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        Button btnDetalhes = new Button("🔍 Ver Detalhes e Reviews");
        btnDetalhes.getStyleClass().add("botao-menu");
        btnDetalhes.setMaxWidth(Double.MAX_VALUE);

        btnDetalhes.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Buscar Filme");
            dialog.setHeaderText("Detalhes do Filme");
            dialog.setContentText("Digite o ID do filme:");
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(id -> {
                new Thread(() -> {
                    try {
                        Map<String, Object> response = ClientService.buscarDetalhesFilme(out, in, token, id);
                        Platform.runLater(() -> exibirDetalhesFilme(response));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            });
        });

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
        cartao.getChildren().addAll(lblTitulo, new Label(""), btnListar, btnDetalhes, new Label(""), btnVoltar);

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

    private void exibirListaFilmes(Map<String, Object> response) {
        if (response != null && "200".equals(response.get("status"))) {
            List<Map<String, Object>> filmes = (List<Map<String, Object>>) response.get("filmes");
            
            if (filmes == null || filmes.isEmpty()) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Catálogo");
                alert.setHeaderText(null);
                alert.setContentText("Nenhum filme cadastrado.");
                alert.showAndWait();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> f : filmes) {
                // Tratamento seguro de números (ID e Nota)
                String idStr = String.valueOf(f.get("id")); 
                String notaStr = String.valueOf(f.get("nota"));
                
                sb.append("----------------------------------\n");
                
                sb.append(String.format("ID: %s | %s (%s)\n", idStr, f.get("titulo"), f.get("ano")));
                
                sb.append("Diretor: " + f.get("diretor") + "\n");
                sb.append("Nota: " + notaStr + "\n");
                
                Object generosObj = f.get("genero");
                String generosStr = "N/A";
                if (generosObj instanceof List) {
                    generosStr = generosObj.toString().replace("[", "").replace("]", "");
                } else if (generosObj != null) {
                    generosStr = generosObj.toString();
                }
                sb.append("Gêneros: " + generosStr + "\n");
                
                if (f.containsKey("qtd_avaliacoes")) {
                    sb.append("Avaliações: " + f.get("qtd_avaliacoes") + "\n");
                }
            }
            sb.append("----------------------------------\n");

            TextArea textArea = new TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(500, 400); 
            textArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Lista de Filmes");
            alert.setHeaderText("Filmes Disponíveis");
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();

        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("Erro: " + (response != null ? response.get("mensagem") : "Desconhecido"));
            alert.showAndWait();
        }
    }

    private void exibirDetalhesFilme(Map<String, Object> response) {
        if (response != null && "200".equals(response.get("status"))) {
            Map<String, Object> f = (Map<String, Object>) response.get("filme");
            List<Map<String, String>> reviews = (List<Map<String, String>>) response.get("reviews");

            StringBuilder sb = new StringBuilder();
            sb.append("=== " + f.get("titulo") + " ===\n");
            sb.append("Ano: " + f.get("ano") + "\n");
            sb.append("Diretor: " + f.get("diretor") + "\n");
            sb.append("Gêneros: " + f.get("genero") + "\n");
            sb.append("Sinopse: " + f.get("sinopse") + "\n\n");
            sb.append("NOTA MÉDIA: " + f.get("nota") + " (" + f.get("qtd_avaliacoes") + " avaliações)\n");
            sb.append("================================\n");
            sb.append("REVIEWS DOS USUÁRIOS:\n\n");

            if (reviews != null && !reviews.isEmpty()) {
                for (Map<String, String> r : reviews) {
                    sb.append("> " + r.get("nome_usuario") + " (Nota: " + r.get("nota") + ")\n");
                    sb.append("  \"" + r.get("titulo") + "\"\n");
                    sb.append("  " + r.get("descricao") + "\n");
                    sb.append("  Em: " + r.get("data") + "\n\n");
                }
            } else {
                sb.append("Ainda não há reviews para este filme.\n");
            }

            TextArea textArea = new TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(500, 400);

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Detalhes do Filme");
            alert.setHeaderText(null);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();

        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("Erro: " + (response != null ? response.get("mensagem") : "Filme não encontrado"));
            alert.showAndWait();
        }
    }
}