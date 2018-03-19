package virtual.machine;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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

    @Override
    public void start(Stage stage) {
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
        Stage stage = new Stage();
        stage.getIcons().add(Y86VM.ICON);
        Environment environ = new Environment();
        stage.setTitle("Y86 Virtual Machine Simulator");
        stage.setScene(new Scene(new Editor(environ)));
        stage.getScene().getStylesheets().add(CSS);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
        stage.setOnCloseRequest((e) -> {
            TabPane tabs = (TabPane) ((BorderPane) stage.getScene().getRoot()).getCenter();
            boolean option = false;
            for (Tab b : tabs.getTabs()) {
                if (b.getText().endsWith("*")) {
                    option = true;
                }
            }
            if (option) {
                Alert al = new Alert(AlertType.CONFIRMATION);
                al.setHeaderText("Would you like to save before closing?");
                al.setTitle("Close");
                al.initOwner(stage);
                al.initModality(Modality.APPLICATION_MODAL);
                al.getButtonTypes().add(ButtonType.NO);
                al.showAndWait().ifPresent((ef) -> {
                    if (ef == ButtonType.OK) {
                        Editor ed = (Editor) stage.getScene().getRoot();
                        ed.saveScripts();
                        if (stage.isFullScreen()) {
                            stage.setFullScreen(false);
                        }
                        st.show();
                    } else if (ef == ButtonType.CANCEL) {
                        e.consume();
                    }
                });
            } else {
                if (stage.isFullScreen()) {
                    stage.setFullScreen(false);
                }
                st.show();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
