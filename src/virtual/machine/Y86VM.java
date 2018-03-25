package virtual.machine;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import virtual.machine.internal.Environment;
import virtual.machine.view.Editor;

/**
 *
 * @author aniket
 */
public class Y86VM extends Application {

    public static final String CSS = Y86VM.class.getResource("material.css").toExternalForm();
    public static final Image ICON = new Image(Y86VM.class.getResourceAsStream("icon.png"));
    private Stage currentStage;

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(currentStage);
                alert.setTitle("An error has occurred");
                alert.setHeaderText("An unsafe exception was caught");
                alert.setContentText("A log of this error has been stored");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String exceptionText = sw.toString();
                Label label = new Label("The exception stacktrace was:");
                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);
                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);
                alert.getDialogPane().setExpandableContent(expContent);
                File f = new File("assembly", "logs");
                if (!f.exists()) {
                    f.mkdirs();
                }
                File save = new File(f, LocalDateTime.now().toString() + "-logs.txt");
                try {
                    Files.write(save.toPath(), sw.toString().getBytes());
                } catch (IOException ex) {
                }
                alert.showAndWait();
            });
        });
        stage.setOnCloseRequest((e) -> {
            Platform.exit();
            System.exit(0);
        });
        stage.getIcons().add(Y86VM.ICON);
        stage.setTitle("Y86 Assembly Virtual Machine");

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        scene.getStylesheets().add(Y86VM.CSS);

        VBox center = new VBox(15);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(10));
        root.setCenter(center);
        center.setMinWidth(390);
        center.setMinHeight(300);

        Label title = new Label("Y86 Assembly Virtual Machine");
        ImageView iconView = new ImageView(Y86VM.ICON);
        iconView.setFitHeight(100);
        iconView.setPreserveRatio(true);
        title.setContentDisplay(ContentDisplay.BOTTOM);
        title.setGraphic(iconView);
        title.setFont(new Font(25));
        center.getChildren().add(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(50, 5, 5, 5));

        Button run = new Button("Launch Machine");
        FontAwesomeIconView plus = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
        plus.setSize("2em");
        run.setGraphic(plus);
        run.setDefaultButton(true);
        run.setFont(new Font(16));
        center.getChildren().add(run);
        BorderPane.setAlignment(run, Pos.CENTER);
        BorderPane.setMargin(run, new Insets(5));
        run.setContentDisplay(ContentDisplay.BOTTOM);
        run.setOnAction((E) -> {
            launchVM(stage);
            stage.hide();
        });
        stage.show();
    }

    private void launchVM(Stage st) {
        currentStage = new Stage();
        currentStage.getIcons().add(Y86VM.ICON);
        Environment environ = new Environment();
        currentStage.setTitle("Y86 Virtual Machine Simulator");
        currentStage.setScene(new Scene(new Editor(environ)));
        currentStage.getScene().getStylesheets().add(CSS);
        currentStage.setFullScreenExitHint("");
        currentStage.setFullScreen(true);
        currentStage.show();
        currentStage.setOnCloseRequest((e) -> {
            Editor ed = (Editor) currentStage.getScene().getRoot();
            TabPane tabs = (TabPane) ((BorderPane) ed.getCenter()).getCenter();
            boolean option = false;
            for (Tab b : tabs.getTabs()) {
                if (b.getText().endsWith("*")) {
                    option = true;
                }
            }
            ed.saveScripts();
            if (option) {
                Alert al = new Alert(AlertType.CONFIRMATION);
                al.setHeaderText("Would you like to save before closing?");
                al.setTitle("Close");
                al.initOwner(currentStage);
                al.initModality(Modality.APPLICATION_MODAL);
                al.getButtonTypes().add(ButtonType.NO);
                al.showAndWait().ifPresent((ef) -> {
                    if (ef == ButtonType.OK) {
                        ed.saveAll();
                        showIntro(currentStage, st);
                    } else if (ef == ButtonType.CANCEL) {
                        e.consume();
                    } else {
                        showIntro(currentStage, st);
                    }
                });
            } else {
                showIntro(currentStage, st);
            }
        });
    }

    private void showIntro(Stage stage, Stage st) {
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
        }
        st.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
