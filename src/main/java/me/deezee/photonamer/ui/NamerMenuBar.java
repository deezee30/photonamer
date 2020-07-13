/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.ui;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import me.deezee.photonamer.PhotoNamer;

public class NamerMenuBar extends MenuBar {

    public NamerMenuBar(PhotoNamer.StartApplication photonamer) {

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem close = new MenuItem("Close");
        close.setOnAction(event -> {
            Platform.exit();
            System.exit(0);
        });
        fileMenu.getItems().addAll(close);

        getMenus().addAll(fileMenu);
    }
}