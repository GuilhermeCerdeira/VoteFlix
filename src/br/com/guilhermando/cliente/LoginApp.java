package br.com.guilhermando.cliente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class LoginApp extends Application {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VoteFLix - Login");

        Label lblConfig = new Label("Configuração do Servidor:");
        lblConfig.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        TextField txtIp = new TextField("127.0.0.1");
        txtIp.setPromptText("IP");
        txtIp.setPrefWidth(120);
        txtIp.setStyle("-fx-background-radius: 5; -fx-border-color: #bdc3c7;"); 

        TextField txtPorta = new TextField("2000");
        txtPorta.setPromptText("Porta");
        txtPorta.setPrefWidth(60);
        txtPorta.setStyle("-fx-background-radius: 5; -fx-border-color: #bdc3c7;");

        Button btnConectar = new Button("Conectar");
        btnConectar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        Circle statusLight = new Circle(5, Color.RED);
        Label lblStatus = new Label("Offline");
        lblStatus.setTextFill(Color.RED);
        
        HBox boxStatus = new HBox(5);
        boxStatus.setAlignment(Pos.CENTER);
        boxStatus.getChildren().addAll(statusLight, lblStatus);

        HBox boxRede = new HBox(10);
        boxRede.setAlignment(Pos.CENTER);
        boxRede.getChildren().addAll(lblConfig, txtIp, new Label(":"), txtPorta, btnConectar);

        Label lblTitulo = new Label("VoteFlix");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 36)); 
        lblTitulo.setStyle("-fx-text-fill: #2c3e50;"); 

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");
        txtUsuario.getStyleClass().add("campo-texto-figma");
        txtUsuario.setDisable(true);

        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha");
        txtSenha.getStyleClass().add("campo-texto-figma");
        txtSenha.setDisable(true); 

        Button btnLogin = new Button("ENTRAR");
        btnLogin.getStyleClass().add("botao-principal");
        btnLogin.setPrefWidth(250);
        btnLogin.setDisable(true); 
        
        Button btnRegistrar = new Button("CRIAR CONTA");
        btnRegistrar.getStyleClass().add("botao-azul"); 
        btnRegistrar.setPrefWidth(250);
        btnRegistrar.setDisable(true);

        Label lblMensagem = new Label();
        lblMensagem.getStyleClass().add("mensagem-erro");
        lblMensagem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        TextArea txtConsole = GuiLogger.createConsole();

        btnConectar.setOnAction(e -> {
            String ip = txtIp.getText();
            String portaStr = txtPorta.getText();
            
            lblMensagem.setText("Tentando conectar...");
            lblMensagem.setStyle("-fx-text-fill: #f1c40f;"); 
            GuiLogger.log("[SISTEMA]", "Conectando em " + ip + ":" + portaStr + "...");

            new Thread(() -> {
                try {
                    int porta = Integer.parseInt(portaStr);
                    
                    clientSocket = new Socket(ip, porta);
                    out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

                    Platform.runLater(() -> {
                        statusLight.setFill(Color.GREEN);
                        lblStatus.setText("Conectado");
                        lblStatus.setTextFill(Color.GREEN);
                        lblMensagem.setText("Conectado! Faça login.");
                        lblMensagem.setStyle("-fx-text-fill: #2ecc71;");
                        GuiLogger.log("[SISTEMA]", "Conexão estabelecida.");
                        
                        txtUsuario.setDisable(false);
                        txtSenha.setDisable(false);
                        btnLogin.setDisable(false);
                        btnRegistrar.setDisable(false);
                        
                        btnConectar.setDisable(true);
                        txtIp.setDisable(true);
                        txtPorta.setDisable(true);
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblMensagem.setText("Falha: " + ex.getMessage());
                        lblMensagem.setStyle("-fx-text-fill: #e74c3c;");
                        GuiLogger.log("[ERRO]", ex.getMessage());
                    });
                }
            }).start();
        });

        btnLogin.setOnAction(e -> {
            String usuario = txtUsuario.getText();
            String senha = txtSenha.getText();

            if (usuario.isEmpty() || senha.isEmpty()) {
                lblMensagem.setText("Preencha todos os campos.");
                return;
            }
            
            lblMensagem.setText("Entrando...");
            lblMensagem.setStyle("-fx-text-fill: #f1c40f;");
            
            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.enviarLogin(out, in, usuario, senha);
                    
                    Platform.runLater(() -> {
                        if (response == null) {
                            lblMensagem.setText("Erro: Servidor caiu.");
                            return;
                        }

                        String status = (String) response.get("status");
                        String mensagem = (String) response.get("mensagem");

                        if ("200".equals(status)) {
                            lblMensagem.setStyle("-fx-text-fill: #2ecc71;");
                            lblMensagem.setText("Sucesso! Redirecionando...");
                            
                            String token = (String) response.get("token");
                            
                            String cargo = "user";
                            if(usuario.equalsIgnoreCase("admin")) {
                                cargo = "admin";
                            }

                            GuiLogger.log("[SISTEMA]", "Login OK. Cargo: " + cargo);

                            try {
                                UserMenuApp menu = new UserMenuApp(clientSocket, out, in, cargo, token);
                                menu.start(primaryStage);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            
                        } else {
                            lblMensagem.setStyle("-fx-text-fill: #e74c3c;");
                            lblMensagem.setText("Erro (" + status + "): " + mensagem);
                        }
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> lblMensagem.setText("Erro de I/O: " + ex.getMessage()));
                }
            }).start();
        });

        btnRegistrar.setOnAction(e -> {
            String usuario = txtUsuario.getText();
            String senha = txtSenha.getText();

            if (usuario.isEmpty() || senha.isEmpty()) {
                lblMensagem.setStyle("-fx-text-fill: #e74c3c;");
                lblMensagem.setText("Preencha usuário e senha.");
                return;
            }

            lblMensagem.setStyle("-fx-text-fill: #f1c40f;");
            lblMensagem.setText("Cadastrando...");

            new Thread(() -> {
                try {
                    Map<String, Object> response = ClientService.enviarCadastro(out, in, usuario, senha);
                    
                    Platform.runLater(() -> {
                        if (response == null) {
                             lblMensagem.setText("Erro: Servidor caiu.");
                             return;
                        }

                        String status = (String) response.get("status");
                        String mensagem = (String) response.get("mensagem");

                        if ("201".equals(status)) {
                            lblMensagem.setStyle("-fx-text-fill: #2ecc71;");
                            lblMensagem.setText("Conta criada! Clique em ENTRAR.");
                        } else {
                            lblMensagem.setStyle("-fx-text-fill: #e74c3c;");
                            lblMensagem.setText("Erro ("+status+"): " + mensagem);
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });


        VBox cartaoLogin = new VBox(15);
        cartaoLogin.setAlignment(Pos.CENTER);
        cartaoLogin.getStyleClass().add("login-card");
        
        cartaoLogin.getChildren().addAll(
            boxRede, boxStatus, new Label(""), 
            lblTitulo, txtUsuario, txtSenha, new Label(""),
            btnLogin, btnRegistrar, lblMensagem
        );

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("fundo-login");
        root.setPadding(new Insets(20));
        root.getChildren().addAll(cartaoLogin, txtConsole);
        VBox.setVgrow(cartaoLogin, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 750);
        
        String cssPath = "style.css";
        java.net.URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}