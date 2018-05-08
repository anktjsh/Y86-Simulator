package virtual.machine.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import virtual.machine.Y86VM;
import static virtual.machine.Y86VM.CSS;

/**
 *
 * @author aniket
 */
public class Preferences extends Stage {

    private static final BooleanProperty DARK_THEME = new SimpleBooleanProperty(false);
    private static final IntegerProperty FONT_SIZE = new SimpleIntegerProperty(13);
    private static final StringProperty FONT_NAME = new SimpleStringProperty("System Regular");
    private static final StringProperty FILE_DIRECTORY = new SimpleStringProperty(new File("").getAbsolutePath() + "/assembly/files");

    static {
        DARK_THEME.addListener((ob, older, newer) -> write());
        FONT_SIZE.addListener((ob, older, newer) -> write());
        FONT_NAME.addListener((ob, older, newer) -> write());
        FILE_DIRECTORY.addListener((ob, older, newer) -> write());
        read();
    }

    static void read() {
        File f = new File("assembly", "settings.txt");
        if (f.exists()) {
            try {
                Scanner in = new Scanner(f);
                if (in.hasNextLine()) {
                    DARK_THEME.set(Boolean.parseBoolean(in.nextLine()));
                }
                if (in.hasNextLine()) {
                    try {
                        FONT_SIZE.set(Integer.parseInt(in.nextLine()));
                    } catch (NumberFormatException e) {
                    }
                }
                if (in.hasNextLine()) {
                    String s = in.nextLine();
                    if (Font.getFamilies().contains(s)) {
                        FONT_NAME.set(s);
                    }
                }
                if (in.hasNextLine()) {
                    String s = in.nextLine();
                    File fa = new File(s);
                    if (fa.exists() && fa.isDirectory()) {
                        FILE_DIRECTORY.set(s);
                    }
                }
            } catch (FileNotFoundException ex) {
            }
        }

    }

    static void write() {
        File f = new File("assembly", "settings.txt");
        if (!f.getParentFile().exists()) {
            f.mkdirs();
        }
        try {
            Files.write(f.toPath(), FXCollections.observableArrayList(DARK_THEME.get() + "", FONT_SIZE.intValue() + "", FONT_NAME.get(),
                    FILE_DIRECTORY.get()));
        } catch (IOException ex) {
        }
    }

    public static void setFileDirectory(String text) {
        FILE_DIRECTORY.set(text);
        write();
    }

    private ToggleButton darkOption;
    private Spinner<Integer> fontOption;
    private ComboBox<String> font;
    private Button choose;
    private TextField dir;

    public Preferences(Stage stage) {
        initOwner(stage);
        setTitle("Preferences");
        initModality(Modality.APPLICATION_MODAL);
        setScene(buildScene());
        setResizable(false);
        if (Preferences.getDarkTheme()) {
            getScene().getStylesheets().add(CSS);
        }
        getIcons().add(Y86VM.ICON);
        Preferences.darkTheme().addListener((ob, older, newer) -> {
            if (newer) {
                getScene().getStylesheets().add(CSS);
            } else {
                getScene().getStylesheets().remove(CSS);
            }
        });
    }

    private Scene buildScene() {
        TabPane tabs = new TabPane();
        BorderPane root = new BorderPane();
        BorderPane ext = new BorderPane();
        tabs.getTabs().addAll(new Tab("Editor", root),
                new Tab("Extensions", ext));
        VBox controls = new VBox(15);
        controls.setPadding(new Insets(25));
        controls.setAlignment(Pos.CENTER);
        root.setCenter(controls);
        controls.getChildren().addAll(
                choose = new Button("Select File Directory"),
                dir = new TextField(getFileDirectory()),
                darkOption = new ToggleButton("Dark Theme"),
                new Label("Font Size"),
                fontOption = new Spinner<>(5, 50, getFontSize(), 1),
                new Label("Font"),
                font = new ComboBox<>(FXCollections.observableArrayList(Font.getFamilies())));
        font.setValue(FONT_NAME.get());
        darkOption.setSelected(getDarkTheme());
        FILE_DIRECTORY.bind(dir.textProperty());
        DARK_THEME.bind(darkOption.selectedProperty());
        FONT_SIZE.bind(fontOption.valueProperty());
        FONT_NAME.bind(font.valueProperty());
        dir.setEditable(false);
        choose.setOnAction((e) -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setInitialDirectory(new File(getFileDirectory()));
            dc.setTitle("Select File Directory");
            File show = dc.showDialog(this);
            if (show != null) {
                Alert al = new Alert(AlertType.CONFIRMATION);
                al.initOwner(getScene().getWindow());
                al.initModality(Modality.APPLICATION_MODAL);
                al.setHeaderText("Would you like to move all source files from the current directory to the new directory?");
                al.showAndWait().ifPresent((ee) -> {
                    if (ee == ButtonType.OK) {
                        File old = new File(FILE_DIRECTORY.get());
                        for (File f : old.listFiles()) {
                            if (f.getName().endsWith(".ys")) {
                                try {
                                    Files.move(f.toPath(), new File(show, f.getName()).toPath());
                                } catch (IOException ex) {
                                }
                            }
                        }
                    }
                });
                dir.setText(show.getAbsolutePath());
            }
        });
        TableView<Instruction> table = new TableView<>();
        ext.setCenter(table);
        ext.setPadding(new Insets(5));
        TableColumn<Instruction, String> ins = new TableColumn<>("Instruction");
        TableColumn<Instruction, String> opr = new TableColumn<>("Implementation");
        TableColumn<Instruction, String> usa = new TableColumn<>("Usage");
        ins.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        opr.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        usa.setCellValueFactory(new PropertyValueFactory<>("usage"));
        ins.setSortable(false);
        opr.setSortable(false);
        usa.setSortable(false);
        table.getColumns().addAll(ins, opr, usa);
        table.getItems().addAll(
                new Instruction("brk", "Sets a breakpoint at this location", "brk #placed on its own line"),
                new Instruction("cmovb", "Conditional Move if below", "cmovb %r10, %r11"),
                new Instruction("cmovnb", "Conditional Move if not below", "cmovnb %r10, %r11"),
                new Instruction("cmovbe", "Conditional Move if below or equal", "cmovbe %r10, %r11"),
                new Instruction("cmova", "Conditional Move if above", "cmova %r10, %r11"),
                new Instruction("imultq", "Multiply", "imultq %r10, %r11"),
                new Instruction("divq", "Divide","divq %r10, %r11"),
                new Instruction("modq", "Modulus", "modq %r10, %r11"),
                new Instruction("sarq", "Arithmetic Right Shift", "sarq %r10, %r11"),
                new Instruction("shrq", "Logical Right Shift", "shrq %r10, %r11"),
                new Instruction("salq", "Arithmetic Left Shift", "salq %r10, %r11"),
                new Instruction("orq", "Bitwise OR", "orq %r10, %r11"),
                new Instruction("jb", "Jump if below", "jb Label"),
                new Instruction("jnb", "Jump if not below", "jnb Label"),
                new Instruction("jbe", "Jump if below or equal", "jbe Label"),
                new Instruction("ja", "Jump if above", "ja Label"),
                new Instruction("notq", "Bitwise NOT", "notq %r10"),
                new Instruction("negq", "2's complement negation", "negq %r10"),
                new Instruction("incq", "Increment", "incq %r10"),
                new Instruction("decq", "Decrement", "decq %r10"),
                new Instruction("bangq", "1 if input is 0, 0 otherwise", "bangq %r10"),
                new Instruction("getc", "Places a character from the console into register", "getc %r10"),
                new Instruction("getq", "Places a 64-bit value from the console into register", "getq %r10"),
                new Instruction("gets", "Places a string from the console into memory", "gets %r10, %r11"),
                new Instruction("outc", "Writes a character from a register onto the console", "outc %r10"),
                new Instruction("outq", "Writes a 64-bit value from a register onto the console", "outq %r10"),
                new Instruction("outs", "Writes a string from memory onto the console", "outs %r10, %r11")
        );
        tabs.getTabs().forEach((b) -> {
            b.setClosable(false);
        });
        return new Scene(tabs, 720, 400);
    }

    public class Instruction {

        private final StringProperty firstName;

        public void setFirstName(String value) {
            firstNameProperty().set(value);
        }

        public String getFirstName() {
            return firstNameProperty().get();
        }

        public StringProperty firstNameProperty() {
            return firstName;
        }

        private final StringProperty lastName;

        public void setLastName(String value) {
            lastNameProperty().set(value);
        }

        public String getLastName() {
            return lastNameProperty().get();
        }

        public StringProperty lastNameProperty() {
            return lastName;
        }
        
        private final StringProperty usage;

        public void setUsage(String value) {
            usageProperty().set(value);
        }

        public String getUsage() {
            return usageProperty().get();
        }

        public StringProperty usageProperty() {
            return usage;
        }

        public Instruction(String a, String b, String c) {
            firstName = new SimpleStringProperty(a);
            lastName = new SimpleStringProperty(b);
            usage = new SimpleStringProperty(c);
        }
    }

    public static boolean getDarkTheme() {
        return DARK_THEME.get();
    }

    public static int getFontSize() {
        return FONT_SIZE.get();
    }

    public static IntegerProperty fontSize() {
        return FONT_SIZE;
    }

    public static BooleanProperty darkTheme() {
        return DARK_THEME;
    }

    public static StringProperty fontName() {
        return FONT_NAME;
    }

    public static String getFontName() {
        return FONT_NAME.get();
    }

    public static StringProperty fileDirectory() {
        return FILE_DIRECTORY;
    }

    public static String getFileDirectory() {
        return FILE_DIRECTORY.get();
    }
}
