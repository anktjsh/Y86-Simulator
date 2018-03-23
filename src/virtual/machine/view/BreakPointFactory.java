package virtual.machine.view;

import java.util.function.IntFunction;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.reactfx.value.Val;

/**
 *
 * @author Aniket
 */
public class BreakPointFactory implements IntFunction<Node> {
    
    private final ObservableSet<Integer> lines;
    
    public BreakPointFactory(ObservableSet<Integer> liner) {
        lines = liner;
    }
    
    @Override
    public Node apply(int lineNumber) {
        Circle tri = new Circle(5, Color.DARKRED);
        tri.visibleProperty().bind(Val.flatMap(tri.sceneProperty(), scene -> {
            return Val.constant(scene != null ? contains(lineNumber) : false);
        }));
        return tri;
    }
    
    private boolean contains(int linenumber) {
        return lines.stream().anyMatch((one) -> (one == linenumber));
    }
    
}
