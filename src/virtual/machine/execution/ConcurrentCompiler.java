package virtual.machine.execution;

import javafx.util.Callback;
import virtual.machine.core.Pair;

/**
 *
 * @author aniket
 */
public class ConcurrentCompiler {

    public static void compile(String in, Callback<Pair<Integer, String>, Void> call) {
        try {
            Compiler.getInstance().compile(in);
            call.call(null);
        } catch (CompilerException e) {
            call.call(new Pair<>(e.getLine() - 1, e.getMessage()));
        }
    }
}
