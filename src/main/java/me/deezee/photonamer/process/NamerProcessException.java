/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process;

public class NamerProcessException extends Exception {

    public NamerProcessException() {
    }

    public NamerProcessException(String message) {
        super(message);
    }

    public NamerProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public NamerProcessException(Throwable cause) {
        super(cause);
    }

    public NamerProcessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}