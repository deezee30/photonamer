/*
 * Part of PhotoNamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.ui;

import javafx.scene.image.Image;

public final class Resources {

    private static final ClassLoader CLASS_LOADER = Thread
            .currentThread()
            .getContextClassLoader();

    public static final Image MAIN_ICON = getImage("pn_icon.png");

    public static Image getImage(final String resource) {
        return new Image(CLASS_LOADER.getResource(resource).toExternalForm());
    }

    private Resources() { }
}