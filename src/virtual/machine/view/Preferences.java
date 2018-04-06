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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import static virtual.machine.Y86VM.CSS;

/**
 *
 * @author aniket
 */
public class Preferences extends Stage {
    
    private static final BooleanProperty DARK_THEME = new SimpleBooleanProperty(false);
    private static final IntegerProperty FONT_SIZE = new SimpleIntegerProperty(13);
    private static final StringProperty FONT_NAME = new SimpleStringProperty("System Regular");
    
    static {
        DARK_THEME.addListener((ob, older, newer) -> write());
        FONT_SIZE.addListener((ob, older, newer) -> write());
        FONT_NAME.addListener((ob, older, newer) -> write());
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
            Files.write(f.toPath(), FXCollections.observableArrayList(DARK_THEME.get() + "", FONT_SIZE.intValue() + "", FONT_NAME.get()));
        } catch (IOException ex) {
        }
    }
    
    private ToggleButton darkOption;
    private Spinner<Integer> fontOption;
    private ComboBox<String> font;
    
    public Preferences(Stage stage) {
        initOwner(stage);
        stage.setTitle("Preferences");
        initModality(Modality.APPLICATION_MODAL);
        setScene(buildScene());
        setResizable(false);
        if (Preferences.getDarkTheme()) {
            getScene().getStylesheets().add(CSS);
        }
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
        controls.getChildren().addAll(darkOption = new ToggleButton("Dark Theme"),
                new Label("Font Size"),
                fontOption = new Spinner<>(5, 50, getFontSize(), 1),
                new Label("Font"),
                font = new ComboBox<>(FXCollections.observableArrayList(Font.getFamilies())));
        font.setValue(FONT_NAME.get());
        darkOption.setSelected(getDarkTheme());
        DARK_THEME.bind(darkOption.selectedProperty());
        FONT_SIZE.bind(fontOption.valueProperty());
        FONT_NAME.bind(font.valueProperty());
        TableView<Instruction> table = new TableView<>();
        ext.setCenter(table);
        TableColumn<Instruction, String> ins = new TableColumn<>("Instruction");
        TableColumn<Instruction, String> opr = new TableColumn<>("Implementation");
        ins.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        opr.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        ins.setSortable(false);
        opr.setSortable(false);
        table.getColumns().addAll(ins, opr);
        table.getItems().addAll(
                new Instruction("brk", "Sets a breakpoint at this location"),
                new Instruction("cmovb", "Conditional Move if below"),
                new Instruction("cmovnb", "Conditional Move if not below"),
                new Instruction("cmovbe", "Conditional Move if below or equal"),
                new Instruction("cmova", "Conditional Move if above"),
                new Instruction("imultq", "Multiply"),
                new Instruction("divq", "Divide"),
                new Instruction("modq", "Modulus"),
                new Instruction("sarq", "Arithmetic Right Shift"),
                new Instruction("shrq", "Logical Right Shift"),
                new Instruction("salq", "Arithmetic Left Shift"),
                new Instruction("orq", "Bitwise OR"),
                new Instruction("jb", "Jump if below"),
                new Instruction("jnb", "Jump if not below"),
                new Instruction("jbe", "Jump if below or equal"),
                new Instruction("ja", "Jump if above"),
                new Instruction("notq", "Bitwise NOT"),
                new Instruction("negq", "2's complement negation"),
                new Instruction("incq", "Increment"),
                new Instruction("decq", "Decrement"),
                new Instruction("bangq", "1 if input is 0, 0 otherwise"));
        for (Tab b : tabs.getTabs()) {
            b.setClosable(false);
        }
        return new Scene(tabs, 400, 270);
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
        
        public Instruction(String a, String b) {
            firstName = new SimpleStringProperty(a);
            lastName = new SimpleStringProperty(b);
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
}
