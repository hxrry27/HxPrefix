package dev.hxrry.hxprefix.gui;

/**
 * Constants for GUI layout and timing
 * 
 * Eliminates magic numbers and makes GUI code more readable
 */
public final class GUIConstants {
    
    // === INVENTORY SIZING ===
    
    /** Slots per row in a chest inventory */
    public static final int SLOTS_PER_ROW = 9;
    
    /** Minimum rows for a chest GUI */
    public static final int MIN_ROWS = 1;
    
    /** Maximum rows for a chest GUI */
    public static final int MAX_ROWS = 6;
    
    /** Total slots in a row */
    public static final int FULL_ROW_SLOTS = 9;
    
    // === PAGINATION LAYOUT ===
    
    /**
     * Calculate content area for pagination
     * Content area is all slots except the bottom navigation row
     * 
     * @param rows Total rows in the GUI
     * @return Last slot index of content area
     */
    public static int getContentAreaEnd(int rows) {
        return (rows - 1) * SLOTS_PER_ROW - 1;
    }
    
    /**
     * Get slot index for previous page button (bottom left area)
     * 
     * @param rows Total rows in the GUI
     * @return Slot index for previous button
     */
    public static int getPreviousPageSlot(int rows) {
        return rows * SLOTS_PER_ROW - 6; // 3rd slot from left on bottom row
    }
    
    /**
     * Get slot index for next page button (bottom right area)
     * 
     * @param rows Total rows in the GUI
     * @return Slot index for next button
     */
    public static int getNextPageSlot(int rows) {
        return rows * SLOTS_PER_ROW - 4; // 3rd slot from right on bottom row
    }
    
    /**
     * Get slot index for page indicator (bottom center)
     * 
     * @param rows Total rows in the GUI
     * @return Slot index for page indicator
     */
    public static int getPageIndicatorSlot(int rows) {
        return rows * SLOTS_PER_ROW - 5; // Center of bottom row
    }
    
    // === ANIMATION TIMING ===
    
    /** Animation update interval in ticks (5 ticks = 0.25 seconds) */
    public static final long ANIMATION_UPDATE_TICKS = 5L;
    
    /** Animation initial delay in ticks (start immediately) */
    public static final long ANIMATION_INITIAL_DELAY = 0L;
    
    /** Fast animation speed in ticks (3 ticks = 0.15 seconds) */
    public static final long ANIMATION_FAST_TICKS = 3L;
    
    /** Slow animation speed in ticks (10 ticks = 0.5 seconds) */
    public static final long ANIMATION_SLOW_TICKS = 10L;
    
    // === SLOT POSITIONS (for fixed layouts) ===
    
    /** Center slot in a 6-row GUI (middle of 4th row) */
    public static final int CENTER_SLOT_6_ROW = 22;
    
    /** Bottom center slot in any GUI */
    public static int getBottomCenterSlot(int rows) {
        return rows * SLOTS_PER_ROW - 5;
    }
    
    /** Top center slot (row 1, slot 5) */
    public static final int TOP_CENTER_SLOT = 4;
    
    // === CONTENT DISPLAY LIMITS ===
    
    /** Maximum colours to display per section without pagination */
    public static final int MAX_COLOURS_PER_SECTION = 7;
    
    /** Maximum prefixes to display without pagination */
    public static final int MAX_PREFIXES_NO_PAGINATION = 45;
    
    /** Maximum suffixes to display without pagination */
    public static final int MAX_SUFFIXES_NO_PAGINATION = 45;
    
    /** Items per page for paginated menus */
    public static final int ITEMS_PER_PAGE = 45; // 5 rows of content
    
    // === SPECIAL SLOTS ===
    
    /** First slot of bottom row */
    public static int getBottomRowStart(int rows) {
        return (rows - 1) * SLOTS_PER_ROW;
    }
    
    /** Last slot of bottom row */
    public static int getBottomRowEnd(int rows) {
        return rows * SLOTS_PER_ROW - 1;
    }
    
    // === BORDER POSITIONS ===
    
    /** Left border column */
    public static final int LEFT_BORDER_COLUMN = 0;
    
    /** Right border column */
    public static final int RIGHT_BORDER_COLUMN = 8;
    
    /**
     * Check if a slot is in the left border
     */
    public static boolean isLeftBorder(int slot) {
        return slot % SLOTS_PER_ROW == LEFT_BORDER_COLUMN;
    }
    
    /**
     * Check if a slot is in the right border
     */
    public static boolean isRightBorder(int slot) {
        return slot % SLOTS_PER_ROW == RIGHT_BORDER_COLUMN;
    }
    
    /**
     * Check if a slot is in the bottom row
     */
    public static boolean isBottomRow(int slot, int rows) {
        return slot >= getBottomRowStart(rows);
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Calculate dynamic row count based on item count
     * 
     * @param itemCount Number of items to display
     * @param minRows Minimum rows
     * @param maxRows Maximum rows
     * @return Appropriate row count
     */
    public static int calculateRows(int itemCount, int minRows, int maxRows) {
        int neededRows = (itemCount / 7) + 2; // +2 for header and navigation
        return Math.min(maxRows, Math.max(minRows, neededRows));
    }
    
    /**
     * Get slot index from row and column
     * 
     * @param row Row number (0-indexed)
     * @param col Column number (0-indexed)
     * @return Slot index
     */
    public static int getSlot(int row, int col) {
        return row * SLOTS_PER_ROW + col;
    }
    
    /**
     * Get row number from slot index
     * 
     * @param slot Slot index
     * @return Row number (0-indexed)
     */
    public static int getRow(int slot) {
        return slot / SLOTS_PER_ROW;
    }
    
    /**
     * Get column number from slot index
     * 
     * @param slot Slot index
     * @return Column number (0-indexed)
     */
    public static int getColumn(int slot) {
        return slot % SLOTS_PER_ROW;
    }
    
    // Private constructor to prevent instantiation
    private GUIConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}