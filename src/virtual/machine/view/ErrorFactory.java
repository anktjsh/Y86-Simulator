package virtual.machine.view;

import java.util.function.IntFunction;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.reactfx.value.Val;
import virtual.machine.core.Pair;

/**
 *
 * @author Aniket
 */
public class ErrorFactory implements IntFunction<Node> {

    private final Pair<Integer, String> lines;

    public ErrorFactory(Pair<Integer, String> liner) {
        lines = liner;
    }

    @Override
    public Node apply(int lineNumber) {
        Polygon tri = new Polygon(0.0, 0.0, 10.0, 5.0, 0.0, 10.0);
        tri.setFill(Color.RED);
        MessageBooleanProperty visible = contains(lineNumber);
        if (visible.getValue()) {
            final Tooltip t = new Tooltip(visible.getMessage());
            Tooltip.install(tri, t);
        }
        tri.visibleProperty().bind(Val.flatMap(tri.sceneProperty(), scene -> {
            return scene != null ? visible : Val.constant(false);
        }));
        return tri;
    }

    private MessageBooleanProperty contains(int linenumber) {
        if (lines.getKey() == linenumber) {
            return new MessageBooleanProperty(lines.getValue(), true);
        }
        return new MessageBooleanProperty("", false);
    }

    private class MessageBooleanProperty extends SimpleBooleanProperty {

        private final String mess;

        public MessageBooleanProperty(String message, boolean bl) {
            super(bl);
            mess = message;
        }

        public String getMessage() {
            return mess;
        }
    }

}
