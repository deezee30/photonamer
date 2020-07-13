/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import me.deezee.photonamer.format.NamerFormat;
import me.deezee.photonamer.format.NamerFormatCondition;
import me.deezee.photonamer.process.NamerProcessException;
import me.deezee.photonamer.util.Formatting;
import me.deezee.photonamer.util.Printer;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class PhotoWrapper implements BasicFileAttributes, Serializable {

    private final Path inputPath;
    private BasicFileAttributes _attributes;
    private Metadata _meta;
    private ExifSubIFDDirectory _exifDir;

    public PhotoWrapper(String inputPath) throws NamerProcessException, InvalidPathException {
        // Validate that all characters are legal
        this(Path.of(inputPath));
    }

    public PhotoWrapper(Path inputPath) throws NamerProcessException  {
        this.inputPath = Validate.notNull(inputPath);

        // Perform checks on path
        try {
            // TODO: Ensure file is valid


            // TODO: Ensure file is supported image

        } catch (Exception anyException) {
            throw new NamerProcessException(anyException);
        }
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Metadata getMetadata() {
        // Lazy init to increase performance when loading a full directory
        if (_meta == null) {
            try {
                _meta = ImageMetadataReader.readMetadata(Files.newInputStream(this.inputPath));
            } catch (ImageProcessingException | IOException e) {
                PhotoNamer.alertError(e);
            }
        }

        return _meta;
    }

    public ExifSubIFDDirectory getExifDirectory() {
        if (getMetadata() == null) return null;

        // Lazy init to increase performance when loading a full directory
        if (_exifDir == null)
            _exifDir = _meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        return _exifDir;
    }

    public Pair<String, String> format(NamerFormat format, int id) {
        Path file = inputPath.getFileName();

        // Know what extension type we are dealing with
        Pair<String, String> fileName = Formatting.getFilenameComponents(file.toString());
        String target = format.getFormat();
        String ext = fileName.getValue();

        // If grouping is enabled, apply to target path
        if (format.getGrouping().isPresent()) {
            target = format.getGrouping().get().getVariable() + File.separator + target;
        }

        for (Map.Entry<NamerFormat.Var, NamerFormatCondition> entry : NamerFormat.getVariables().entrySet()) {
            target = target.replaceAll("\\$" + entry.getKey().getName(),
                    String.valueOf(entry.getValue().replace(id, this)));
        }

        return Pair.of(target, ext);
    }

    public Path applyFormat(NamerFormat format, Path outputDir, int id) throws NamerProcessException {
        // Generate new file name based on formatting config
        Pair<String, String> components = format(format, id);
        String friendly = components.getKey();
        String ext = components.getValue();

        // Detect conflicting file names and prevent overwriting
        String conflictName = friendly;
        int conflictNo = 0;
        while (true) {
            try {

                if (conflictNo != 0) {
                    conflictName = friendly + " (" + conflictNo + ")";
                } else {
                    // For grouped images, ensure that group folder exists first of all.
                    // If the conflict number is 0, then there is no guarantee that it exists.
                    int separator = conflictName.indexOf(File.separatorChar);
                    if (separator != -1) {
                        String group = conflictName.substring(0, separator);
                        Path groupFolder = outputDir.resolve(group);

                        // Attempt to create directory if it doesn't exist
                        if (!Files.exists(groupFolder))
                            Files.createDirectory(groupFolder);
                    }
                }

                Path target = Files.move(inputPath, outputDir.resolve(conflictName + "." + ext));

                // success (no error)
                return target;
            } catch (FileAlreadyExistsException exists) {
                Printer.debug("'%s' already exists. Retrying with a new name...", conflictName + "." + ext);
                conflictNo++;
            } catch (IOException ex) {
                throw new NamerProcessException(ex);
            }
        }
    }

    public BasicFileAttributes getAttributes() {
        // Lazy init to increase performance when loading a full directory
        if (_attributes == null) {
            try {
                _attributes = Files.readAttributes(this.inputPath, BasicFileAttributes.class);
            } catch (IOException e) {
                PhotoNamer.alertError(e);
            }
        }

        return _attributes;
    }

    @Override
    public FileTime lastModifiedTime() {
        return getAttributes().lastModifiedTime();
    }

    @Override
    public FileTime lastAccessTime() {
        return getAttributes().lastAccessTime();
    }

    @Override
    public FileTime creationTime() {
        return getAttributes().creationTime();
    }

    @Override
    public boolean isRegularFile() {
        return getAttributes().isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return getAttributes().isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return getAttributes().isSymbolicLink();
    }

    @Override
    public boolean isOther() {
        return getAttributes().isOther();
    }

    @Override
    public long size() {
        return getAttributes().size();
    }

    @Override
    public Object fileKey() {
        return getAttributes().fileKey();
    }

    public int createdAt(ChronoField unit) {
        return fromMillis(creationTime().toMillis(), unit);
    }

    public int modifiedAt(ChronoField unit) {
        return fromMillis(lastModifiedTime().toMillis(), unit);
    }

    public Optional<Integer> takenAt(ChronoField unit) {
        // Lazy init to increase performance when loading a full directory
        ExifSubIFDDirectory dir = getExifDirectory();
        if (dir == null) return Optional.empty();

        Date date = dir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        if (date == null) return Optional.empty();

        return Optional.of(fromMillis(date.getTime(), unit));
    }

    private static int fromMillis(long millis, ChronoField unit) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).get(unit);
    }
}