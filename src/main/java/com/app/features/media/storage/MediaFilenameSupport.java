package com.app.features.media.storage;

import java.util.Locale;

import com.app.core.exception.ExceptionFactory;

public final class MediaFilenameSupport {

    private static final int MAXIMUM_FILENAME_LENGTH = 255;

    private MediaFilenameSupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String normalize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw ExceptionFactory.invalidParam("Media filename is required.");
        }

        String normalizedSeparators = originalFilename.replace('\\', '/');
        String filename = normalizedSeparators.substring(normalizedSeparators.lastIndexOf('/') + 1).trim();

        if (filename.isBlank() || filename.length() > MAXIMUM_FILENAME_LENGTH) {
            throw ExceptionFactory.invalidParam("Media filename is invalid.");
        }

        for (int index = 0; index < filename.length(); index++) {
            if (Character.isISOControl(filename.charAt(index))) {
                throw ExceptionFactory.invalidParam("Media filename is invalid.");
            }
        }

        return filename;
    }

    public static String extensionOf(String filename) {
        int separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == filename.length() - 1) {
            throw ExceptionFactory.invalidParam("Media filename must include an extension.");
        }

        String extension = filename.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("^[a-z0-9]+$")) {
            throw ExceptionFactory.invalidParam("Media file extension is invalid.");
        }

        return extension;
    }
}
