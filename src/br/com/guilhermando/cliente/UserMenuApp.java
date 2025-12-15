package br.com.guilhermando.cliente;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Map;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import java.util.Optional;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class UserMenuApp extends Application {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final String currentUserRole;
    private final String token;

    public UserMenuApp(Socket socket, PrintWriter out, BufferedReader in, String role, String token) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.currentUserRole = role;
        this.token = token;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFlix - Menu Principal");

        Label lblTitulo = new Label("Menu Principal");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblTitulo.setStyle("-fx-text-fill: #2c3e50;");

        Label lblSubtitulo = new Label("Logado como: " + currentUserRole.toUpperCase());
        lblSubtitulo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Label lblSecao1 = new Label("CONTEÚDO");
        lblSecao1.getStyleClass().add("subtitulo");

        Button btnFilmes = new Button("🎬  Opções de Filmes");
        btnFilmes.getStyleClass().add("botao-menu");
        btnFilmes.setMaxWidth(Double.MAX_VALUE);
        btnFilmes.setOnAction(e -> System.out.println("Ir para tela de Filmes..."));

        Button btnReviews = new Button("📖  Opções de Reviews");
        btnReviews.getStyleClass().add("botao-menu");
        btnReviews.setMaxWidth(Double.MAX_VALUE);
        btnReviews.setOnAction(e -> System.out.println("Ir para tela de Reviews..."));

        Label lblSecao2 = new Label("MINHA CONTA");
        lblSecao2.getStyleClass().add("subtitulo");

        Button btnMeusDados = new Button("👤  Ver Meus Dados");
        btnMeusDados.getStyleClass().add("botao-menu");
        btnMeusDados.setMaxWidth(Double.MAX_VALUE);
        btnMeusDados.setOnAction(e -> System.out.println("Ver dados..."));

        Button btnAlterarSenha = new Button("🔒  Alterar Senha");
        btnAlterarSenha.getStyleClass().add("botao-menu");
        btnAlterarSenha.setMaxWidth(Double.MAX_VALUE);
        btnAlterarSenha.setOnAction(e -> System.out.println("Alterar senha..."));
        
        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 10, 0));

        Button btnDeletar = new Button("Deletar Conta");
        btnDeletar.getStyleClass().add("botao-perigo");
        btnDeletar.setMaxWidth(Double.MAX_VALUE);
        btnDeletar.setOnAction(e -> System.out.println("Deletar conta..."));

        Button btnLogout = new Button("Sair (Logout)");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-underline: true; -fx-cursor: hand; -fx-font-size: 14px;");
        
        btnLogout.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String json = "{\"operacao\":\"LOGOUT\", \"token\":\"" + token + "\"}";
                    out.println(json);
                    in.readLine(); 
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                }
            }).start();

            primaryStage.close();

            try {
                new LoginApp().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnAdmin = null;
        
        if ("admin".equals(currentUserRole)) {
            Label lblSecaoAdmin = new Label("ADMINISTRAÇÃO");
            lblSecaoAdmin.getStyleClass().add("subtitulo");
            lblSecaoAdmin.setTextFill(Color.web("#e74c3c"));

            btnAdmin = new Button("Acessar Painel Admin");
            btnAdmin.getStyleClass().add("botao-menu");
            btnAdmin.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;"); 
            btnAdmin.setMaxWidth(Double.MAX_VALUE);
            
            btnAdmin.setOnAction(e -> {
                try {
                    new AdminMenuApp(socket, out, in, currentUserRole, token).start(primaryStage);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            
        }

        btnMeusDados.setOnAction(e -> {
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.buscarMeusDados(out, in, token);
                    
                    Platform.runLater(() -> {
                        if (response != null && "200".equals(response.get("status"))) {
                            
                            Object usuarioObj = response.get("usuario");
                            String textoDados = "";

                            if (usuarioObj instanceof Map) {
                                Map<?, ?> uData = (Map<?, ?>) usuarioObj;
                                String nome = (String) uData.get("nome");
                                String tipo = (String) uData.get("tipo");
                                textoDados = "Usuário: " + nome + "\nCargo/Tipo: " + tipo;
                            } else {
                                textoDados = "Usuário: " + usuarioObj.toString();
                            }

                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Meus Dados");
                            alert.setHeaderText("Informações da Conta");
                            alert.setContentText(textoDados);
                            alert.showAndWait();

                        } else {
                            String msg = response != null ? (String)response.get("mensagem") : "Erro de conexão";
                            mostrarAlerta(AlertType.ERROR, "Erro", msg);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        btnAlterarSenha.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Alterar Senha");
            dialog.setHeaderText("Segurança da Conta");
            dialog.setContentText("Digite sua nova senha:");

            Optional<String> result = dialog.showAndWait();
            
            result.ifPresent(novaSenha -> {
                if (novaSenha.length() < 6) {
                    Alert erro = new Alert(AlertType.WARNING);
                    erro.setContentText("A senha deve ter no mínimo 6 caracteres.");
                    erro.showAndWait();
                    return;
                }

                new Thread(() -> {
                    try {
                        Map<String, Object> response = ClientService.alterarSenha(out, in, token, novaSenha);
                        
                        Platform.runLater(() -> {
                            if (response != null && "200".equals(response.get("status"))) {
                                Alert sucesso = new Alert(AlertType.INFORMATION);
                                sucesso.setTitle("Sucesso");
                                sucesso.setContentText("Senha alterada com sucesso!");
                                sucesso.showAndWait();
                            } else {
                                String msg = response != null ? (String)response.get("mensagem") : "Erro";
                                Alert erro = new Alert(AlertType.ERROR);
                                erro.setContentText(msg);
                                erro.showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            });
        });

        btnDeletar.setOnAction(e -> {
            Alert confirmacao = new Alert(AlertType.CONFIRMATION);
            confirmacao.setTitle("DeletarConta");
            confirmacao.setHeaderText("Você tem certeza que deseja deletar sua conta?");
            confirmacao.setContentText("pipipipopopopo");

            Optional<ButtonType> resultado = confirmacao.showAndWait();
            
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Map<String, Object> response = ClientService.deletarConta(out, in, token);
                        
                        Platform.runLater(() -> {
                            if (response != null && "200".equals(response.get("status"))) {
                                Alert adeus = new Alert(AlertType.INFORMATION);
                                adeus.setTitle("Conta Deletada");
                                adeus.setHeaderText(null);
                                adeus.setContentText("Sua conta foi deletada com sucesso. O programa será encerrado.");
                                adeus.showAndWait();
                                
                                primaryStage.close();
                                
                                
                            } else {
                                String msg = response != null ? (String)response.get("mensagem") : "Erro de conexão";
                                Alert erro = new Alert(AlertType.ERROR);
                                erro.setTitle("Erro");
                                erro.setContentText("Falha ao deletar: " + msg);
                                erro.showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        btnFilmes.setOnAction(e -> {
            try {
                MoviesMenuApp moviesApp = new MoviesMenuApp(socket, out, in, currentUserRole, token);
                moviesApp.start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        btnReviews.setOnAction(e -> {
            try {
                new ReviewsMenuApp(socket, out, in, currentUserRole, token).start(primaryStage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        VBox cartaoMenu = new VBox(10);
        cartaoMenu.setPadding(new Insets(30));
        cartaoMenu.setAlignment(Pos.TOP_LEFT);
        cartaoMenu.getStyleClass().add("login-card"); 
        cartaoMenu.setPrefWidth(400);

        cartaoMenu.getChildren().addAll(
            lblTitulo, 
            lblSubtitulo,
            new Label(""), 
            lblSecao1,
            btnFilmes, 
            btnReviews,
            new Label(""), 
            lblSecao2,
            btnMeusDados, 
            btnAlterarSenha,
            sep,
            btnDeletar,
            btnLogout
        );

        if (btnAdmin != null) {
            cartaoMenu.getChildren().addAll(new Label(""), new Label("ADMINISTRAÇÃO"), btnAdmin);
        }

        javafx.scene.control.TextArea txtConsole = GuiLogger.createConsole();

        VBox root = new VBox(15); 
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("fundo-login");
        root.setPadding(new javafx.geometry.Insets(20)); 
        
        root.getChildren().addAll(cartaoMenu, txtConsole);
        VBox.setVgrow(cartaoMenu, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 700);
        
        String cssPath = "style.css";
        java.net.URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void mostrarAlerta(AlertType tipo, String titulo, String msg) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}