package virtual.machine.view;

import java.util.function.IntFunction;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.reactfx.value.Val;

/**
 *
 * @author aniket
 */
public class CursorFactory implements IntFunction<Node> {

    private final IntegerProperty num;

    public CursorFactory(IntegerProperty liner) {
        num = liner;
    }

    @Override
    public Node apply(int lineNumber) {
        Polygon tri = new Polygon(0.0, 0.0, 10.0, 5.0, 0.0, 10.0);
        tri.setFill(Color.GREEN);
        boolean visible = contains(lineNumber);
        if (visible) {
            final Tooltip t = new Tooltip("Instruction Pointer");
            Tooltip.install(tri, t);
        }
        tri.visibleProperty().bind(Val.flatMap(tri.sceneProperty(), scene -> {
            return scene != null ? Val.constant(visible) : Val.constant(false);
        }));
        return tri;
    }

    private boolean contains(int linenumber) {
        return num.get() == linenumber;
    }

}
