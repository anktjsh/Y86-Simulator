package virtual.machine.view;

import java.util.function.IntFunction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
        BooleanProperty visible = contains(lineNumber);
        tri.visibleProperty().bind(Val.flatMap(tri.sceneProperty(), scene -> {
            return scene != null ? visible : Val.constant(false);
        }));
        return tri;
    }

    private BooleanProperty contains(int linenumber) {
        for (int one : lines) {
            if (one == linenumber) {
                return new SimpleBooleanProperty(true);
            }
        }
        return new SimpleBooleanProperty(false);
    }

}
