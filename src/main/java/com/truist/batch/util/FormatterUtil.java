package com.truist.batch.util;

import org.apache.commons.lang3.StringUtils;

import com.truist.batch.model.DataType;
import com.truist.batch.model.FieldMapping;
import com.truist.batch.model.PaddingSide;
import com.truist.batch.model.TargetField;

public class FormatterUtil {

	/**
	 * Pads a string value to the specified length, using the given pad character
	 * and side.
	 * 
	 * @param value   The original string (null will be treated as empty).
	 * @param length  The desired total length after padding.
	 * @param padSide The side to pad: "LEFT" or "RIGHT" (case-insensitive).
	 * @param padChar The character to use for padding.
	 * @return The padded string.
	 */
	public static String pad(String value, FieldMapping mapping) {
		if (mapping == null)
			value = "";
		if (StringUtils.equalsIgnoreCase(mapping.getTargetFormat(), "date")) {
			if (StringUtils.isBlank(mapping.getFormat())) {
				throw new IllegalArgumentException("Date format required for field: " + mapping.getTargetField());
			}
			return formatDate(value, mapping.getFormat());
		} else if (StringUtils.equalsIgnoreCase(mapping.getTargetFormat(), "numeric")) {
			if (StringUtils.isBlank(mapping.getFormat())) {
				throw new IllegalArgumentException("Numeric format required for field: " + mapping.getTargetField());
			}
			return formatNumericByPattern(value, mapping.getFormat());
		}
		int padLength = mapping.getLength() - value.length();
		if (padLength <= 0)
			return value.substring(0, mapping.getLength());

		StringBuilder padding = new StringBuilder();
		for (int i = 0; i < padLength; i++) {
			padding.append(mapping.getPadChar());
		}
		if ("LEFT".equalsIgnoreCase(mapping.getPad())) {
			return padding + value;
		} else { // default/right
			return value + padding;
		}
	}
	
	
	/**
     * Format value using TargetField definition
     */
    public static String formatValue(String value, TargetField targetField) {
        if (value == null) value = "";
        
        // Apply data type specific formatting
        if (targetField.getDataType() == DataType.DATE) {
            if (StringUtils.isBlank(targetField.getFormat())) {
                throw new IllegalArgumentException("Date format required for field: " + targetField.getName());
            }
            value = formatDate(value, targetField.getFormat());
        } else if (targetField.getDataType() == DataType.NUMERIC) {
            if (StringUtils.isNotBlank(targetField.getFormat())) {
                value = formatNumericByPattern(value, targetField.getFormat());
            }
        }
        
        // Apply padding
        return applyPadding(value, targetField);
    }
    
    
    /**
     * Apply padding using TargetField configuration
     */
    public static String applyPadding(String value, TargetField targetField) {
        if (value == null) value = "";
        
        int targetLength = targetField.getLength();
        
        // Truncate if too long
        if (value.length() > targetLength) {
            return value.substring(0, targetLength);
        }
        
        // Return as-is if already correct length
        if (value.length() == targetLength) {
            return value;
        }
        
        // Determine padding character and side
        String padChar = " "; // default
        PaddingSide padSide = PaddingSide.RIGHT; // default
        
        if (targetField.getPadding() != null) {
            if (targetField.getPadding().getCharacter() != null) {
                padChar = targetField.getPadding().getCharacter();
            }
            if (targetField.getPadding().getSide() != null) {
                padSide = targetField.getPadding().getSide();
            }
        }
        
        // Apply padding
        int padLength = targetLength - value.length();
        String padding = padChar.repeat(padLength);
        
        return padSide == PaddingSide.LEFT ? padding + value : value + padding;
    }
    
    
    
    /**
     * Simple padding method for direct use
     */
    public static String pad(String value, int length, String padSide, String padChar) {
        if (value == null) value = "";
        
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        
        int padLength = length - value.length();
        String padding = padChar.repeat(padLength);
        
        return "LEFT".equalsIgnoreCase(padSide) ? padding + value : value + padding;
    }

    /**
     * Format value with explicit parameters (for enhanced processor)
     */
    public static String formatWithTargetField(String value, TargetField targetField, String sourceFormat) {
        if (value == null) value = "";
        
        // Apply source format conversion if needed
        if (StringUtils.isNotBlank(sourceFormat) && !sourceFormat.equals(targetField.getFormat())) {
            // Convert from source format to target format
            if (targetField.getDataType() == DataType.DATE) {
                value = convertDateFormat(value, sourceFormat, targetField.getFormat());
            }
        }
        
        return formatValue(value, targetField);
    }
    
    /**
     * Convert date from one format to another
     */
    private static String convertDateFormat(String value, String sourceFormat, String targetFormat) {
        if (StringUtils.isBlank(value)) return "";
        
        try {
            java.time.format.DateTimeFormatter sourceFormatter = java.time.format.DateTimeFormatter.ofPattern(sourceFormat);
            java.time.format.DateTimeFormatter targetFormatter = java.time.format.DateTimeFormatter.ofPattern(targetFormat);
            java.time.LocalDate date = java.time.LocalDate.parse(value, sourceFormatter);
            return targetFormatter.format(date);
        } catch (Exception e) {
            // Fallback to general date formatting
            return formatDate(value, targetFormat);
        }
    }

	/**
	 * Attempts to parse the inputDate using known common patterns and formats it to
	 * the outputFormat. If the inputDate does not match any known format, returns
	 * the inputDate as-is.
	 *
	 * @param inputDate    The date string to parse.
	 * @param outputFormat The desired output date pattern.
	 * @return The formatted date string, or the original inputDate if parsing
	 *         fails.
	 */
	public static String formatDate(String inputDate, String outputFormat) {
		if (inputDate == null || inputDate.trim().isEmpty())
			return "";

		java.util.List<String> knownFormats = java.util.Arrays.asList("yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy",
				"yyyyMMdd", "MMddyyyy");

		for (String format : knownFormats) {
			try {
				java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter
						.ofPattern(format);
				java.time.LocalDate date = java.time.LocalDate.parse(inputDate, inputFormatter);
				java.time.format.DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter
						.ofPattern(outputFormat);
				return outputFormatter.format(date);
			} catch (java.time.format.DateTimeParseException ignored) {
			}
		}
		return inputDate; // return as-is if no known format matches
	}

	/**
	 * Returns today's date formatted as yyyyMMdd.
	 *
	 * @return A string representing today's date.
	 */
	public static String getToday() {
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
		return java.time.LocalDate.now().format(formatter);
	}

	/**
	 * Returns the current time in HHmmss format.
	 *
	 * @return A string representing the current time.
	 */
	public static String getCurrentTimeStamp() {
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HHmmss");
		return java.time.LocalTime.now().format(formatter);
	}

	public static String formatByType(String value, String formatType, Integer length, Integer Precession) {
		if (value == null)
			return "";
		return switch (formatType) {
		case "+9(12)V9(6)", "+9(3)V9(3)", "+9(12)", "+9(6)", "+9(5)", "+9(4)", "+9(3)" ->
			formatNumericByPattern(value, formatType);
		default -> value;
		};
	}

	private static String formatNumericByPattern(String value, String formatType) {
		if (value == null || value.isEmpty())
			return "";

		// Remove any decimals and non-numeric characters
		String numericValue = value.replaceAll("[^0-9.-]", "");

		// Remove decimal part if exists for integer-only formats
		if (!formatType.contains("V")) {
			numericValue = numericValue.split("\\.")[0];
		}

		// Determine total length and precision
		int totalLength = 0;
		int precision = 0;

		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\+?9\\((\\d+)\\)(V9\\((\\d+)\\))?")
				.matcher(formatType);

		if (matcher.matches()) {
			totalLength = Integer.parseInt(matcher.group(1));
			if (matcher.group(3) != null) {
				precision = Integer.parseInt(matcher.group(3));
			}
		}

		// Remove decimal for formatting
		numericValue = numericValue.replace(".", "");

		// Pad with 0s on the left
		int padLength = totalLength + precision - numericValue.length();
		if (padLength > 0) {
			numericValue = "0".repeat(padLength) + numericValue;
		}

		// Prepend '+' sign if required
		if (formatType.startsWith("+")) {
			numericValue = "+" + numericValue;
		}

		return numericValue;
	}
}
