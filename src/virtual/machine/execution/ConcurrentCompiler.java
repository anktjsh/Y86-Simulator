package virtual.machine.execution;

import virtual.machine.core.TriCallback;

/**
 *
 * @author aniket
 */
public class ConcurrentCompiler {

    public static void compile(String in, TriCallback<Integer, String, Void> call) {
        try {
            Compiler.getInstance().compile(in);
            call.call(null, null);
        } catch (CompilerException e) {
            call.call(e.getLine() - 1, e.getMessage());
        }
    }
}
