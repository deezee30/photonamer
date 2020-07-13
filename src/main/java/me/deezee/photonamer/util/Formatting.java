/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.util;

import org.apache.commons.lang3.tuple.Pair;

public final class Formatting {

    public static Pair<String, String> getFilenameComponents(String filename) {
        int dot = filename.lastIndexOf(".");
        // Return basic filename for files with no extensions
        if (dot == -1)
            // Image does not have an extension to work with
            return Pair.of(filename, "");

        // Exclude dot in extension
        return Pair.of(
                filename.substring(0, dot), // filename without extension
                filename.substring(dot + 1) // file extension without dot
        );
    }

    public static String capitalise(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    // Disable initialisation
    private Formatting() {}
}