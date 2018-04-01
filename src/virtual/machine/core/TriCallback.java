package virtual.machine.core;

/**
 *
 * @author aniket
 * @param <A>
 * @param <P>
 * @param <R>
 */
@FunctionalInterface
public interface TriCallback<A, P, R> {

    public R call(A param1, P param2);
}
