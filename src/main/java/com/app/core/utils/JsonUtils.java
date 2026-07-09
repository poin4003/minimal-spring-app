package com.app.core.utils;

public class JsonUtils {

    public static String simplifyJsonErrorMessage(String raw) {
        if (raw == null) return "Invalid JSON format.";

        if (raw.contains("not one of the values accepted for Enum class")) {
            String field = "";
            String validValues = "";

            int fieldIndex = raw.lastIndexOf("[\"");
            if (fieldIndex != -1) {
                int endIndex = raw.indexOf("\"]", fieldIndex);
                if (endIndex != -1) {
                    field = raw.substring(fieldIndex + 2, endIndex);
                }
            }

            int validIndex = raw.indexOf("[");
            int validEndIndex = raw.indexOf("]", validIndex);
            if (validIndex != -1 && validEndIndex != -1) {
                validValues = raw.substring(validIndex + 1, validEndIndex);
            }

            return String.format("Invalid enum value for field '%s'. Valid values: [%s]",
                    field.isEmpty() ? "unknown" : field,
                    validValues.isEmpty() ? "unknown" : validValues);
        }

        if (raw.contains("Cannot deserialize value of type")) {
            String field = "";
            int fieldIndex = raw.lastIndexOf("[\"");
            if (fieldIndex != -1) {
                int endIndex = raw.indexOf("\"]", fieldIndex);
                if (endIndex != -1) {
                    field = raw.substring(fieldIndex + 2, endIndex);
                }
            }
            return String.format("Invalid data type for field '%s'.", field.isEmpty() ? "unknown" : field);
        }

        if (raw.contains("Unexpected character")) {
            return "Malformed JSON syntax.";
        }

        return raw.replaceAll("\\[Source:.*", "").trim();
    }
}
