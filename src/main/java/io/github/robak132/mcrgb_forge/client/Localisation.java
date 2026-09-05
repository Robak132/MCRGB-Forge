package io.github.robak132.mcrgb_forge.client;

import java.util.Locale;

public final class Localisation {

    public static final String UI_R_FOR_RED = "ui.mcrgb_forge.r_for_red";
    public static final String UI_G_FOR_GREEN = "ui.mcrgb_forge.g_for_green";
    public static final String UI_B_FOR_BLUE = "ui.mcrgb_forge.b_for_blue";
    public static final String UI_H_FOR_HUE_HSV = "ui.mcrgb_forge.h_for_hue_hsv";
    public static final String UI_S_FOR_SAT_HSV = "ui.mcrgb_forge.s_for_sat_hsv";
    public static final String UI_V_FOR_VAL_HSV = "ui.mcrgb_forge.v_for_val_hsv";
    public static final String UI_H_FOR_HUE_HSL = "ui.mcrgb_forge.h_for_hue_hsl";
    public static final String UI_S_FOR_SAT_HSL = "ui.mcrgb_forge.s_for_sat_hsl";
    public static final String UI_L_FOR_LIT_HSL = "ui.mcrgb_forge.l_for_lit_hsl";
    public static final String UI_RGB = "ui.mcrgb_forge.rgb";
    public static final String UI_HSV = "ui.mcrgb_forge.hsv";
    public static final String UI_HSL = "ui.mcrgb_forge.hsl";
    public static final String UI_REFINE = "ui.mcrgb_forge.refine";
    public static final String UI_REFRESH_INFO = "ui.mcrgb_forge.refresh_info";
    public static final String UI_HEADER = "ui.mcrgb_forge.header";
    public static final String UI_SAVED_COLORS = "ui.mcrgb_forge.saved_colors";
    public static final String UI_EDIT_PALETTE_INFO = "ui.mcrgb_forge.edit_palette_info";
    public static final String UI_DELETE_PALETTE_INFO = "ui.mcrgb_forge.delete_palette_info";
    public static final String UI_BACK_INFO = "ui.mcrgb_forge.back_info";

    public static final String CONFIG_TITLE = "title.mcrgb_forge.config";
    public static final String CONFIG_CATEGORY = "options.mcrgb_forge.category.configs";
    public static final String OPTION_ALWAYS_SHOW_TOOLTIPS = "option.mcrgb_forge.always_show_in_tooltips";
    public static final String OPTION_MAX_TOOLTIP_LINES = "option.mcrgb_forge.max_tooltip_lines";
    public static final String OPTION_SLIDER_CONSTANT_UPDATE = "option.mcrgb_forge.slider_constant_update";
    public static final String OPTION_READ_JSON_FILE = "option.mcrgb_forge.read_json_file";
    public static final String OPTION_COLOR_MODE = "option.mcrgb_forge.color_mode";
    public static final String OPTION_ITEM_SPAWNING_MODE = "option.mcrgb_forge.item_spawning_mode";
    public static final String OPTION_GIVE_COMMAND = "option.mcrgb_forge.give_command";
    public static final String OPTION_BYPASS_OP = "option.mcrgb_forge.bypass_op";
    public static final String OPTIONS_ALL_CONTEXTS = "options.mcrgb_forge.all_contexts";
    public static final String OPTIONS_PICKER_ONLY = "options.mcrgb_forge.picker_only";
    public static final String OPTIONS_WHILE_SCROLLING = "options.mcrgb_forge.while_scrolling";
    public static final String OPTIONS_AFTER_SCROLLING = "options.mcrgb_forge.after_scrolling";

    public static final String TOOLTIP_ALWAYS_SHOW_TOOLTIPS = "tooltip.mcrgb_forge.always_show_in_tooltips";
    public static final String TOOLTIP_MAX_TOOLTIP_LINES = "tooltip.mcrgb_forge.max_tooltip_lines";
    public static final String TOOLTIP_SLIDER_CONSTANT_UPDATE = "tooltip.mcrgb_forge.slider_constant_update";
    public static final String TOOLTIP_READ_JSON_FILE = "tooltip.mcrgb_forge.read_json_file";
    public static final String TOOLTIP_COLOR_MODE = "tooltip.mcrgb_forge.color_mode";
    public static final String TOOLTIP_ITEM_SPAWNING_MODE = "tooltip.mcrgb_forge.item_spawning_mode";
    public static final String TOOLTIP_GIVE_COMMAND = "tooltip.mcrgb_forge.give_command";
    public static final String TOOLTIP_BYPASS_OP = "tooltip.mcrgb_forge.bypass_op";
    public static final String TOOLTIP_SHIFT_PROMPT = "tooltip.mcrgb_forge.shift_prompt";
    public static final String TOOLTIP_ITEM_SHOW_MORE = "tooltip.mcrgb_forge.item_show_more";
    public static final String TOOLTIP_SHOW_MORE = "tooltip.mcrgb_forge.show_more";

    public static final String TOAST_TITLE = "toast.mcrgb_forge.title";
    public static final String TOAST_CACHE_LOADED = "toast.mcrgb_forge.cache_loaded";
    public static final String TOAST_SCAN_STARTED = "toast.mcrgb_forge.scan_started";
    public static final String TOAST_SCAN_IN_PROGRESS = "toast.mcrgb_forge.scan_in_progress";
    public static final String TOAST_RELOADED = "toast.mcrgb_forge.reloaded";
    public static final String TOAST_COPIED_HEX_TO_CLIPBOARD = "toast.mcrgb_forge.copied_hex_to_clipboard";

    public static final String WARNING_NO_CLOTH_CONFIG = "warning.mcrgb_forge.noclothconfig";
    public static final String KEY_CATEGORY = "key.category.mcrgb_forge.mcrgb_forge";
    public static final String KEY_OPEN_GUI = "key.mcrgb_forge.color_inv_open";
    public static final String KEY_QUICK_SEARCH = "key.mcrgb_forge.quick_search_from_clipboard";

    private Localisation() {
    }

    public static String colorMode(Enum<?> value) {
        return "options.mcrgb_forge.color_mode." + enumName(value);
    }

    public static String itemSpawningMode(Enum<?> value) {
        return "options.mcrgb_forge.item_spawning_mode." + enumName(value);
    }

    private static String enumName(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
