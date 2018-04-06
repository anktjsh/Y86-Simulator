package virtual.machine;

import javafx.application.Preloader;
import javafx.application.Preloader.ProgressNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Simple Preloader Using the ProgressBar Control
 *
 * @author aniket
 */
public class Y86Preloader extends Preloader {

    public static final String CSS = Y86VM.class.getResource("material.css").toExternalForm();
    public static final Image ICON = new Image(Y86VM.class.getResourceAsStream("icon.png"));
    private ProgressBar bar;
    private Stage stage;
    private boolean noLoadingProgress = true;

    private Scene createPreloaderScene() {
        bar = new ProgressBar(0);
        BorderPane p = new BorderPane();
        Label title = new Label("Y86-64 Assembly Virtual Machine Simulator");
        title.setFont(new Font(20));
        p.setTop(title);
        BorderPane.setAlignment(p.getTop(), Pos.CENTER);
        ImageView im = new ImageView(ICON);
        p.setCenter(im);
        im.setPreserveRatio(true);
        im.setFitWidth(200);
        p.setBottom(bar);
        BorderPane.setAlignment(p.getBottom(), Pos.CENTER);
        p.setPadding(new Insets(10));
        return new Scene(p, 450, 300, Color.rgb(50, 50, 50));
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(createPreloaderScene());
        stage.getScene().getStylesheets().add(CSS);
        stage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        if (pn.getProgress() != 1.0 || !noLoadingProgress) {
            bar.setProgress(pn.getProgress() / 2);
            if (pn.getProgress() > 0) {
                noLoadingProgress = false;
            }
        }
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification pn) {
        if (pn instanceof ProgressNotification) {
            double v = ((ProgressNotification) pn).getProgress();
            if (!noLoadingProgress) {
                v = 0.5 + v / 2;
            }
            bar.setProgress(v);
        } else if (pn instanceof StateChangeNotification) {
            stage.hide();
        }
    }
}
