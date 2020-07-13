/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.format;

import me.deezee.photonamer.PhotoWrapper;

@FunctionalInterface
public interface NamerFormatCondition {

    Object replace(int id, PhotoWrapper photo);
}