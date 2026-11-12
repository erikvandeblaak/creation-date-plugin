package nl.vandeblaak.creationdate;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

/**
 * Reads the file system creation time of files and directories.
 */
public final class CreationDates {

    private CreationDates() {
    }

    /**
     * @return the creation time, or {@code null} for files that are not on the local disk or cannot be read
     */
    public static @Nullable Instant of(@Nullable VirtualFile file) {
        // Only real files on disk; skips entries inside jars/archives and in-memory files
        if (file == null || !file.isValid() || !file.isInLocalFileSystem()) {
            return null;
        }
        try {
            return Files.readAttributes(file.toNioPath(), BasicFileAttributes.class).creationTime().toInstant();
        } catch (IOException | UnsupportedOperationException | InvalidPathException e) {
            return null;
        }
    }
}
