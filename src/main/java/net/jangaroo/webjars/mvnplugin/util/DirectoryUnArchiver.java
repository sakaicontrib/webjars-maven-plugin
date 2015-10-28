package net.jangaroo.webjars.mvnplugin.util;

import net.jangaroo.webjars.mvnplugin.UnpackJarResourcesMojo;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * A simple unarchiver that copys between directories.
 */
public class DirectoryUnArchiver extends AbstractUnArchiver {

    /**
     * Validate the parameters, but all the sourcefile to be a directory.
     */
    @Override
    protected void validate()
            throws ArchiverException {
        File sourceFile = getSourceFile();
        File destDirectory = getDestDirectory();
        File destFile = getDestFile();

        if (sourceFile == null) {
            throw new ArchiverException("The source file isn't defined.");
        }

        if (!sourceFile.exists()) {
            throw new ArchiverException("The source file " + sourceFile + " doesn't exist.");
        }

        if (destDirectory == null && destFile == null) {
            throw new ArchiverException("The destination isn't defined.");
        }

        if (destDirectory != null && destFile != null) {
            throw new ArchiverException("You must choose between a destination directory and a destination file.");
        }

        if (destDirectory != null && !destDirectory.isDirectory()) {
            setDestFile(destDirectory);
            setDestDirectory(null);
        }

        if (destFile != null && destFile.isDirectory()) {
            setDestDirectory(destFile);
            setDestFile(null);
        }
    }

    @Override
    protected void execute() throws ArchiverException {
        Path prefix = Paths.get(UnpackJarResourcesMojo.META_INF_RESOURCES);
        Path source = getSourceFile().toPath();
        Path destination = getDestDirectory().toPath();
        if (!Files.isDirectory(source)) {
            throw new ArchiverException("Source isn't a directory: " + source.toAbsolutePath());
        }
        try {
            Files.walk(source).filter(p -> {
                try {
                    String relativePath = source.relativize(p).toString();
                    return isSelected(p.toFile().getName(), new PlexusIoFileResource(p.toFile(), relativePath));
                } catch (ArchiverException e) {
                    throw new RuntimeException("Failed to decided if file is included :" + p.toAbsolutePath(), e);
                }
            }).forEach(f -> {
                try {
                    Path relative = source.relativize(f);
                    // This trims off the prefix we don't want in the output This is what ResourcesUnArchiver also does
                    relative = prefix.relativize(relative);
                    Path target = destination.resolveSibling(relative);
                    if (!Files.exists(target.getParent())) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.copy(f, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy file: " + f.toAbsolutePath(), e);
                }
            });
        } catch (IOException | RuntimeException e) {
            throw new ArchiverException("Failed to copy", e);
        }
    }

    @Override
    protected void execute(String path, File outputDirectory) throws ArchiverException {
        throw new UnsupportedOperationException();

    }
}
