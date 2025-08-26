package dev.hxrry.hxprefix.api.models;

import org.bukkit.Material;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * represents a selectable style option (colour, prefix, or suffix)
 */
public class StyleOption {
    
    /**
     * type of style option
     */
    public enum Type {
        COLOUR_SOLID("solid"),
        COLOUR_GRADIENT("gradient"),
        COLOUR_SPECIAL("special"), // rainbow, etc
        PREFIX("prefix"),
        SUFFIX("suffix");
        
        private final String value;
        
        Type(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isColour() {
            return this == COLOUR_SOLID || this == COLOUR_GRADIENT || this == COLOUR_SPECIAL;
        }
    }
    
    private final String id;
    private final String displayName;
    private final Type type;
    private final String value; // the actual value to apply
    private final Material material;
    private final boolean glow;
    private final List<String> description;
    private final List<String> allowedRanks;
    private final Map<String, Object> metadata; // extra data like animation info
    
    // full constructor
    public StyleOption(@NotNull String id, @NotNull String displayName, @NotNull Type type,
                      @NotNull String value, @NotNull Material material, boolean glow,
                      @Nullable List<String> description, @NotNull List<String> allowedRanks,
                      @Nullable Map<String, Object> metadata) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.value = value;
        this.material = material;
        this.glow = glow;
        this.description = description;
        this.allowedRanks = allowedRanks;
        this.metadata = metadata;
    }
    
    // simple constructor for basic options
    public StyleOption(@NotNull String id, @NotNull String displayName, @NotNull Type type,
                      @NotNull String value, @NotNull Material material, @NotNull List<String> allowedRanks) {
        this(id, displayName, type, value, material, false, null, allowedRanks, null);
    }
    
    // getters
    @NotNull
    public String getId() { 
        return id; 
    }
    
    @NotNull
    public String getDisplayName() { 
        return displayName; 
    }
    
    @NotNull
    public Type getType() { 
        return type; 
    }
    
    @NotNull
    public String getValue() { 
        return value; 
    }
    
    @NotNull
    public Material getMaterial() { 
        return material; 
    }
    
    public boolean hasGlow() { 
        return glow; 
    }
    
    @Nullable
    public List<String> getDescription() { 
        return description; 
    }
    
    @NotNull
    public List<String> getAllowedRanks() { 
        return allowedRanks; 
    }
    
    @Nullable
    public Map<String, Object> getMetadata() { 
        return metadata; 
    }
    
    // utility methods
    
    /**
     * check if a rank can use this option
     */
    public boolean isAllowedForRank(@NotNull String rank) {
        return allowedRanks.contains(rank.toLowerCase()) || 
               allowedRanks.contains("*"); // * means all ranks
    }
    
    /**
     * check if this is a gradient colour
     */
    public boolean isGradient() {
        return type == Type.COLOUR_GRADIENT;
    }
    
    /**
     * check if this is rainbow
     */
    public boolean isRainbow() {
        return type == Type.COLOUR_SPECIAL && value.contains("rainbow");
    }
    
    /**
     * check if this has animation metadata
     */
    public boolean hasAnimation() {
        if (metadata == null) return false;
        Object animData = metadata.get("animation");
        return animData instanceof Map && ((Map<?, ?>) animData).containsKey("enabled");
    }
    
    /**
     * get animation frames if available
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public List<String> getAnimationFrames() {
        if (!hasAnimation()) return null;
        Map<String, Object> animData = (Map<String, Object>) metadata.get("animation");
        return (List<String>) animData.get("frames");
    }
    
    /**
     * get animation speed if available
     */
    public int getAnimationSpeed() {
        if (!hasAnimation()) return 5; // default speed
        @SuppressWarnings("unchecked")
        Map<String, Object> animData = (Map<String, Object>) metadata.get("animation");
        Object speed = animData.get("speed");
        return speed instanceof Integer ? (Integer) speed : 5;
    }
    
    /**
     * check if this option requires a specific permission
     */
    public boolean requiresPermission() {
        return metadata != null && metadata.containsKey("permission");
    }
    
    /**
     * get required permission if any
     */
    @Nullable
    public String getRequiredPermission() {
        if (metadata == null) return null;
        Object perm = metadata.get("permission");
        return perm instanceof String ? (String) perm : null;
    }
    
    /**
     * check if this is a seasonal/event option
     */
    public boolean isSeasonal() {
        return metadata != null && metadata.containsKey("seasonal");
    }
    
    /**
     * create a copy with different allowed ranks
     */
    @NotNull
    public StyleOption withRanks(@NotNull List<String> newRanks) {
        return new StyleOption(id, displayName, type, value, material, glow,
                              description, newRanks, metadata);
    }
    
    @Override
    public String toString() {
        return "StyleOption{" +
            "id='" + id + '\'' +
            ", type=" + type +
            ", value='" + value + '\'' +
            ", ranks=" + allowedRanks.size() +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyleOption that = (StyleOption) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}