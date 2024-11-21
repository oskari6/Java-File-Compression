package com.filecompressor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

//javac --module-path "C:/Program Files/Java/javafx-sdk-23.0.1/lib" --add-modules javafx.controls,javafx.fxml Main.java

public class Main extends Application {

    @Override
    public void start (Stage primaryStage){
        Label label = new Label("No file selected");
        Label compressionInfo = new Label();
        Button textButton = new Button("Compress File");
        Button textButton2 = new Button("Decompress File");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file");

        textButton.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if(selectedFile != null){
                label.setText("Selected file: " + selectedFile.getAbsolutePath());
                try {
                    FileCompressor fileCompressor = new FileCompressor();
                    long originalSize = selectedFile.length();
                    File compressedFile = fileCompressor.compress(selectedFile);
                    long compressedSize = compressedFile.length();
                    double compressionRatio = (1 - ((double) compressedSize / originalSize)) *100;
                    compressionInfo.setText(String.format("Original size : %d bytes\nCompressed size: %d bytes\nCompression: %.1f%%", originalSize, compressedSize, compressionRatio));
                } catch(Exception ex){
                    compressionInfo.setText("Error: " + ex.getMessage());
                }
            } else{
                label.setText("No file selected");
                compressionInfo.setText("");
            }
        });
        textButton2.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if(selectedFile != null){
                label.setText("Selected file: " + selectedFile.getAbsolutePath());
                try {
                    FileCompressor fileCompressor = new FileCompressor();
                    long originalSize = selectedFile.length();
                    File decompressedFile = fileCompressor.decompress(selectedFile);
                    long decompressedSize = decompressedFile.length();
                    double decompressionRatio = (1 - ((double) originalSize / decompressedSize)) *100;
                    compressionInfo.setText(String.format("Compressed size : %d bytes\nDecompressed size: %d bytes\nDecompression: %.1f%%", originalSize, decompressedSize, decompressionRatio));
                } catch(Exception ex){
                    compressionInfo.setText("Error: " + ex.getMessage());
                }
            } else{
                label.setText("No file selected");
                compressionInfo.setText("");
            }
        });

        VBox root = new VBox(10, label, textButton, textButton2,compressionInfo);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("File selector");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String [] args){
        launch(args);
    }
}