/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process;

import me.deezee.photonamer.NamerSettings;

import java.util.Optional;

public final class NamerProcessFactory {

    private static NamerProcessFactory instance;

    private NamerProcess currentProcess;

    public NamerProcess newProcess(NamerSettings settings) throws NamerProcessException {
        return newProcess(settings, amount -> true);
    }

    public NamerProcess newProcess(NamerSettings settings,
                                   NamerProcessFinishTask onFinish) throws NamerProcessException {
        settings.validate();

        currentProcess = new NamerProcess(settings, onFinish);
        return currentProcess;
    }

    public Optional<NamerProcess> getCurrentProcess() {
        return Optional.ofNullable(currentProcess);
    }

    public static NamerProcessFactory getInstance() {
        return instance == null ? instance = new NamerProcessFactory() : instance;
    }

    // Disable public initialisation
    private NamerProcessFactory() {}
}