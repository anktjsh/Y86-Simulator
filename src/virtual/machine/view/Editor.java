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
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import static virtual.machine.Y86VM.ICON;
import virtual.machine.core.Pair;
import virtual.machine.core.Script;
import virtual.machine.internal.Environment;
import virtual.machine.execution.Compiler;
import virtual.machine.execution.CompilerException;
import virtual.machine.internal.Memory.Data;
import virtual.machine.internal.Registers.Register;
import virtual.machine.core.Strings;
import virtual.machine.internal.Environment.Condition;

/**
 *
 * @author aniket
 */
public class Editor extends BorderPane {

    private final String DEFAULT_STRING = "init:\n\tirmovq  $0x100, %rsp\n\n\tcall Main\n\thalt\n\n"
            + "Main:\n\n\tret\n\n\n\t.pos 0x100\nStack:\n\n\n\n\n";
    private final TabPane pane;
    private final BorderPane center;
    private final BorderPane top, left, bottom;
    private final ToolBar icons;
    private final TableView<Data> memory;
    private final TableView<Register> registers;
    private final Environment environment;
    private final BooleanProperty runnable = new SimpleBooleanProperty(false);
    private Terminal term;
    private final ObservableSet<Integer> breakpoints = FXCollections.observableSet(new HashSet<>());

    public Editor(Environment environ) {
        environment = environ;
        MenuBar bar = build();
        bar.setUseSystemMenuBar(true);
        pane = new TabPane();
        pane.getSelectionModel().selectedItemProperty().addListener((ob, older, newer) -> {
            if (runnable.get()) {
                reset();
                if (older instanceof ScriptTab) {
                    ((ScriptTab) older).reset();
                }
            }
        });
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
                new MaterialDesignIconView(MaterialDesignIcon.ANDROID_STUDIO),
                new MaterialDesignIconView(MaterialDesignIcon.PLAY),
                new MaterialDesignIconView(MaterialDesignIcon.FAST_FORWARD),
                new MaterialDesignIconView(MaterialDesignIcon.REPLAY),
                new MaterialDesignIconView(MaterialDesignIcon.STOP));
        String style = Preferences.getDarkTheme() ? "-fx-fill:white;" : "";
        ico.stream().map((ic) -> {
            ic.setSize("2em");
            return ic;
        }).map((ic) -> {
            ic.setStyle(style);
            return ic;
        }).forEachOrdered((ic) -> {
            icons.getItems().add(new Button("", ic));
        });
        MaterialDesignIconView cons = new MaterialDesignIconView(MaterialDesignIcon.CONSOLE);
        cons.setStyle(style);
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            String st = newer ? "-fx-fill:white;" : "";
            ico.forEach((ic) -> {
                ic.setStyle(st);
            });
            cons.setStyle(st);
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
        Tooltip.install(icons.getItems().get(14), new Tooltip("Stop Execution"));
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
        BorderPane.setMargin(pane, new Insets(5));
        readScripts();
        if (pane.getTabs().isEmpty()) {
            File file = new File(Preferences.getFileDirectory(), "function.ys");
            if (!file.exists()) {
                pane.getTabs().addAll(new ScriptTab(new Script(file,
                        DEFAULT_STRING), environ));
            }
        }
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
        VBox right = new VBox(5);
        Label pc = new Label("Program Counter : " + Strings.getHex(environment.programCounter(), 4));
        Label st = new Label("Status : " + environment.getStatus());
        right.getChildren().addAll(pc, st);
        right.setAlignment(Pos.TOP_CENTER);
        right.setPadding(new Insets(5));
        registers.setEditable(true);
        Label titl = new Label("Registers");
        right.getChildren().add(titl);
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
        setRight(right);
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
        TableView<Condition> codes = new TableView<>();
        TableColumn nam = new TableColumn("Condition Code");
        nam.setSortable(false);
        TableColumn<Condition, String> cod = new TableColumn<>("Value");
        cod.setSortable(false);
        cod.setCellValueFactory(new PropertyValueFactory("stateValue"));
        nam.setCellValueFactory(new PropertyValueFactory("name"));
        codes.getColumns().addAll(nam, cod);
        codes.getItems().addAll(environment.getCodes());
        environment.zeroProperty().addListener((ob, older, newer) -> {
            codes.refresh();
        });
        environment.overflowProperty().addListener((ob, older, newer) -> {
            codes.refresh();
        });
        environment.signProperty().addListener((ob, older, newer) -> {
            codes.refresh();
        });
        environment.carryProperty().addListener((ob, older, newer) -> {
            codes.refresh();
        });
        environment.status().addListener((ob, older, newer) -> {
            Platform.runLater(() -> st.setText("Status : " + environment.getStatus()));
        });
        environment.counter().addListener((ob, older, newer) -> {
            Platform.runLater(()
                    -> pc.setText("Program Counter : " + Strings.getHex(environment.programCounter(), 4)));
        });
        environment.setBreakCall((param) -> {
            Platform.runLater(() -> {
                Alert al = new Alert(AlertType.INFORMATION);
                ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
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
                    }
                });
            });
            return null;
        });
        right.getChildren().add(registers);
        right.getChildren().add(new Label("Condition Codes"));
        right.getChildren().add(codes);
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
                db.getFiles().stream().filter((file) -> (file.getName().endsWith(".ys"))).forEachOrdered((file) -> {
                    readFile(file);
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });
        bottom = new BorderPane();
        center.setBottom(bottom);
        Button console = new Button("Console", cons);
        BorderPane.setMargin(bottom, new Insets(5));
        BorderPane.setAlignment(bottom, Pos.CENTER_LEFT);
        bottom.setTop(console);
        term = new Terminal(environ);
        console.setOnAction((E) -> {
            if (bottom.getCenter() == null) {
                bottom.setCenter(term);
            } else {
                bottom.setCenter(null);
            }
        });
        bottom.setCenter(term);
    }

    private void readScripts() {
        try {
            File config = new File("assembly/config.txt");
            List<String> read = Files.readAllLines(config.toPath());
            File f = new File(Preferences.getFileDirectory());
            if (!f.exists()) {
                f.mkdirs();
            }
            read.forEach((s) -> {
                pane.getTabs().add(new ScriptTab(new Script(new File(s), ""), environment));
            });
            pane.getSelectionModel().select(pane.getTabs().get(pane.getTabs().size() - 1));
        } catch (IOException ex) {
        }
    }

    public void saveScripts() {
        File config = new File("assembly/config.txt");
        ArrayList<String> save = new ArrayList<>();
        pane.getTabs().stream().filter((b) -> (b instanceof ScriptTab)).map((b) -> (ScriptTab) b).forEachOrdered((st) -> {
            save.add(st.getScript().getFile().getAbsolutePath());
        });
        System.out.println(save);
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

    public void find() {
        getSelectedTab().ifPresent((e) -> {
            e.find();
        });
    }

    public void replace() {
        getSelectedTab().ifPresent((e) -> {
            e.replace();
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
            }, breakpoints, false);
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
                ArrayList<Byte> all = new ArrayList<>();
                int loc = 0;
                ObservableSet<Integer> actualBreakpoints = FXCollections.observableSet(new HashSet<>());
                int line = 0;
                StringBuilder sb = new StringBuilder();
                for (Pair<String, ArrayList<Byte>> p : interpret) {
                    sb.append(Strings.getHex(loc, 4)).append("\t");
                    String value = "";
                    boolean notAllZeros = false;
                    for (Byte b : p.getValue()) {
                        if (b == 0x01 && p.getKey().trim().equals("brk")) {
                            actualBreakpoints.add(loc);
                        } else {
                            all.add(b);
                        }
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
                    sb.append(value);
                    sb.append("\t|\t");
                    if (!p.getKey().endsWith(":")) {
                        sb.append("\t");
                    }
                    sb.append(p.getKey()).append("\n");
                    line++;
                }
                ef.setObjectText(sb.toString());
                ef.alignCounter(0);
                runnable.set(true);
                memory.refresh();
                fadingNotification(pane, "Compilation Successful");
                File obj = new File(ef.getScript().getFile().getParentFile(), ef.getScript().getFile().getName().substring(0, ef.getScript().getFile().getName().indexOf(".")) + ".yo");
                byte[] arr = new byte[all.size()];
                for (int x = 0; x < arr.length; x++) {
                    arr[x] = all.get(x);
                }
                try {
                    Files.write(obj.toPath(), arr);
                } catch (IOException ex) {
                }
            } catch (CompilerException ex) {
                fadingNotification(pane, "Compilation Failed");
            }
        });
    }

    public void reset() {
        environment.getRegister().reset();
        environment.getMemory().reset();
        refresh();
        environment.reset();
        getSelectedTab().ifPresent((e) -> {
            e.reset();
        });
        runnable.set(false);
        term.clear();
    }

    public void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Files");
        fc.setInitialDirectory(new File(Preferences.getFileDirectory()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly File", "*.ys"));
        File open = fc.showOpenDialog(getScene().getWindow());
        if (open != null) {
            readFile(open);
        }
    }

    public void loadFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Files");
        fc.setInitialDirectory(new File(Preferences.getFileDirectory()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Assembly Object File", "*.yo"));
        File open = fc.showOpenDialog(getScene().getWindow());
        if (open != null) {
            try {
                byte[] read = Files.readAllBytes(open.toPath());
                for (int x = 0; x < read.length; x++) {
                    environment.getMemory().putByte(x, read[x]);
                }
                runnable.set(true);
                memory.refresh();
                environment.reset();
            } catch (IOException ex) {
            }
        }
    }

    private void readFile(File open) {
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
            pane.getTabs().add(new ScriptTab(sca, environment));
            pane.getSelectionModel().select(pane.getTabs().get(pane.getTabs().size() - 1));
        }
    }

    public void newFile() {
        TextInputDialog tid = new TextInputDialog("assembly");
        ((Stage) tid.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
        tid.initOwner(getScene().getWindow());
        tid.initModality(Modality.APPLICATION_MODAL);
        tid.setTitle("File Name");
        tid.setHeaderText("Enter Filename");
        tid.showAndWait().ifPresent((ef) -> {
            if (!ef.endsWith(".ys")) {
                ef = ef + ".ys";
            }
            File f = new File(Preferences.getFileDirectory());
            File fa = new File(f, ef);
            try {
                Files.createFile(fa.toPath());
                pane.getTabs().add(new ScriptTab(new Script(fa, ""), environment));
                pane.getSelectionModel().select(pane.getTabs().get(pane.getTabs().size() - 1));
            } catch (IOException ex) {
            }
        });
    }

    private MenuBar build() {
        MenuBar bar = new MenuBar();
        bar.getMenus().addAll(new Menu("File"), new Menu("Edit"),
                new Menu("Run"));
        bar.getMenus().get(0).getItems().addAll(new MenuItem("New File"),
                new MenuItem("Open File"),
                new MenuItem("Load Object File"),
                new MenuItem("Save"), new MenuItem("Save All"));
        bar.getMenus().get(0).getItems().get(0).setOnAction((e) -> {
            newFile();
        });
        bar.getMenus().get(0).getItems().get(1).setOnAction((ActionEvent e) -> {
            openFile();
        });
        bar.getMenus().get(0).getItems().get(2).setOnAction((ActionEvent e) -> {
            loadFile();
        });
        bar.getMenus().get(0).getItems().get(3).setOnAction((e) -> {
            if (!term.isFocused()) {
                save();
            }
        });
        bar.getMenus().get(0).getItems().get(4).setOnAction((e) -> {
            if (!term.isFocused()) {
                saveAll();
            }
        });
        bar.getMenus().get(1).getItems().addAll(new MenuItem("Undo"),
                new MenuItem("Redo"), new MenuItem("Cut"), new MenuItem("Copy"),
                new MenuItem("Paste"), new MenuItem("Select All"),
                new MenuItem("Find"), new MenuItem("Replace"));
        bar.getMenus().get(1).getItems().get(0).setOnAction((E) -> {
            if (!term.isFocused()) {
                undo();
            }
        });
        bar.getMenus().get(1).getItems().get(1).setOnAction((E) -> {
            if (!term.isFocused()) {
                redo();
            }
        });
        bar.getMenus().get(1).getItems().get(2).setOnAction((E) -> {
            if (!term.isFocused()) {
                cut();
            }
        });
        bar.getMenus().get(1).getItems().get(3).setOnAction((E) -> {
            if (!term.isFocused()) {
                copy();
            }
        });
        bar.getMenus().get(1).getItems().get(4).setOnAction((E) -> {
            if (!term.isFocused()) {
                paste();
            }
        });
        bar.getMenus().get(1).getItems().get(5).setOnAction((E) -> {
            if (!term.isFocused()) {
                selectAll();
            }
        });
        bar.getMenus().get(1).getItems().get(6).setOnAction((E) -> {
            if (!term.isFocused()) {
                find();
            }
        });
        bar.getMenus().get(1).getItems().get(7).setOnAction((E) -> {
            if (!term.isFocused()) {
                replace();
            }
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
        bar.getMenus().get(0).getItems().get(0).setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(0).getItems().get(1).setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(0).getItems().get(2).setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        bar.getMenus().get(0).getItems().get(3).setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(0).getItems().get(4).setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        bar.getMenus().get(1).getItems().get(0).setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(1).setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(2).setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(3).setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(4).setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(5).setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(6).setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
        bar.getMenus().get(1).getItems().get(7).setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            bar.getMenus().get(0).getItems().addAll(new MenuItem("Preferences"),
                    new MenuItem("About"));
            bar.getMenus().get(0).getItems().get(5).setOnAction((e) -> {
                if (pref == null) {
                    pref = new Preferences((Stage) getScene().getWindow());
                }
                pref.showAndWait();
            });
            bar.getMenus().get(0).getItems().get(6).setOnAction((e) -> {
                Alert al = new Alert(AlertType.INFORMATION);
                ((Stage) al.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
                al.initModality(Modality.APPLICATION_MODAL);
                al.initOwner(getScene().getWindow());
                al.setTitle("About Y86VM");
                al.setHeaderText("Y86-64 VM Created by Aniket Joshi (2018)");
                al.setContentText("Y86-64 VM supports all standard Y86-64 operations with additional single operand and double operand mathematical functions, breakpoints,"
                        + " as well as the Carry Flag and all associated jumps and conditional moves for unsigned operations.\n\n"
                        + "Y86-64 VM was written in Java with the help of JavaFx, RichTextFx, NSMenuFx, and FontAwesomeFx");
                al.showAndWait();
            });
        }
        return bar;
    }

    private Preferences pref;

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
