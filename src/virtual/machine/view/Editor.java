package virtual.machine.view;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import virtual.machine.core.Pair;
import virtual.machine.core.Script;
import virtual.machine.internal.Environment;
import virtual.machine.execution.Compiler;
import virtual.machine.execution.CompilerException;
import virtual.machine.internal.Memory.Data;
import virtual.machine.internal.Registers.Register;
import virtual.machine.core.Strings;

/**
 *
 * @author aniket
 */
public class Editor extends BorderPane {

    private final String DEFAULT_STRING = "init:\n\tirmovq  $0x100, %rsp\n\n\tcall Main\n\thalt\n\n"
            + "Main:\n\n\tret\n\n\n\t.pos 0x100\nStack:\n\n\n\n\n";
    private final TabPane pane;
    private final BorderPane center;
    private final VirtualizedScrollPane virtual;
    private final BorderPane top, left, right, rightTop;
    private final ToolBar icons;
    private final TableView<Data> memory;
    private final TableView<Register> registers;
    private final Environment environment;
    private final CodeArea object;
    private final IntegerProperty counterLine = new SimpleIntegerProperty(0);
    private final BooleanProperty runnable = new SimpleBooleanProperty(false);
    private final ObservableSet<Integer> breakpoints = FXCollections.observableSet(new HashSet<>());
    
    public Editor(Environment environ) {
        environment = environ;
        MenuBar bar = build();
        bar.setUseSystemMenuBar(true);
        pane = new TabPane();
        top = new BorderPane();
        setTop(top);
        top.setCenter(bar);
        icons = new ToolBar();
        ObservableList<MaterialDesignIconView> ico = FXCollections.observableArrayList();
        ico.addAll(new MaterialDesignIconView(MaterialDesignIcon.FOLDER_PLUS),
                new MaterialDesignIconView(MaterialDesignIcon.FOLDER_UPLOAD),
                new MaterialDesignIconView(MaterialDesignIcon.CONTENT_SAVE),
                new MaterialDesignIconView(MaterialDesignIcon.CONTENT_SAVE_ALL),
                new MaterialDesignIconView(MaterialDesignIcon.UNDO),
                new MaterialDesignIconView(MaterialDesignIcon.REDO),
                new MaterialDesignIconView(MaterialDesignIcon.CODE_ARRAY),
                new MaterialDesignIconView(MaterialDesignIcon.PLAY),
                new MaterialDesignIconView(MaterialDesignIcon.FAST_FORWARD),
                new MaterialDesignIconView(MaterialDesignIcon.REPLAY),
                new MaterialDesignIconView(MaterialDesignIcon.STOP));
        ico.stream().map((mid) -> {
            mid.setSize("2em");
            return mid;
        }).map((mid) -> {
            mid.setStyle("-fx-fill:white;");
            return mid;
        }).forEachOrdered((mid) -> {
            icons.getItems().add(new Button("", mid));
        });
        icons.getItems().add(2, new Separator(Orientation.VERTICAL));
        icons.getItems().add(5, new Separator(Orientation.VERTICAL));
        icons.getItems().add(8, new Separator(Orientation.VERTICAL));
        icons.getItems().add(12, new Separator(Orientation.VERTICAL));
        top.setBottom(icons);
        ((Button) icons.getItems().get(0)).setOnAction((e) -> {
            newFile();
        });
        ((Button) icons.getItems().get(1)).setOnAction((e) -> {
            openFile();
        });
        ((Button) icons.getItems().get(3)).setOnAction((e) -> {
            save();
        });
        ((Button) icons.getItems().get(4)).setOnAction((e) -> {
            saveAll();
        });
        ((Button) icons.getItems().get(6)).setOnAction((e) -> {
            undo();
        });
        ((Button) icons.getItems().get(7)).setOnAction((e) -> {
            redo();
        });
        ((Button) icons.getItems().get(9)).setOnAction((e) -> {
            compile();
        });
        ((Button) icons.getItems().get(10)).setOnAction((e) -> {
            run();
        });
        ((Button) icons.getItems().get(11)).setOnAction((e) -> {
            next();
        });
        ((Button) icons.getItems().get(10)).disableProperty().bind(runnable.not());
        ((Button) icons.getItems().get(11)).disableProperty().bind(runnable.not());
        ((Button) icons.getItems().get(13)).setOnAction((e) -> {
            reset();
        });
        ((Button) icons.getItems().get(14)).setOnAction((e) -> {
            stop();
        });
        Tooltip.install(icons.getItems().get(13), new Tooltip("Reset Environment"));
        Tooltip.install(icons.getItems().get(11), new Tooltip("Next Instruction"));
        Tooltip.install(icons.getItems().get(10), new Tooltip("Run Program"));
        Tooltip.install(icons.getItems().get(9), new Tooltip("Compile Program"));
        Tooltip.install(icons.getItems().get(7), new Tooltip("Redo"));
        Tooltip.install(icons.getItems().get(6), new Tooltip("Undo"));
        Tooltip.install(icons.getItems().get(4), new Tooltip("Save All"));
        Tooltip.install(icons.getItems().get(3), new Tooltip("Save"));
        Tooltip.install(icons.getItems().get(1), new Tooltip("Open File"));
        Tooltip.install(icons.getItems().get(0), new Tooltip("New File"));
        memory = new TableView<>();
        memory.setEditable(true);
        registers = new TableView<>();
        setCenter(center = new BorderPane());
        center.setCenter(pane);
        object = new CodeArea();
        object.setEditable(false);
        virtual = new VirtualizedScrollPane(object);
        CursorFactory cursor = new CursorFactory(counterLine);
        object.setParagraphGraphicFactory((line) -> {
            return cursor.apply(line);
        });
        BorderPane.setMargin(pane, new Insets(5));
        BorderPane.setMargin(object, new Insets(5));
        readScripts();
        if (pane.getTabs().isEmpty()) {
            File file = new File("assembly/files", "function.s");
            int count = 1;
            while (file.exists()) {
                file = new File("assembly/files", "function" + count + ".s");
                count++;
            }
            pane.getTabs().addAll(new ScriptTab(new Script(file,
                    DEFAULT_STRING)));
        }
        setOnKeyPressed((e) -> {
            if (e.isControlDown() || e.isMetaDown()) {
                if (null != e.getCode()) {
                    switch (e.getCode()) {
                        case S:
                            if (e.isShiftDown()) {
                                saveAll();
                            } else {
                                save();
                            }
                            break;
                        case O:
                            openFile();
                            break;
                        case N:
                            newFile();
                            break;
                        case Z:
                            undo();
                            break;
                        case Y:
                            redo();
                            break;
                        case X:
                            cut();
                            break;
                        case C:
                            copy();
                            break;
                        case V:
                            paste();
                            break;
                        default:
                            break;
                    }
                }
            }
            
        });
        /**
         * left column all memory
         */
        TableColumn<Data, String> address = new TableColumn<>("Address");
        address.setSortable(false);
        TableColumn<Data, String> value = new TableColumn<>("Value");
        value.setSortable(false);
        memory.getColumns().addAll(address, value);
        address.setCellValueFactory(new PropertyValueFactory("address"));
        value.setCellValueFactory(new PropertyValueFactory("value"));
        left = new BorderPane();
        left.setCenter(memory);
        BorderPane.setMargin(memory, new Insets(5));
        left.setTop(new Label("Memory"));
        BorderPane.setAlignment(left.getTop(), Pos.CENTER);
        BorderPane.setMargin(left.getTop(), new Insets(5));
        setLeft(left);
        memory.setItems(environment.getMemory().getData());
        value.setCellFactory(TextFieldTableCell.<Data>forTableColumn());
        value.setOnEditCommit((CellEditEvent<Data, String> t) -> {
            String p = t.getNewValue();
            if (p.startsWith("0x")) {
                p = p.substring(2);
            }
            try {
                long l = Long.parseLong(p, 16);
                environ.getMemory().putByte(t.getTablePosition().getRow(), (byte) l);
            } catch (NumberFormatException e) {
                t.consume();
            }
            memory.refresh();
        });

        /**
         * bottom all registers
         */
        right = new BorderPane();
        registers.setEditable(true);
        Label titl = new Label("Registers");
        right.setTop(titl);
        TableColumn<Register, String> register = new TableColumn("Register");
        register.setSortable(false);
        TableColumn<Register, String> hex = new TableColumn("Value");
        hex.setSortable(false);
        TableColumn<Register, String> deci = new TableColumn("Decimal");
        deci.setSortable(false);
        registers.getColumns().addAll(register, hex, deci);
        register.setCellValueFactory(new PropertyValueFactory("name"));
        hex.setCellValueFactory(new PropertyValueFactory("hex"));
        deci.setCellValueFactory(new PropertyValueFactory("decimal"));
        BorderPane.setAlignment(titl, Pos.CENTER);
        BorderPane.setMargin(titl, new Insets(5));
        setRight(right);
        BorderPane.setMargin(registers, new Insets(5));
        registers.getItems().addAll(environment.getRegister().registerData());
        hex.setCellFactory(TextFieldTableCell.<Register>forTableColumn());
        hex.setOnEditCommit((CellEditEvent<Register, String> t) -> {
            String p = t.getNewValue();
            if (p.startsWith("0x")) {
                p = p.substring(2);
            }
            try {
                long l = Long.parseLong(p, 16);
                environ.getRegister().setValueInRegister(t.getRowValue().getName().substring(1), l);
            } catch (NumberFormatException e) {
                t.consume();
            }
            registers.refresh();
        });
        deci.setCellFactory(TextFieldTableCell.<Register>forTableColumn());
        deci.setOnEditCommit((CellEditEvent<Register, String> t) -> {
            try {
                long l = Long.parseLong(t.getNewValue());
                environ.getRegister().setValueInRegister(t.getRowValue().getName().substring(1), l);
            } catch (NumberFormatException e) {
                t.consume();
            }
            registers.refresh();
        });

        /**
         * condition codes
         */
        rightTop = new BorderPane();
        VBox flags = new VBox(5);
        Label z = new Label("Zero Flag : " + environment.isZero()),
                s = new Label("Sign Flag : " + environment.isSign()),
                o = new Label("Overflow Flag : " + environment.isOverflow()),
                st = new Label("Status : " + environment.getStatus()),
                pc = new Label("Program Counter : " + Strings.getHex(environment.programCounter(), 4));
        flags.setAlignment(Pos.CENTER);
        flags.setPadding(new Insets(5));
        flags.getChildren().addAll(z, s, o, st, pc);
        rightTop.setCenter(flags);
        environment.zeroProperty().addListener((ob, older, newer) -> {
            z.setText("Zero Flag : " + environment.isZero());
        });
        environment.overflowProperty().addListener((ob, older, newer) -> {
            s.setText("Sign Flag : " + environment.isSign());
        });
        environment.signProperty().addListener((ob, older, newer) -> {
            o.setText("Overflow Flag : " + environment.isOverflow());
        });
        environment.status().addListener((ob, older, newer) -> {
            st.setText("Status : " + environment.getStatus());
        });
        environment.counter().addListener((ob, older, newer) -> {
            pc.setText("Program Counter : " + Strings.getHex(environment.programCounter(), 4));
            alignCounter(newer.intValue());
        });
        environment.setBreakCall((param) -> {
            Platform.runLater(() -> {
                Alert al = new Alert(AlertType.INFORMATION);
                al.initOwner(getScene().getWindow());
                al.initModality(Modality.APPLICATION_MODAL);
                al.setHeaderText("A breakpoint has been encountered");
                ButtonType next, run, halt;
                next = new ButtonType("Next Instruction", ButtonData.OK_DONE);
                run = new ButtonType("Continue", ButtonData.YES);
                halt = new ButtonType("Halt Machine", ButtonData.CANCEL_CLOSE);
                al.getButtonTypes().setAll(next, run, halt);
                al.showAndWait().ifPresent((eb) -> {
                    if (eb.equals(next)) {
                        environment.override();
                        next();
                    } else if (eb.equals(run)) {
                        environment.override();
                        run();
                    } else {
                        System.out.println(environment.getStatus());
                    }
                });
            });
            return null;
        });
        counterLine.addListener((ob, older, newer) -> {
            object.setParagraphGraphicFactory((line) -> {
                return cursor.apply(line);
            });
        });
        right.setTop(rightTop);
        right.setCenter(registers);
        setOnDragOver((event) -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        setOnDragDropped((event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                String filePath = null;
                db.getFiles().stream().filter((file) -> (file.getName().endsWith(".s"))).forEachOrdered((file) -> {
                    loadFile(file);
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
    }
    
    private void alignCounter(int newValue) {
        if (center.getBottom() != null) {
            int num = 0;
            boolean found = false;
            for (int x = 0; x < object.getParagraphs().size(); x++) {
                if (object.getParagraphs().get(x).getText().startsWith(Strings.getHex(newValue, 4))) {
                    found = true;
                    num = x;
                } else {
                    if (found) {
                        break;
                    }
                }
            }
            counterLine.set(num);
        }
    }
    
    private void readScripts() {
        try {
            File config = new File("assembly/config.txt");
            List<String> read = Files.readAllLines(config.toPath());
            File f = new File("assembly", "files");
            if (!f.exists()) {
                f.mkdirs();
            }
            read.forEach((s) -> {
                pane.getTabs().add(new ScriptTab(new Script(new File(s), "")));
            });
        } catch (IOException ex) {
        }
    }
    
    public void saveScripts() {
        File config = new File("assembly/config.txt");
        ArrayList<String> save = new ArrayList<>();
        pane.getTabs().stream().filter((b) -> (b instanceof ScriptTab)).map((b) -> (ScriptTab) b).forEachOrdered((st) -> {
            save.add(st.getScript().getFile().getAbsolutePath());
        });
        try {
            Files.write(config.toPath(), save);
        } catch (IOException ex) {
        }
    }
    
    private Optional<ScriptTab> getSelectedTab() {
        Tab b = pane.getSelectionModel().getSelectedItem();
        if (b != null) {
            ScriptTab sc = (ScriptTab) b;
            return Optional.of(sc);
        }
        return Optional.empty();
    }
    
    public void save() {
        getSelectedTab().ifPresent((e) -> {
            e.save();
        });
    }
    
    public void undo() {
        getSelectedTab().ifPresent((e) -> {
            e.undo();
        });
    }
    
    public void redo() {
        getSelectedTab().ifPresent((e) -> {
            e.redo();
        });
    }
    
    public void cut() {
        getSelectedTab().ifPresent((e) -> {
            e.cut();
        });
    }
    
    public void copy() {
        getSelectedTab().ifPresent((e) -> {
            e.copy();
        });
    }
    
    public void paste() {
        getSelectedTab().ifPresent((e) -> {
            e.paste();
        });
    }
    
    public void selectAll() {
        getSelectedTab().ifPresent((e) -> {
            e.selectAll();
        });
    }
    
    public void saveAll() {
        pane.getTabs().stream().filter((b) -> (b instanceof ScriptTab)).map((b) -> (ScriptTab) b).forEachOrdered((sc) -> {
            sc.save();
        });
    }
    
    public void next() {
        if (runnable.get()) {
            environment.nextInstruction((param) -> {
                refresh();
                return null;
            }, breakpoints);
        }
    }
    
    public void run() {
        if (runnable.get()) {
            environment.run((param) -> {
                refresh();
                return null;
            }, breakpoints);
        }
    }
    
    private void refresh() {
        memory.refresh();
        registers.refresh();
    }
    
    public void stop() {
        environment.setStatus(1);
        runnable.set(false);
    }
    
    public void compile() {
        save();
        reset();
        getSelectedTab().ifPresent((ef) -> {
            try {
                ArrayList<Pair<String, ArrayList<Byte>>> interpret = Compiler.getInstance().compile(ef.getScript().getCurrentCode());
                int loc = 0;
                ObservableSet<Integer> breakp = FXCollections.observableSet();
                ObservableSet<Integer> actualBreakpoints = FXCollections.observableSet(new HashSet<>());
                getSelectedTab().ifPresent((efa) -> {
                    breakp.addAll(efa.getBreakpoints());
                });
                int line = 0;
                object.replaceText("");
                for (Pair<String, ArrayList<Byte>> p : interpret) {
                    object.appendText(Strings.getHex(loc, 4) + "\t");
                    String value = "";
                    boolean notAllZeros = false;
                    if (breakp.contains(line)) {
                        actualBreakpoints.add(loc);
                    }
                    for (Byte b : p.getValue()) {
                        value += Strings.getHexMinusPrefix(b & 0xFF, 2);
                        environment.getMemory().putByte(loc, b);
                        if (b != 0) {
                            notAllZeros = true;
                        }
                        loc++;
                    }
                    if (notAllZeros) {
                        while (value.length() < 20) {
                            value += " ";
                        }
                    } else {
                        value = "                    ";
                    }
                    breakpoints.clear();
                    breakpoints.addAll(actualBreakpoints);
                    object.appendText(value);
                    object.appendText("\t|\t");
                    if (!p.getKey().endsWith(":")) {
                        object.appendText("\t");
                    }
                    object.appendText(p.getKey() + "\n");
                    line++;
                }
                virtual.setMinHeight(getScene().getHeight() / 2);
                center.setBottom(virtual);
                alignCounter(0);
                runnable.set(true);
                memory.refresh();
                environment.reset();
                fadingNotification(pane, "Compilation Successful");
            } catch (CompilerException ex) {
                fadingNotification(pane, "Compilation Failed\n" + ex.getMessage());
            }
        });
    }
    
    public void reset() {
        environment.getRegister().reset();
        environment.getMemory().reset();
        refresh();
        environment.reset();
        if (center.getBottom() != null) {
            center.setBottom(null);
        }
        runnable.set(false);
    }
    
    public void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Files");
        fc.setInitialDirectory(new File("assembly", "files"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly File", "*.s"));
        File open = fc.showOpenDialog(getScene().getWindow());
        if (open != null) {
            loadFile(open);
        }
    }
    
    private void loadFile(File open) {
        Script sca = new Script(open, "");
        boolean go = true;
        for (Tab b : pane.getTabs()) {
            if (b instanceof ScriptTab) {
                ScriptTab sc = (ScriptTab) b;
                if (sc.getScript().equals(sca)) {
                    go = false;
                }
            }
        }
        if (go) {
            pane.getTabs().add(new ScriptTab(sca));
        }
    }
    
    public void newFile() {
        TextInputDialog tid = new TextInputDialog("assembly");
        tid.initOwner(getScene().getWindow());
        tid.initModality(Modality.APPLICATION_MODAL);
        tid.setTitle("File Name");
        tid.setHeaderText("Enter Filename");
        tid.showAndWait().ifPresent((ef) -> {
            if (ef.endsWith(".s")) {
                ef = ef + ".s";
            }
            File f = new File("assembly", "files");
            File fa = new File(f, ef);
            try {
                Files.createFile(fa.toPath());
                pane.getTabs().add(new ScriptTab(new Script(fa, "")));
            } catch (IOException ex) {
            }
        });
    }
    
    private MenuBar build() {
        MenuBar bar = new MenuBar();
        bar.getMenus().addAll(new Menu("File"), new Menu("Edit"),
                new Menu("Run"));
        bar.getMenus().get(0).getItems().addAll(new MenuItem("New File"),
                new MenuItem("Open File"), new MenuItem("Save"), new MenuItem("Save All"));
        bar.getMenus().get(0).getItems().get(0).setOnAction((e) -> {
            newFile();
        });
        bar.getMenus().get(0).getItems().get(1).setOnAction((ActionEvent e) -> {
            openFile();
        });
        bar.getMenus().get(0).getItems().get(2).setOnAction((e) -> {
            save();
        });
        bar.getMenus().get(0).getItems().get(2).setOnAction((e) -> {
            saveAll();
        });
        bar.getMenus().get(1).getItems().addAll(new MenuItem("Undo"),
                new MenuItem("Redo"), new MenuItem("Cut"), new MenuItem("Copy"),
                new MenuItem("Paste"), new MenuItem("Select All"));
        bar.getMenus().get(1).getItems().get(0).setOnAction((E) -> {
            undo();
        });
        bar.getMenus().get(1).getItems().get(1).setOnAction((E) -> {
            redo();
        });
        bar.getMenus().get(1).getItems().get(2).setOnAction((E) -> {
            cut();
        });
        bar.getMenus().get(1).getItems().get(3).setOnAction((E) -> {
            copy();
        });
        bar.getMenus().get(1).getItems().get(4).setOnAction((E) -> {
            paste();
        });
        bar.getMenus().get(1).getItems().get(5).setOnAction((E) -> {
            selectAll();
        });
        bar.getMenus().get(2).getItems().addAll(new MenuItem("Compile"),
                new MenuItem("Next Instruction"),
                new MenuItem("Run"),
                new MenuItem("Reset"),
                new MenuItem("Stop"));
        bar.getMenus().get(2).getItems().get(0).setOnAction((e) -> {
            compile();
        });
        bar.getMenus().get(2).getItems().get(2).setOnAction((e) -> {
            run();
        });
        bar.getMenus().get(2).getItems().get(1).setOnAction((e) -> {
            next();
        });
        bar.getMenus().get(2).getItems().get(3).setOnAction((e) -> {
            reset();
        });
        bar.getMenus().get(2).getItems().get(4).setOnAction((E) -> {
            stop();
        });
        return bar;
    }
    
    public static void fadingNotification(Node node, String message) {
        Font font = Font.font("Verdana", FontWeight.NORMAL, 20);
        Color boxColor = Color.GREY;
        Color textColor = Color.WHITE;
        double arcH = 5;
        double arcW = 5;
        final Rectangle rectangle = new Rectangle();
        final Text text = new Text(message);
        double x = 0;
        double y = 0;
        text.setLayoutX(x);
        text.setLayoutY(y);
        text.setFont(font);
        text.setFill(textColor);
        text.setTextAlignment(TextAlignment.CENTER);
        Scene scene = node.getScene();
        final Parent p = scene.getRoot();
        if (p instanceof Group) {
            Group group = (Group) p;
            group.getChildren().add(rectangle);
            group.getChildren().add(text);
        } else if (p instanceof Pane) {
            Pane group = (Pane) p;
            group.getChildren().add(rectangle);
            group.getChildren().add(text);
        }
        Bounds bounds = text.getBoundsInParent();
        double sWidth = scene.getWidth();
        double sHeight = scene.getHeight();
        x = sWidth / 2 - (bounds.getWidth() / 2);
        y = sHeight / 2 - (bounds.getHeight() / 2);
        text.setLayoutX(x);
        text.setLayoutY(y);
        bounds = text.getBoundsInParent();
        double baseLineOffset = text.getBaselineOffset();
        rectangle.setFill(boxColor);
        rectangle.setLayoutX(x - arcW);
        rectangle.setLayoutY(y - baseLineOffset - arcH);
        rectangle.setArcHeight(arcH);
        rectangle.setArcWidth(arcW);
        rectangle.setWidth(bounds.getWidth() + arcW * 2);
        rectangle.setHeight(bounds.getHeight() + arcH * 2);
        FadeTransition ft = new FadeTransition(
                Duration.millis(4500), rectangle);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.play();
        ft.setOnFinished((ev) -> {
            if (p instanceof Group) {
                Group group = (Group) p;
                group.getChildren().remove(rectangle);
                group.getChildren().remove(text);
            } else if (p instanceof Pane) {
                Pane group = (Pane) p;
                group.getChildren().remove(rectangle);
                group.getChildren().remove(text);
            }
        });
        FadeTransition ft2 = new FadeTransition(
                Duration.millis(5000), text);
        ft2.setFromValue(1.0);
        ft2.setToValue(0.0);
        ft2.play();
    }
    
}
