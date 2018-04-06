package virtual.machine.view;

import java.util.function.IntFunction;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import org.fxmisc.richtext.GenericStyledArea;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

/**
 * Graphic factory that produces labels containing line numbers. To customize
 * appearance, use {@code .lineno} style class in CSS stylesheets.
 */
public class LineNumberFactory implements IntFunction<Node> {

    private static final Insets DEFAULT_INSETS = new Insets(0.0, 5.0, 0.0, 5.0);

    private static Paint DEFAULT_TEXT_FILL = Preferences.getDarkTheme() ? Color.WHITE : Color.web("#666");
    private static Background DEFAULT_BACKGROUND
            = new Background(new BackgroundFill(Preferences.getDarkTheme() ? Color.rgb(50, 50, 50) : Color.web("#ddd"), null, null));
    private static Font DEFAULT_FONT
            = Font.font("monospace", FontPosture.ITALIC, Preferences.getFontSize());

    static {
        Preferences.fontSize().addListener((ob, older, newer) -> {
            DEFAULT_FONT = Font.font("monospace", FontPosture.ITALIC, newer.intValue());
        });
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            DEFAULT_TEXT_FILL = newer ? Color.WHITE : Color.web("#666");
            DEFAULT_BACKGROUND = new Background(new BackgroundFill(newer ? Color.rgb(50, 50, 50) : Color.web("#ddd"), null, null));
        });
    }

    public static IntFunction<Node> get(GenericStyledArea<?, ?, ?> area) {
        return get(area, digits -> "%1$" + digits + "s");
    }

    public static IntFunction<Node> get(
            GenericStyledArea<?, ?, ?> area,
            IntFunction<String> format) {
        return new LineNumberFactory(area, format);
    }

    private final Val<Integer> nParagraphs;
    private final IntFunction<String> format;

    private LineNumberFactory(
            GenericStyledArea<?, ?, ?> area,
            IntFunction<String> format) {
        nParagraphs = LiveList.sizeOf(area.getParagraphs());
        this.format = format;
    }

    @Override
    public Node apply(int idx) {
        Val<String> formatted = nParagraphs.map(n -> format(idx + 1, n));

        Label lineNo = new Label();
        lineNo.setFont(DEFAULT_FONT);
        lineNo.setBackground(DEFAULT_BACKGROUND);
        lineNo.setTextFill(DEFAULT_TEXT_FILL);
        lineNo.setPadding(DEFAULT_INSETS);
        lineNo.setAlignment(Pos.TOP_RIGHT);
        lineNo.getStyleClass().add("lineno");

        lineNo.textProperty().bind(formatted.conditionOnShowing(lineNo));

        return lineNo;
    }

    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        return String.format(format.apply(digits), x);
    }
}
