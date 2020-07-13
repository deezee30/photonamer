/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process;

@FunctionalInterface
public interface NamerProcessFinishTask {

    boolean onFinish(NamerProcessResult result);
}