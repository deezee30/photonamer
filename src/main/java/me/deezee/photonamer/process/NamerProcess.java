/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.process;

import me.deezee.photonamer.NamerSettings;
import me.deezee.photonamer.PhotoNamer;
import me.deezee.photonamer.PhotoWrapper;
import me.deezee.photonamer.ServiceExecutor;
import me.deezee.photonamer.process.timer.Timer;
import me.deezee.photonamer.util.Printer;
import org.apache.commons.lang3.Validate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class NamerProcess {

    private final NamerSettings settings;
    private final NamerProcessFinishTask onFinish;
    private final ConcurrentHashMap<Path, Path> moved = new ConcurrentHashMap<>();
    private volatile boolean busy = false;
    private volatile boolean finished = false;

    NamerProcess(NamerSettings settings, NamerProcessFinishTask onFinish) {
        this.settings = Validate.notNull(settings, "Namer process settings cannot be null");
        this.onFinish = Validate.notNull(onFinish, "onFinish task must not be null (empty is allowed)");
    }

    public synchronized boolean start() throws NamerProcessException {
        if (busy)       throw new NamerProcessException("Process is busy");
        if (finished)   throw new NamerProcessException("Process has already been executed");

        int maxDepth = settings.includeSubDirectories() ? Integer.MAX_VALUE : 1;

        busy = true;

        Printer.log("\nRenaming...");

        // Perform task asynchronously
        Future<NamerProcessResult> exe = ServiceExecutor.getCachedExecutor().submit(() -> {
            Timer timer = new Timer().start();

            // Set up a stream of images within directory
            Stream<Path> imgs = Files.walk(settings.getDirectory(), maxDepth);

            AtomicInteger count = new AtomicInteger(0);
            AtomicInteger id = new AtomicInteger(0);

            // Remember errors
            AtomicReference<Exception> error = new AtomicReference<>();

            imgs.forEach(inputFile -> {
                // Ensure file is a valid image first of all
                if (!settings.isImage(inputFile))
                    return;

                // Ensure image hasn't already been moved to prevent double-renaming
                if (moved.containsValue(inputFile))
                    return;

                try {
                    PhotoWrapper photo = new PhotoWrapper(inputFile);

                    // If filtering out any photos that don't have datetime taken attribute tag,
                    // then load the metadata for each EXIF or Xmp file and check
                    if (settings.isFilterDateTimeTakenOnly() && photo.takenAt(ChronoField.YEAR).isEmpty())
                        return;

                    count.getAndIncrement();

                    Path target = photo.applyFormat(settings.getFormatting(), settings.getOutputDirectory(), id.get());

                    // If succeeds, save renamed folder to cache to prevent double renaming and potential undo
                    moved.put(inputFile, target);

                    // Log success if available
                    Printer.debug("Renamed (#%d) '%s' to '%s'", id.get(), inputFile.toString(), target.toString());
                } catch (NamerProcessException e) {
                    // This particular one failed, save it, if it hasn't occurred already
                    if (error.get() == null)
                        error.set(e);

                    // Log error if available
                    Printer.debug("Failed for (#%d) '%s': %s", id.get(), inputFile.toString(), e.toString());
                } finally {
                    id.getAndIncrement();
                }
            });

            NamerProcessResult.Type type = NamerProcessResult.Type.UNKNOWN;

            int len = moved.size();

            if (error.get() == null) {
                // No errors
                if (len == 0) type = NamerProcessResult.Type.FAIL;
                if (len == count.get()) type = NamerProcessResult.Type.SUCCESS;
            } else {
                type = NamerProcessResult.Type.FAIL;
                PhotoNamer.alertError(error.get());
            }

            return new NamerProcessResult(settings, type, len, timer.forceStop());
        });

        boolean ok;

        try {
            NamerProcessResult res = exe.get();

            Printer.log("%s (Count: %d, %.2fs)", res.getType().getMessage(),
                    res.getAmountChanged(), res.getTimeCompleted(TimeUnit.MILLISECONDS) / 1000f);

            // Clear up
            busy = false;
            finished = true;
            ok = onFinish.onFinish(res);

        } catch (InterruptedException | ExecutionException e) {
            throw new NamerProcessException(e);
        }

        return ok;
    }

    public synchronized boolean undo() throws NamerProcessException {
        if (busy)                           throw new NamerProcessException("Process is busy");
        if (!finished || moved.isEmpty())   throw new NamerProcessException("Nothing to undo");

        finished = false;
        busy = true;

        Printer.log("\nUndoing...");

        // Perform task asynchronously
        Future<NamerProcessResult> exe = ServiceExecutor.getCachedExecutor().submit(() -> {
            Timer timer = new Timer().start();

            int id = 0;
            int count = 0;

            for (Map.Entry<Path, Path> entrySet : moved.entrySet()) {
                // Process is to rename 'tgt' back to 'src'
                Path src = entrySet.getKey();
                Path tgt = entrySet.getValue();

                // Check preconditions
                String err = null;
                if (Files.exists(src))                  err = "New target already exists";
                if (err != null && !Files.exists(tgt))  err = "Source no longer exists";
                if (err != null) {
                    Printer.debug("Skipping undo for (#%d) '%s' -> '%s': %s",
                            id, tgt.toString(), src.toString(), err);
                    id++;
                    continue;
                }

                // Rename target file back to source file
                try {
                    Files.move(tgt, src);

                    // Log success if available
                    Printer.debug("Renamed (#%d) '%s' to '%s'", id, tgt.toString(), src.toString());

                    count++;
                } catch (Exception e) {
                    // Log error if available - and skip renaming this file
                    Printer.debug("Failed undo for (#%d) '%s' -> '%s': %s",
                            id, tgt.toString(), src.toString(), e.toString());
                } finally {
                    id++;
                }
            }

            NamerProcessResult.Type type = NamerProcessResult.Type.UNKNOWN;

            if (id == 0) type = NamerProcessResult.Type.FAIL;
            else if (count == id) type = NamerProcessResult.Type.SUCCESS;

            return new NamerProcessResult(settings, type, count, timer.forceStop());
        });

        boolean ok;

        try {
            NamerProcessResult res = exe.get();

            Printer.log("%s (Count: %d/%d, %.2fs)", res.getType().getMessage(),
                    res.getAmountChanged(), moved.size(), res.getTimeCompleted(TimeUnit.MILLISECONDS) / 1000f);

            // Clear up
            moved.clear();
            busy = false;
            ok = onFinish.onFinish(res);

        } catch (InterruptedException | ExecutionException e) {
            throw new NamerProcessException(e);
        }

        return ok;
    }

    public NamerSettings getSettings() {
        return settings;
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean hasFinished() {
        return finished;
    }
}