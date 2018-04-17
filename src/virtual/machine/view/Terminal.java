package virtual.machine.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import virtual.machine.internal.Environment;

/**
 *
 * @author aniket
 */
public class Terminal extends BorderPane {

    private final TextArea txtArea;
    private final String LINE_SEPARATOR = System.lineSeparator();
    private final Font font;

    private final int INITIAL_CURSOR_POSITION = 2;

    private boolean isKeysDisabled;
    private int minCursorPosition = INITIAL_CURSOR_POSITION;

    private final Environment env;

    public Terminal(Environment env) {
        this.env = env;
        this.txtArea = new TextArea();
        setCenter(txtArea);
        this.font = Font.font("SansSerif", FontWeight.BOLD, FontPosture.REGULAR, 15);
        txtArea.setOnKeyPressed((e) -> {
            if (null != e.getCode()) switch (e.getCode()) {
                case BACK_SPACE:{
                    int cursorPosition = txtArea.getCaretPosition();
                    if (isKeysDisabled) {
                        e.consume();
                    }       if (cursorPosition == minCursorPosition && !isKeysDisabled) {
                        disableBackspaceKey();
                        e.consume();
                    } else if (cursorPosition > minCursorPosition && isKeysDisabled) {
                        enableBackspaceKey();
                    }       break;
                    }
                case ENTER:
                    e.consume();
                    disableTerminal();
                    String command = extractCommand();
                    executeCommand(command);
                    showNewLine();
                    showPrompt();
                    enableTerminal();
                    setMinCursorPosition();
                    break;
                case DOWN:
                case UP:
                    e.consume();
                    break;
                case LEFT:{
                    int cursorPosition = txtArea.getCaretPosition();
                    if (isKeysDisabled) {
                        e.consume();
                    }       if (cursorPosition == minCursorPosition && !isKeysDisabled) {
                        disableBackspaceKey();
                        e.consume();
                    } else if (cursorPosition > minCursorPosition && isKeysDisabled) {
                        enableBackspaceKey();
                    }       break;
                    }
                default:
                    break;
            }
        });
        txtArea.caretPositionProperty().addListener((ob, older, newer) -> {
            if (newer.intValue() < minCursorPosition) {
                Platform.runLater(() -> {
                    txtArea.positionCaret(txtArea.getText().length());
                });
            }
        });
        txtArea.setFont(font);
        showPrompt();
        env.output((s) -> {
            print(s);
            return null;
        });
    }

    private void disableBackspaceKey() {
        isKeysDisabled = true;
    }

    private void enableBackspaceKey() {
        isKeysDisabled = false;
    }

    private void setMinCursorPosition() {
        minCursorPosition = txtArea.getCaretPosition();
    }

    public void clear() {
        recreateStream();
        txtArea.setText("");
        showPrompt();
    }

    private void showPrompt() {
        txtArea.appendText("> ");
    }

    private void showNewLine() {
        txtArea.appendText(LINE_SEPARATOR);
    }

    public void enableTerminal() {
        txtArea.setDisable(false);
    }

    public void disableTerminal() {
        txtArea.setDisable(true);
    }

    private String extractCommand() {
        removeLastLineSeparator();
        String newCommand = stripPreviousCommands();
        return newCommand;
    }

    private void removeLastLineSeparator() {
        String terminalText = txtArea.getText();
        if (terminalText.charAt(terminalText.length() - 1) == '\n') {
            terminalText = terminalText.substring(0, terminalText.length() - 1);
        }
        txtArea.setText(terminalText);
    }

    private String stripPreviousCommands() {
        String terminalText = txtArea.getText();
        int lastPromptIndex = terminalText.lastIndexOf('>') + 2;
        if (lastPromptIndex < 0 || lastPromptIndex >= terminalText.length()) {
            return "";
        } else {
            return terminalText.substring(lastPromptIndex);
        }
    }

    private void print(String invalid_Statement) {
        txtArea.appendText(invalid_Statement);
    }

    private void executeCommand(String command) {
        if (env.allowingInput()) {
            getInput().action(command);
        }
    }

    private static CustomInputStream input;

    private static CustomInputStream getInput() {
        if (input == null) {
            recreateStream();
        }
        return input;
    }

    private static void recreateStream() {
        input = new CustomInputStream();
        in = new Scanner(getInput());
    }

    private static Scanner in;

    public static Scanner scan() {
        if (in == null) {
            in = new Scanner(getInput());
        }
        return in;
    }

    public static class CustomInputStream extends InputStream {

        final BlockingQueue<String> queue;

        public CustomInputStream() {
            queue = new LinkedBlockingQueue<>();
        }

        private String s;
        int pos;

        @Override
        public int read() throws IOException {
            while (null == s || s.length() <= pos) {
                try {
                    s = queue.take();
                    pos = 0;
                } catch (InterruptedException ex) {
                }
            }
            int ret = (int) s.charAt(pos);
            pos++;
            return ret;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytes_copied = 0;
            while (bytes_copied < 1) {
                while (null == s || s.length() <= pos) {
                    try {
                        s = queue.take();
                        pos = 0;
                    } catch (InterruptedException ex) {
                    }
                }
                int bytes_to_copy = len < s.length() - pos ? len : s.length() - pos;
                System.arraycopy(s.getBytes(), pos, b, off, bytes_to_copy);
                pos += bytes_to_copy;
                bytes_copied += bytes_to_copy;
            }
            return bytes_copied;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public void action(String s) {
            queue.add(s + "\r\n");
        }

    }

}
