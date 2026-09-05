package io.github.robak132.mcrgb_forge.client;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScanner;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScanner.ScanResult;
import io.github.robak132.mcrgb_forge.client.analysis.Palette;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.gui.ColorsGuiDescription;
import io.github.robak132.mcrgb_forge.client.gui.MCRGBClientScreen;
import io.github.robak132.mcrgb_forge.client.serialization.CacheSerializer;
import io.github.robak132.mcrgb_forge.client.serialization.PaletteSerializer;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
@Slf4j(topic = MOD_ID)
public class MCRGBClient {

    private static final ColorScanner scanner = new ColorScanner();
    private static final PaletteSerializer paletteSerializer = new PaletteSerializer();
    private static final CacheSerializer cacheSerializer = new CacheSerializer();
    private static final AtomicReference<Map<Block, List<SpriteDetails>>> LAST_SCAN = new AtomicReference<>();
    private static Future<ScanResult> activeScan = null;

    @Getter
    private static List<Palette> palettes;

    private MCRGBClient() {
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (palettes == null) {
            loadPalettes();
        }

        Map<Block, List<SpriteDetails>> cached = MCRGBConfig.READ_JSON_FILE.get() ? cacheSerializer.load() : null;
        if (cached != null) {
            setLastScan(cached);
            log.info("Startup cache loaded, {} blocks analyzed.", cached.size());
            showToast(Component.translatable(Localisation.TOAST_CACHE_LOADED, cached.size()));
        } else {
            triggerScan();
            log.info("No startup color cache found. Starting scan...");
            showToast(Component.translatable(Localisation.TOAST_SCAN_STARTED));
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (activeScan != null) {
            activeScan.cancel(true);
        }
        LAST_SCAN.set(null);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) {
            return;
        }

        if (MCRGBKeybindings.OPEN_GUI.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof MCRGBClientScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                openColorsGui();
            }
        }
        if (MCRGBKeybindings.QUICK_SEARCH.consumeClick() && Minecraft.getInstance().screen == null) {
            openColorsGuiFromClipboard();
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!MCRGBConfig.ALWAYS_SHOW_TOOLTIPS.get()) {
            return;
        }

        Map<Block, List<SpriteDetails>> data = getLastScan();
        if (data == null) {
            return;
        }

        Block block = Block.byItem(event.getItemStack().getItem());

        List<SpriteDetails> sprites = data.get(block);
        if (sprites == null || sprites.isEmpty()) {
            return;
        }

        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable(Localisation.TOOLTIP_SHIFT_PROMPT)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        int colorLines = 0;
        int maxLines = MCRGBConfig.MAX_TOOLTIP_LINES.get();
        // Show clustered colors
        for (SpriteDetails sd : sprites) {
            List<String> labels = sd.getStrings();
            List<Integer> colors = sd.getTextColors();

            for (int i = 0; i < labels.size(); i++) {
                if (colorLines >= maxLines) {
                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.translatable(Localisation.TOOLTIP_ITEM_SHOW_MORE)
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }
                String label = labels.get(i);
                int color = colors.get(i);
                MutableComponent text = Component.literal(label).withStyle(ChatFormatting.GRAY);
                if (i == 0) {
                    event.getToolTip().add(text);
                } else {
                    MutableComponent colorBlock = Component.literal("⬛").withStyle(Style.EMPTY.withColor(color));
                    event.getToolTip().add(colorBlock.append(text));
                }
                colorLines++;
            }
        }
    }

    public static void addPalette(Palette palette) {
        palettes.add(palette);
    }

    public static void removePalette(Palette palette) {
        palettes.remove(palette);
    }

    public static void savePalettes() {
        paletteSerializer.save(palettes);
    }

    public static void loadPalettes() {
        palettes = paletteSerializer.load();
    }

    public static void triggerScan() {
        if (activeScan != null && !activeScan.isDone()) {
            return;
        }
        startScan();
    }

    public static void refreshScan() {
        if (activeScan != null && !activeScan.isDone()) {
            activeScan.cancel(true);
        }
        startScan();
    }

    private static void startScan() {
        activeScan = scanner.scanAsync(MCRGBClient::onSuccess, MCRGBClient::onError);
    }

    private static void openColorsGui() {
        openColorsGui(new RGB(255, 255, 255));
    }

    private static void openColorsGuiFromClipboard() {
        Integer color = io.github.robak132.mcrgb_forge.client.utils.TypeConversionUtils.hexToInt(
                Minecraft.getInstance().keyboardHandler.getClipboard());
        openColorsGui(color == null ? new RGB(255, 255, 255) : new RGB(color));
    }

    public static void openColorsGui(RGB initialColor) {
        Minecraft mc = Minecraft.getInstance();
        Map<Block, List<SpriteDetails>> scan = getLastScan();
        if (scan == null) {
            showToast(Component.translatable(Localisation.TOAST_SCAN_IN_PROGRESS));
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        mc.setScreen(new MCRGBClientScreen(new ColorsGuiDescription(initialColor, scan)));
    }

    private static void onSuccess(ScanResult result) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            Map<Block, List<SpriteDetails>> scan = immutableScan(result.blockSprites());
            LAST_SCAN.set(scan);
            if (MCRGBConfig.READ_JSON_FILE.get()) {
                cacheSerializer.save(scan);
            }
            log.info("Color scan completed, {} blocks analyzed.", scan.size());
            showToast(Component.translatable(Localisation.TOAST_RELOADED));
        });
    }

    public static Map<Block, List<SpriteDetails>> getLastScan() {
        return LAST_SCAN.get();
    }

    private static void setLastScan(Map<Block, List<SpriteDetails>> scan) {
        LAST_SCAN.set(immutableScan(scan));
    }

    private static Map<Block, List<SpriteDetails>> immutableScan(Map<Block, List<SpriteDetails>> scan) {
        Map<Block, List<SpriteDetails>> snapshot = new HashMap<>(scan.size());
        scan.forEach((block, sprites) -> snapshot.put(block, List.copyOf(sprites)));
        return Map.copyOf(snapshot);
    }

    private static void onError(Throwable error) {
        if (error instanceof CancellationException) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            log.error("Color scan failed", error);
            mc.gui.setOverlayMessage(Component.literal("Color scan failed."), false);
        });
    }

    public static void showToast(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        SystemToast.addOrUpdate(minecraft.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                Component.translatable(Localisation.TOAST_TITLE), message);
    }
}
