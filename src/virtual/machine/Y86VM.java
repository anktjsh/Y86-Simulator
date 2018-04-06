package virtual.machine;

import de.codecentric.centerdevice.MenuToolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.ProgressNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import virtual.machine.internal.Environment;
import virtual.machine.view.Editor;
import virtual.machine.view.Preferences;

/**
 *
 * @author aniket
 */
public class Y86VM extends Application {

    public static final String CSS = Y86VM.class.getResource("material.css").toExternalForm();
    public static final Image ICON = new Image(Y86VM.class.getResourceAsStream("icon.png"));

    BooleanProperty ready = new SimpleBooleanProperty(false);

    private void longStart() {
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int max = 10;
                for (int i = 1; i <= max; i++) {
                    Thread.sleep(300);
                    notifyPreloader(new ProgressNotification(((double) i) / max));
                }
                ready.setValue(Boolean.TRUE);
                notifyPreloader(new StateChangeNotification(
                        StateChangeNotification.Type.BEFORE_START));
                return null;
            }
        };
        new Thread(task).start();
    }

    @Override
    public void start(Stage stage) {
        longStart();
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(stage);
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
        launchVM(stage);
        ready.addListener((ov, t, t1) -> {
            if (Boolean.TRUE.equals(t1)) {
                Platform.runLater(() -> {
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        setAppleSpecificFeatures(stage);
                    }
                    stage.show();
                });
            }
        });
    }

    private void launchVM(Stage currentStage) {
        currentStage.getIcons().add(Y86VM.ICON);
        Environment environ = new Environment();
        currentStage.setTitle("Y86-64 Assembly Virtual Machine Simulator");
        currentStage.setScene(new Scene(new Editor(environ)));
        if (Preferences.getDarkTheme()) {
            currentStage.getScene().getStylesheets().add(CSS);
        }
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            if (newer) {
                currentStage.getScene().getStylesheets().add(CSS);
            } else {
                currentStage.getScene().getStylesheets().remove(CSS);
            }
        });
        currentStage.setFullScreenExitHint("");
        currentStage.setFullScreen(true);
        currentStage.setOnCloseRequest((e) -> {
            closeRequest(currentStage, e);
        });
    }

    private void closeRequest(Stage currentStage, WindowEvent e) {
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
                    hideFullScreen(currentStage);
                } else if (ef == ButtonType.CANCEL) {
                    e.consume();
                } else {
                    hideFullScreen(currentStage);
                }
            });
        } else {
            hideFullScreen(currentStage);
        }
    }

    private void hideFullScreen(Stage stage) {
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Preferences pref;

    private void setAppleSpecificFeatures(Stage stage) {
        MenuToolkit tk = MenuToolkit.toolkit();
        Menu defaultApplicationMenu = tk.createDefaultApplicationMenu("Y86-64 VM");
        tk.setApplicationMenu(defaultApplicationMenu);
        defaultApplicationMenu.getItems().get(2).setOnAction((e) -> {
            Alert al = new Alert(AlertType.INFORMATION);
            al.initModality(Modality.APPLICATION_MODAL);
            al.initOwner(stage);
            al.setTitle("About Y86VM");
            al.setHeaderText("Y86-64 VM Created by Aniket Joshi (2018)");
            al.setContentText("Y86-64 VM supports all standard Y86-64 operations with additional single operand and double operand mathematical functions, breakpoints,"
                    + " as well as the Carry Flag and all associated jumps and conditional moves for unsigned operations.");
            al.showAndWait();
        });
        defaultApplicationMenu.getItems().get(0).setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCodeCombination.META_DOWN));
        defaultApplicationMenu.getItems().get(0).setOnAction((e) -> {
            if (pref == null) {
                pref = new Preferences(stage);
            }
            pref.showAndWait();
        });
    }

}
