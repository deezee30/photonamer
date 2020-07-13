/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer;

import com.google.common.collect.ImmutableList;
import me.deezee.photonamer.format.NamerFormat;
import me.deezee.photonamer.process.NamerProcessException;
import me.deezee.photonamer.util.Formatting;
import org.apache.commons.lang3.Validate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NamerSettings {

    private static final String ERROR_SRC_CANNOT_BE_NULL = "Source directory cannot be null";
    private static final String ERROR_TGT_CANNOT_BE_NULL = "Output directory cannot be null";
    private static final String ERROR_FORMAT_CANNOT_BE_NULL = "Namer formatting styles cannot be null";
    private static final String ERROR_IMG_EXT_CANNOT_BE_NULL = "No null image extensions allowed";

    private Path            directory           = null;
    private Path            outputDirectory     = null;
    private NamerFormat     formatting          = null;
    private boolean         incSubDirs          = false;
    private boolean filterDateTimeTakenOnly = false;
    private List<String>    imgExtensions       = null;

    public Path getDirectory() {
        return directory;
    }

    public NamerSettings setDirectory(Path directory) {
        this.directory = Validate.notNull(directory, ERROR_SRC_CANNOT_BE_NULL);
        return this;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public NamerSettings setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = Validate.notNull(outputDirectory, ERROR_TGT_CANNOT_BE_NULL);
        return this;
    }

    public NamerFormat getFormatting() {
        return formatting;
    }

    public NamerSettings setFormatting(NamerFormat formatting) {
        this.formatting = Validate.notNull(formatting, ERROR_FORMAT_CANNOT_BE_NULL);
        return this;
    }

    public boolean includeSubDirectories() {
        return incSubDirs;
    }

    public NamerSettings setIncludeSubDirectories(boolean incSubDirs) {
        this.incSubDirs = incSubDirs;
        return this;
    }

    public boolean isFilterDateTimeTakenOnly() {
        return filterDateTimeTakenOnly;
    }

    public NamerSettings setFilterDateTimeTakenOnly(boolean filterDateTimeTakenOnly) {
        this.filterDateTimeTakenOnly = filterDateTimeTakenOnly;
        return this;
    }

    public List<String> getImageExtensions() {
        return imgExtensions;
    }

    public NamerSettings setImageExtensions(List<String> imgExtensions) {
        this.imgExtensions = ImmutableList.copyOf(Validate.noNullElements(
                imgExtensions, ERROR_IMG_EXT_CANNOT_BE_NULL));
        return this;
    }

    public void validate() throws NamerProcessException {
        if (directory == null)          throw new NamerProcessException(ERROR_SRC_CANNOT_BE_NULL);
        if (outputDirectory == null)    throw new NamerProcessException(ERROR_TGT_CANNOT_BE_NULL);
        if (formatting == null)         throw new NamerProcessException(ERROR_FORMAT_CANNOT_BE_NULL);
        if (imgExtensions == null)      throw new NamerProcessException(ERROR_IMG_EXT_CANNOT_BE_NULL);
    }

    public boolean isImage(Path file) {
        return validFile(file) && isImage(file.getFileName().toString());
    }

    public boolean isImage(String filename) {
        return isImage(filename, imgExtensions);
    }

    public static boolean isImage(Path file, String... allowedImgExts) {
        return validFile(file) && isImage(file.getFileName().toString(), allowedImgExts);
    }

    public static boolean isImage(Path file, List<String> allowedImgExts) {
        return validFile(file) && isImage(file.getFileName().toString(), allowedImgExts);
    }

    public static boolean isImage(String filename, String... allowedImgExts) {
        return isImage(filename, ImmutableList.copyOf(allowedImgExts));
    }

    public static boolean isImage(String filename, List<String> allowedImgExts) {
        return allowedImgExts.contains(Formatting.getFilenameComponents(filename).getValue());
    }

    private static boolean validFile(Path file) {
        return Files.exists(file) && !Files.isDirectory(file);
    }
}