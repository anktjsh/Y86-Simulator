package virtual.machine.view;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import virtual.machine.core.Pair;
import virtual.machine.core.Script;
import virtual.machine.execution.Compiler;
import virtual.machine.execution.ConcurrentCompiler;

/**
 *
 * @author aniket
 */
public class ScriptTab extends Tab {
    
    private final CodeArea area;
    private final Script script;
    
    private final ObservableSet<Integer> breakpoints = FXCollections.observableSet(new HashSet<>());
    private final Pair<Integer, String> errorLines = new Pair<>(-1, "");
    private IntFunction<Node> numberFactory;
    private IntFunction<Node> arrowFactory;
    private IntFunction<Node> breakFactory;
    
    private final IntegerProperty rowPosition;
    
    private static final String[] KEYWORDS = new String[]{
        "jl", "subq", "jge", "cmovge", "jmp", "nop",
        "xorq", "cmovg", "cmove", "andq",
        "cmovl", "addq", "rrmovq",
        "irmovq", "jne", "ret", "jle", "rmmovq",
        "cmovne", "cmovle", "call",
        "halt", "popq", "pushq", "mrmovq", "je", "jg",
        "multq", "divq", "modq", "sarq", "shrq", "salq", "orq",
        "incq", "decq", "notq", "negq", "bangq"
    };
    
    private static final String[] DIRECTIVES = new String[]{
        "align", "pos", "quad"
    };
    
    private static final String[] REGISTERS = new String[]{
        "rax", "rcx", "rdx", "rsp", "rbp", "r8", "r9", "r10",
        "r11", "r12", "r13", "r14", "rbx", "rsi", "rdi"
    };
    
    private static final Set<String> CURRENT = new HashSet<>(Compiler.getInstance().getLabels());
    
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String REGISTER_PATTERN = "\\b(" + String.join("|", REGISTERS) + ")\\b";
    private static String LABEL_PATTERN = "\\b(" + String.join("|", CURRENT) + ")\\b";
    private static final String COMMENT_PATTERN = "#[^\n]*";
    private static final String CONSTANT_PATTERN = "\\$";
    private static final String DIRECTIVE_PATTERN = "\\b(" + String.join("|", DIRECTIVES) + ")\\b";
    
    private static Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<DIRECTIVE>" + DIRECTIVE_PATTERN + ")"
            + "|(?<REGISTER>" + REGISTER_PATTERN + ")"
            + "|(?<LABEL>" + LABEL_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<CONSTANT>" + CONSTANT_PATTERN + ")"
    );
    
    public ObservableSet<Integer> getBreakpoints() {
        return breakpoints;
    }
    
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
        area.getParagraphs().addListener((ListChangeListener.Change<? extends Paragraph<Collection<String>, String, Collection<String>>> c) -> {
            c.next();
            breakpoints.stream().filter((n) -> (c.getList().size() <= n || c.getList().get(n).getText().isEmpty())).forEachOrdered((n) -> {
                breakpoints.remove(n);
            });
        });
        area.caretPositionProperty().addListener((ob, older, newer) -> {
            rowPosition.set(getRow(area.getCaretPosition()));
        });
        breakpoints.addListener((SetChangeListener.Change<? extends Integer> c) -> {
            placeFactory();
            script.saveBreakpoints(breakpoints);
        });
        area.setStyle("-fx-font-size:15;-fx-font-weight:bold;");
        setOnCloseRequest((e) -> {
            if (getText().endsWith("*")) {
                Alert al = new Alert(Alert.AlertType.CONFIRMATION);
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
        setContent(new VirtualizedScrollPane(area));
        initFactory();
        placeFactory();
        area.getStylesheets().add(getClass().getResource("css.css").toExternalForm());
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
                new MenuItem("Select All"),
                new MenuItem("Insert Breakpoint"),
                new MenuItem("Remove Breakpoint"));
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
        area.getContextMenu().getItems().get(6).setOnAction((e) -> {
            addBreakpoint();
        });
        area.getContextMenu().getItems().get(7).setOnAction((e) -> {
            breakpoints.remove(rowPosition.get());
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
        breakpoints.addAll(script.getBreakpoints());
    }
    
    private void addBreakpoint() {
        String[] spl = area.getText().split("\n");
        if (rowPosition.get() < spl.length && !spl[rowPosition.get()].trim().isEmpty()) {
            Scanner sc = new Scanner(spl[rowPosition.get()]);
            String word = sc.next();
            if (!word.endsWith(":") && !word.startsWith(".")) {
                breakpoints.add(rowPosition.get());
            }
        }
    }
    
    public final void concurrent(String s) {
        ConcurrentCompiler.compile(s, (Pair<Integer, String> param) -> {
            if (param != null) {
                errorLines.setKey(param.getKey());
                errorLines.setValue(param.getValue());
            } else {
                errorLines.setKey(-1);
                errorLines.setValue("");
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
    
    public void save() {
        script.save(area.getText());
        setText(script.getFile().getName());
    }
    
    private void initFactory() {
        numberFactory = LineNumberFactory.get(area);
        arrowFactory = new ErrorFactory(errorLines);
        breakFactory = new BreakPointFactory(breakpoints);
    }
    
    private void placeFactory() {
        area.setParagraphGraphicFactory(
                line -> {
                    HBox hbox = new HBox(5, numberFactory.apply(line), breakFactory.apply(line), arrowFactory.apply(line));
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
