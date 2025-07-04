package com.prefix27.customization.utils;

import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9 ._-]+$");
    private static final Pattern PROFANITY_PATTERN = Pattern.compile("(?i).*(fuck|shit|damn|bitch|ass|crap).*");
    
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    public static boolean isValidNickname(String nickname, int minLength, int maxLength) {
        if (nickname == null) {
            return false;
        }
        
        String stripped = ColorUtils.stripLegacyColors(nickname);
        
        return stripped.length() >= minLength && 
               stripped.length() <= maxLength && 
               SAFE_TEXT_PATTERN.matcher(stripped).matches();
    }
    
    public static boolean isValidPrefix(String prefix, int minLength, int maxLength, List<String> forbiddenWords) {
        if (prefix == null) {
            return false;
        }
        
        String stripped = ColorUtils.stripLegacyColors(prefix).toLowerCase();
        
        // Check length
        if (stripped.length() < minLength || stripped.length() > maxLength) {
            return false;
        }
        
        // Check forbidden words
        for (String forbiddenWord : forbiddenWords) {
            if (stripped.contains(forbiddenWord.toLowerCase())) {
                return false;
            }
        }
        
        // Check for profanity (basic)
        if (PROFANITY_PATTERN.matcher(stripped).matches()) {
            return false;
        }
        
        // Check if it contains only safe characters
        return SAFE_TEXT_PATTERN.matcher(stripped).matches();
    }
    
    public static boolean containsProfanity(String text) {
        if (text == null) {
            return false;
        }
        
        return PROFANITY_PATTERN.matcher(text.toLowerCase()).matches();
    }
    
    public static boolean isValidColorName(String colorName) {
        if (colorName == null || colorName.isEmpty()) {
            return false;
        }
        
        // Check if it's a valid hex color
        if (ColorUtils.isValidHexColor(colorName)) {
            return true;
        }
        
        // Check if it's a named color
        return ColorUtils.parseColor(colorName) != null;
    }
    
    public static boolean isValidGradient(String gradientString) {
        if (gradientString == null || gradientString.isEmpty()) {
            return false;
        }
        
        if (!gradientString.contains(":")) {
            return false;
        }
        
        String[] parts = gradientString.split(":");
        if (parts.length != 2) {
            return false;
        }
        
        return isValidColorName(parts[0]) && isValidColorName(parts[1]);
    }
    
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove dangerous characters
        return input.replaceAll("[<>\"'&]", "")
                   .trim();
    }
    
    public static boolean isValidLength(String text, int minLength, int maxLength) {
        if (text == null) {
            return false;
        }
        
        String stripped = ColorUtils.stripLegacyColors(text);
        return stripped.length() >= minLength && stripped.length() <= maxLength;
    }
    
    public static boolean containsForbiddenWords(String text, List<String> forbiddenWords) {
        if (text == null || forbiddenWords == null) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        for (String forbiddenWord : forbiddenWords) {
            if (lowerText.contains(forbiddenWord.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}