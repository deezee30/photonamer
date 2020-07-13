/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process;

import me.deezee.photonamer.NamerSettings;
import me.deezee.photonamer.process.timer.Timer;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.TimeUnit;

public class NamerProcessResult {

    private final NamerSettings settings;
    private final Type type;
    private final int changed;
    private final Timer timer;

    public NamerProcessResult(NamerSettings settings, Type type, int changed, Timer timer) {
        this.settings = Validate.notNull(settings, "Settings used cannot be null");
        this.type = Validate.notNull(type, "Process result type cannot be null");
        this.changed = changed < 0 ? -1 : changed;
        this.timer = Validate.notNull(timer, "Process task timer cannot be null");
    }

    public NamerSettings getSettings() {
        return settings;
    }

    public Type getType() {
        return type;
    }

    public int getAmountChanged() {
        return changed;
    }

    public long getTimeCompleted(TimeUnit unit) {
        return timer.getTime(unit);
    }

    public enum Type {

        SUCCESS("Photo renaming process was successful!"),
        FAIL("Photo renaming process failed!"),
        UNKNOWN("Photo renaming partially (?) successful.");

        private final String msg;

        Type(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }
    }
}