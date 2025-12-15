package br.com.guilhermando.cliente;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class GuiLogger {
    private static TextArea outputArea;
    private static StringBuilder history = new StringBuilder();

    public static TextArea createConsole() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setPrefHeight(80);
        console.setWrapText(true);
        console.getStyleClass().add("console-log");
        
        console.setText(history.toString());
        console.setScrollTop(Double.MAX_VALUE);

        setOutputArea(console);
        
        return console;
    }

    public static void setOutputArea(TextArea area) {
        outputArea = area;
    }

    public static void log(String prefixo, String mensagem) {
        String linha = prefixo + " " + mensagem + "\n";
        
        history.append(linha);

        if (outputArea != null) {
            Platform.runLater(() -> {
                outputArea.appendText(linha);
                outputArea.setScrollTop(Double.MAX_VALUE); 
            });
        }
        
        System.out.print(linha);
    }
}