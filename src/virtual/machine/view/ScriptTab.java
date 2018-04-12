package virtual.machine.view;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import static virtual.machine.Y86VM.ICON;
import virtual.machine.core.Pair;
import virtual.machine.core.Script;
import virtual.machine.execution.Compiler;
import virtual.machine.execution.ConcurrentCompiler;

/**
 *
 * @author aniket
 */
public class ScriptTab extends Tab {

    public static final String LIGHT = ScriptTab.class.getResource("light_css.css").toExternalForm(),
            DARK = ScriptTab.class.getResource("dark_css.css").toExternalForm();
    private final CodeArea area;
    private final Script script;
    private final BorderPane center, bottom;

    private final Pair<Integer, String> errorLines = new Pair<>(-1, "");
    private IntFunction<Node> numberFactory;
    private IntFunction<Node> arrowFactory;

    private final IntegerProperty rowPosition;

    private static final String[] KEYWORDS = new String[]{
        "jl", "subq", "jge", "cmovge", "jmp", "nop",
        "xorq", "cmovg", "cmove", "andq",
        "cmovl", "addq", "rrmovq", "cmovb", "cmovnb", "cmovbe", "cmova",
        "ja", "jbe", "jbe", "jnb",
        "irmovq", "jne", "ret", "jle", "rmmovq",
        "cmovne", "cmovle", "call",
        "halt", "popq", "pushq", "mrmovq", "je", "jg",
        "imultq", "divq", "modq", "sarq", "shrq", "salq", "orq",
        "incq", "decq", "notq", "negq", "bangq"
    };

    private static final String[] DIRECTIVES = new String[]{
        "align", "pos", "quad", "brk"
    };

    private static final Set<String> CURRENT = new HashSet<>(Compiler.getInstance().getLabels());

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String REGISTER_PATTERN = "%[^\\n]{2,3}\\b";
    private static String LABEL_PATTERN = "\\b(" + String.join("|", CURRENT) + ")\\b";
    private static final String COMMENT_PATTERN = "#[^\n]*";
    private static final String CONSTANT_PATTERN = "\\$([^,]+)";
    private static final String DIRECTIVE_PATTERN = "\\b(" + String.join("|", DIRECTIVES) + ")\\b[^\\n]*";

    private static Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<DIRECTIVE>" + DIRECTIVE_PATTERN + ")"
            + "|(?<REGISTER>" + REGISTER_PATTERN + ")"
            + "|(?<LABEL>" + LABEL_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<CONSTANT>" + CONSTANT_PATTERN + ")"
    );

    private int getRow(int caret) {
        String spl[] = area.getText().split("\n");
        int count = 0;
        for (int x = 0; x < spl.length; x++) {
            count += (spl[x].length() + 1);
            if (caret < count) {
                return x;
            } else if (caret == count) {
                return x + 1;
            }
        }
        return -1;
    }

    public ScriptTab(Script scr) {
        super(scr.getFile().getName());
        script = scr;
        rowPosition = new SimpleIntegerProperty();
        area = new CodeArea();
        area.setStyle("-fx-font-size:" + Preferences.getFontSize() + ";"
                + "-fx-font-family:" + Preferences.getFontName() + ";");
        Preferences.fontSize().addListener((ob, older, neweR) -> {
            Platform.runLater(() -> {
                area.setStyle("-fx-font-size:" + neweR.intValue() + ";"
                        + "-fx-font-family:" + Preferences.getFontName() + ";");
                placeFactory();
            });
        });
        Preferences.darkTheme().addListener((ob, older, neweR) -> {
            Platform.runLater(() -> {
                area.getStylesheets().remove((older ? DARK : LIGHT));
                area.getStylesheets().add(neweR ? DARK : LIGHT);
                placeFactory();
            });
        });
        Preferences.fontName().addListener((ob, older, newer) -> {
            Platform.runLater(() -> {
                area.setStyle("-fx-font-size:" + Preferences.getFontSize() + ";"
                        + "-fx-font-family:" + newer + ";");
            });
        });
        area.getStylesheets().add(Preferences.getDarkTheme() ? DARK : LIGHT);
        setOnCloseRequest((e) -> {
            if (getText().endsWith("*")) {
                Alert al = new Alert(AlertType.CONFIRMATION);
                ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
                al.initModality(Modality.APPLICATION_MODAL);
                al.initOwner(getTabPane().getScene().getWindow());
                al.setHeaderText("Would you like to save before closing?");
                al.setTitle("Closing Tab");
                al.getButtonTypes().add(ButtonType.NO);
                al.showAndWait().ifPresent((ef) -> {
                    if (ef == ButtonType.OK) {
                        save();
                    } else if (ef == ButtonType.CANCEL) {
                        e.consume();
                    }
                });
            }
        });
        setContextMenu(new ContextMenu());
        getContextMenu().getItems().addAll(new MenuItem("Close"),
                new MenuItem("Close All"),
                new MenuItem("Close Other"),
                new MenuItem("Copy File"),
                new MenuItem("Copy File Path"));
        getContextMenu().getItems().get(0).setOnAction((e) -> {
            Event.fireEvent(this, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
            if (getTabPane() != null) {
                getTabPane().getTabs().remove(this);
            }
        });
        getContextMenu().getItems().get(1).setOnAction((e) -> {
            TabPane pane = getTabPane();
            pane.getTabs().stream().map((b) -> {
                Event.fireEvent(b, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                return b;
            }).filter((b) -> (getTabPane() != null)).forEachOrdered((b) -> {
                getTabPane().getTabs().remove(b);
            });
        });
        getContextMenu().getItems().get(2).setOnAction((e) -> {
            TabPane pane = getTabPane();
            pane.getTabs().stream().filter((b) -> (!b.equals(this))).map((b) -> {
                Event.fireEvent(b, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                return b;
            }).filter((b) -> (getTabPane() != null)).forEachOrdered((b) -> {
                getTabPane().getTabs().remove(b);
            });
        });
        getContextMenu().getItems().get(3).setOnAction((e) -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putUrl(getScript().getFile().getAbsolutePath());
            cc.getFiles().add(getScript().getFile());
            cc.put(DataFormat.PLAIN_TEXT, area.getText());
            Clipboard.getSystemClipboard().setContent(cc);
        });
        getContextMenu().getItems().get(4).setOnAction((e) -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putUrl(getScript().getFile().getAbsolutePath());
            cc.putString(cc.getUrl());
            Clipboard.getSystemClipboard().setContent(cc);
        });
        center = new BorderPane();
        center.setCenter(new VirtualizedScrollPane(area));
        setContent(center);
        center.setBottom(bottom = new BorderPane());
        Label loc = new Label("");
        bottom.setRight(loc);
        area.caretPositionProperty().addListener((ob, older, newer) -> {
            rowPosition.set(getRow(area.getCaretPosition()));
            loc.setText((rowPosition.get() + 1) + ":" + area.getCaretColumn());
        });
        initFactory();
        placeFactory();
        area.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(100))
                .subscribe(change -> {
                    area.setStyleSpans(0, computeHighlighting(area.getText()));
                });
        area.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(500))
                .subscribe(change -> {
                    concurrent(area.getText());
                });
        concurrent(scr.getCurrentCode());
        area.replaceText(scr.getCurrentCode());
        area.scrollToPixel(0, 0);
        area.textProperty().addListener((ob, older, newer) -> {
            if (scr.canSave(newer)) {
                setText(scr.getFile().getName() + "*");
            } else {
                setText(scr.getFile().getName());
            }
        });
        area.setContextMenu(new ContextMenu());
        area.getContextMenu().getItems().addAll(
                new MenuItem("Undo"),
                new MenuItem("Redo"),
                new MenuItem("Cut"),
                new MenuItem("Copy"),
                new MenuItem("Paste"),
                new MenuItem("Select All"));
        area.getContextMenu().getItems().get(0).setOnAction((e) -> {
            undo();
        });
        area.getContextMenu().getItems().get(1).setOnAction((e) -> {
            redo();
        });
        area.getContextMenu().getItems().get(2).setOnAction((e) -> {
            cut();
        });
        area.getContextMenu().getItems().get(3).setOnAction((e) -> {
            copy();
        });
        area.getContextMenu().getItems().get(4).setOnAction((e) -> {
            paste();
        });
        area.getContextMenu().getItems().get(5).setOnAction((E) -> {
            selectAll();
        });
        area.setOnKeyPressed((e) -> {
            if (e.getCode() == KeyCode.ENTER) {
                area.deleteText(area.getSelection());
                int n = area.getCaretPosition();
                if (n != 0) {
                    String tabs = "\t";
                    area.insertText(n, tabs);
                }
            }
        });
    }

    public final void concurrent(String s) {
        ConcurrentCompiler.compile(s, (Integer p1, String p2) -> {
            if (p1 != null) {
                errorLines.setKey(p1);
                errorLines.setValue(p2);
                setStyle("-fx-background-color:red;");
            } else {
                errorLines.setKey(-1);
                errorLines.setValue("");
                setStyle("");
            }
            placeFactory();
            return null;
        });
    }

    public void undo() {
        area.undo();
    }

    public void redo() {
        area.redo();
    }

    public void cut() {
        area.cut();
    }

    public void copy() {
        area.copy();
    }

    public void paste() {
        area.paste();
    }

    public void selectAll() {
        area.selectAll();
    }

    public Script getScript() {
        return script;
    }

    public void find() {
        bottom.setTop(getFindBox());
        ((HBox) getFindBox()).getChildren().get(3).requestFocus();
    }

    private Node getReplaceBox() {
        if (replaceBox == null) {
            replaceBox = createReplaceBox((ab) -> {
                bottom.setTop(null);
                return null;
            });
        }
        return replaceBox;
    }

    private Node findBox;
    private Node replaceBox;

    private Node getFindBox() {
        if (findBox == null) {
            findBox = createFindBox((ab) -> {
                bottom.setTop(null);
                return null;
            });
        }
        return findBox;
    }

    private Node createFindBox(Callback<Void, Void> call) {
        HBox box = new HBox(15);
        box.setPadding(new Insets(5, 10, 5, 10));
        TextField fi;
        Button prev, next, close;
        MaterialDesignIconView cl = new MaterialDesignIconView(MaterialDesignIcon.CLOSE);
        if (Preferences.getDarkTheme()) {
            cl.setStyle("-fx-fill:white;");
        }
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            String style = newer ? "-fx-fill:white;" : "";
            cl.setStyle(style);
        });
        box.getChildren().addAll(fi = new TextField(),
                prev = new Button("Previous"),
                next = new Button("Next"),
                close = new Button("", cl));
        fi.setPromptText("Find");
        fi.setOnAction((ea) -> {
            next.fire();
        });
        prev.setOnAction((efd) -> {
            int start = area.getSelection().getStart();
            String a = area.getText().substring(0, start);
            int index = a.lastIndexOf(fi.getText());
            if (index != -1) {
                area.selectRange(index, index + fi.getText().length());
            }
        });
        next.setOnAction((sdfsdfsd) -> {
            if (area.getSelection().getLength() == 0) {
                String a = fi.getText();
                int index = area.getText().indexOf(a);
                if (index != -1) {
                    area.selectRange(index, index + a.length());
                }
            } else {
                int end = area.getSelection().getEnd();
                String a = area.getText().substring(end);
                int index = a.indexOf(fi.getText());
                if (index != -1) {
                    index += end;
                    area.selectRange(index, index + fi.getText().length());
                }
            }
        });
        close.setOnAction((se) -> {
            call.call(null);
        });
        return box;
    }

    public void replace() {
        bottom.setTop(getReplaceBox());
        ((HBox) ((VBox) ((BorderPane) getReplaceBox()).getCenter()).getChildren().get(0))
                .getChildren().get(0).requestFocus();
    }

    private Node createReplaceBox(Callback<Void, Void> call) {
        VBox total = new VBox();
        BorderPane border = new BorderPane(total);
        HBox top = new HBox(15);
        HBox below = new HBox(5);
        total.getChildren().addAll(top, below);
        below.setPadding(new Insets(5, 10, 5, 10));
        top.setPadding(new Insets(5, 10, 5, 10));
        TextField fi, replace;
        Button prev, next, rep, reAll, close;
        top.getChildren().addAll(fi = new TextField(),
                prev = new Button("Previous"),
                next = new Button("Next"));
        fi.setPromptText("Find");
        below.getChildren().addAll(replace = new TextField(),
                rep = new Button("Replace"),
                reAll = new Button("Replace All"));
        replace.setPromptText("Replace");
        fi.setOnAction((ea) -> {
            next.fire();
        });
        replace.setOnAction((es) -> {
            rep.fire();
        });
        prev.setOnAction((efd) -> {
            int start = area.getSelection().getStart();
            String a = area.getText().substring(0, start);
            int index = a.lastIndexOf(fi.getText());
            if (index != -1) {
                area.selectRange(index, index + fi.getText().length());
            }
        });
        next.setOnAction((sdfsdfsd) -> {
            if (area.getSelection().getLength() == 0) {
                String a = fi.getText();
                int index = area.getText().indexOf(a);
                if (index != -1) {
                    area.selectRange(index, index + a.length());
                }
            } else {
                int end = area.getSelection().getEnd();
                String a = area.getText().substring(end);
                int index = a.indexOf(fi.getText());
                if (index != -1) {
                    index += end;
                    area.selectRange(index, index + fi.getText().length());
                }
            }
        });
        rep.setOnAction((sdfsdfsd) -> {
            String a = fi.getText();
            String b = replace.getText();
            if (area.getText().contains(a)) {
                int index = area.getText().indexOf(a);
                area.replaceText(index, index + a.length(), b);
            }
        });
        reAll.setOnAction((efsf) -> {
            String a = fi.getText();
            String b = replace.getText();
            while (area.getText().contains(a)) {
                int index = area.getText().indexOf(a);
                area.replaceText(index, index + a.length(), b);
            }
        });
        MaterialDesignIconView cl = new MaterialDesignIconView(MaterialDesignIcon.CLOSE);
        if (Preferences.getDarkTheme()) {
            cl.setStyle("-fx-fill:white;");
        }
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            String style = newer ? "-fx-fill:white;" : "";
            cl.setStyle(style);
        });
        border.setRight(close = new Button("", cl));
        BorderPane.setMargin(border.getRight(), new Insets(5, 10, 5, 10));
        close.setOnAction((se) -> {
            call.call(null);
        });
        return border;
    }

    public void save() {
        script.save(area.getText());
        setText(script.getFile().getName());
    }

    private void initFactory() {
        numberFactory = LineNumberFactory.get(area);
        arrowFactory = new ErrorFactory(errorLines);
    }

    private void placeFactory() {
        area.setParagraphGraphicFactory(
                line -> {
                    HBox hbox = new HBox(5, numberFactory.apply(line), arrowFactory.apply(line));
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    return hbox;
                });
    }

    private static void refreshPattern() {
        if (!CURRENT.equals(Compiler.getInstance().getLabels())) {
            CURRENT.clear();
            CURRENT.addAll(Compiler.getInstance().getLabels());
            LABEL_PATTERN = "\\b(" + String.join("|", CURRENT) + ")\\b";
            PATTERN = Pattern.compile(
                    "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<DIRECTIVE>" + DIRECTIVE_PATTERN + ")"
                    + "|(?<REGISTER>" + REGISTER_PATTERN + ")"
                    + "|(?<LABEL>" + LABEL_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<CONSTANT>" + CONSTANT_PATTERN + ")"
            );
        }
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        refreshPattern();
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass
                    = matcher.group("KEYWORD") != null ? "keyword"
                    : matcher.group("REGISTER") != null ? "register"
                    : matcher.group("LABEL") != null ? "label"
                    : matcher.group("COMMENT") != null ? "comment"
                    : matcher.group("CONSTANT") != null ? "hex"
                    : matcher.group("DIRECTIVE") != null ? "directive"
                    : null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
