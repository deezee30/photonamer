/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process.timer;

import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TimedTask<T> {

    private final Timer timer = new Timer();
    private T t;

    @Nullable
    public final T execute() throws Exception {
        return executeAndThen(() -> {});
    }

    @Nullable
    public final T executeAndThen(@Nonnull Runnable runnable) throws Exception {
        timer.onFinishExecute(Validate.notNull(runnable)).start();
        t = process();
        timer.forceStop();
        return t;
    }

    @Nonnull
    public final Timer getTimer() {
        return timer;
    }

    @Nullable
    public final T getT() {
        return t;
    }

    @Nullable
    protected abstract T process() throws Exception;
}