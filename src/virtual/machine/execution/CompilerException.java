package virtual.machine.execution;

/**
 *
 * @author aniket
 */
public class CompilerException extends Exception {

    private final int line;

    public CompilerException(int line, String arg) {
        super(arg);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
