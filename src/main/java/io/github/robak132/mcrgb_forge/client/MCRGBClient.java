package io.github.robak132.mcrgb_forge.client;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.config.MCRGBConfig.EMI_LOADED;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.analysis.ColorScanner;
import io.github.robak132.mcrgb_forge.client.analysis.Palette;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import io.github.robak132.mcrgb_forge.client.gui.ColorsGuiDescription;
import io.github.robak132.mcrgb_forge.client.gui.MCRGBClientScreen;
import io.github.robak132.mcrgb_forge.client.integration.EmiIntegration;
import io.github.robak132.mcrgb_forge.client.serialization.CacheSerializer;
import io.github.robak132.mcrgb_forge.client.serialization.PaletteSerializer;
import io.github.robak132.mcrgb_forge.client.utils.Utils;
import io.github.robak132.mcrgb_forge.config.MCRGBConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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

    private static final long PARTIAL_SCREEN_REFRESH_INTERVAL_NANOS = 250_000_000L;
    private static final ColorScanner scanner = new ColorScanner();
    private static final PaletteSerializer paletteSerializer = new PaletteSerializer();
    private static final CacheSerializer cacheSerializer = new CacheSerializer();
    private static final AtomicReference<Map<Block, List<SpriteDetails>>> LAST_SCAN = new AtomicReference<>();
    private static Future<Map<Block, List<SpriteDetails>>> activeScan = null;
    private static boolean pendingInitialization;
    @Getter
    private static int scanBlockCount;
    private static long lastPartialScreenRefresh;
    private static RGB lastPickerColor = new RGB(255, 255, 255);
    private static TextureNoise lastTextureNoise = TextureNoise.NONE;

    @Getter
    private static List<Palette> palettes;

    private MCRGBClient() {
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        log.debug("Client joined world; EMI_LOADED={}, palettesLoaded={}", EMI_LOADED, palettes != null);
        if (palettes == null) {
            loadPalettes();
            log.debug("Loaded {} saved palettes", palettes.size());
        }

        pendingInitialization = true;
    }

    private static void initializeColorData() {
        pendingInitialization = false;
        log.debug("Initializing color data; cacheExists={}", cacheSerializer.exists());

        Map<Block, List<SpriteDetails>> cached = Map.of();
        if (cacheSerializer.exists()) {
            showToast(Component.translatable(Localisation.TOAST_CACHE_LOADING));
            Map<Block, List<SpriteDetails>> loaded = cacheSerializer.load();
            if (loaded != null && !loaded.isEmpty()) {
                cached = new HashMap<>(loaded);
                if (EMI_LOADED) {
                    int cachedBeforeFilter = cached.size();
                    Set<Block> indexedBlocks = Set.copyOf(EmiIntegration.retainIndexedBlocks(cached.keySet()));
                    cached.keySet().retainAll(indexedBlocks);
                    log.debug("Filtered startup cache with EMI index: {} -> {} blocks",
                            cachedBeforeFilter, cached.size());
                }
            }
        }

        List<Block> targetBlocks = scanner.resolveScanBlocks();
        Map<Block, List<SpriteDetails>> seed = new HashMap<>();
        for (Block block : targetBlocks) {
            List<SpriteDetails> details = cached.get(block);
            if (details != null) {
                seed.put(block, details);
            }
        }

        if (seed.size() >= targetBlocks.size() && !seed.isEmpty()) {
            scanBlockCount = targetBlocks.size();
            setLastScan(seed);
            log.info("Startup cache loaded, {} blocks analyzed.", seed.size());
            showToast(Component.translatable(Localisation.TOAST_CACHE_LOADED, seed.size()));
            return;
        }

        if (!seed.isEmpty()) {
            log.info("Resuming color scan from cache: {}/{} blocks", seed.size(), targetBlocks.size());
        } else {
            log.info("No startup color cache found. Starting scan...");
        }
        startScan(seed);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Map<Block, List<SpriteDetails>> current = LAST_SCAN.get();
        Map<Block, List<SpriteDetails>> snapshot = current == null || current.isEmpty()
                ? null
                : immutableScan(current);
        boolean scanning = activeScan != null && !activeScan.isDone();
        log.debug("Client logging out; cancellingActiveScan={}, cachedBlocks={}",
                scanning, snapshot == null ? 0 : snapshot.size());
        if (activeScan != null) {
            activeScan.cancel(true);
            activeScan = null;
        }
        LAST_SCAN.set(null);
        pendingInitialization = false;
        scanBlockCount = 0;

        if (snapshot != null) {
            cacheSerializer.save(snapshot);
            log.info("Saved {} blocks to color cache on logout{}",
                    snapshot.size(), scanning ? " (scan incomplete)" : "");
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) {
            return;
        }

        if (pendingInitialization) {
            if (!EMI_LOADED || EmiIntegration.isIndexReady()) {
                initializeColorData();
            }
        }
        if (EMI_LOADED) {
            EmiIntegration.refreshColorSearch();
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
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Minecraft.getInstance().player != null
                && activeScan != null
                && !activeScan.isDone()) {
            scanner.replenishScanBatch();
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
        for (SpriteDetails sd : sprites) {
            for (Component line : ColorInfoLines.linesFor(sd)) {
                if (colorLines >= maxLines) {
                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.translatable(Localisation.TOOLTIP_ITEM_SHOW_MORE)
                            .withStyle(ChatFormatting.GRAY));
                    return;
                }
                event.getToolTip().add(line);
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
            log.debug("Ignoring triggerScan; color scan already running");
            return;
        }
        startScan(Map.of());
    }

    public static void refreshScan() {
        if (activeScan != null && !activeScan.isDone()) {
            log.debug("Cancelling in-progress color scan before refresh");
            activeScan.cancel(true);
            activeScan = null;
        }
        startScan(Map.of());
    }

    private static void startScan(Map<Block, List<SpriteDetails>> completed) {
        log.debug("Starting color scan; mode={}, emiLoaded={}, seeded={}",
                MCRGBConfig.COLOR_CALCULATION_MODE.get(), EMI_LOADED, completed.size());
        Map<Block, List<SpriteDetails>> partialScan = new ConcurrentHashMap<>(completed);
        Map<Block, List<SpriteDetails>> partialView = Collections.unmodifiableMap(partialScan);
        scanBlockCount = 0;
        LAST_SCAN.set(partialView);
        if (EMI_LOADED) {
            EmiIntegration.invalidateColorSearch();
        }
        lastPartialScreenRefresh = 0;
        showToast(Component.translatable(Localisation.TOAST_SCAN_STARTED));
        activeScan = scanner.scanAsync(
                completed,
                update -> onPartialScan(partialScan, partialView, update),
                result -> onSuccess(partialView, result),
                error -> onError(partialView, error),
                count -> {
                    scanBlockCount = count;
                    log.debug("Color scan scheduled for {} blocks", count);
                });
    }

    private static void openColorsGui() {
        openColorsGui(lastPickerColor);
    }

    private static void openColorsGuiFromClipboard() {
        Integer color = Utils.hexToInt(Minecraft.getInstance().keyboardHandler.getClipboard());
        openColorsGui(color == null ? lastPickerColor : new RGB(color));
    }

    public static void openColorsGui(RGB initialColor) {
        Minecraft mc = Minecraft.getInstance();
        Map<Block, List<SpriteDetails>> scan = getLastScan();
        if (scan == null) {
            String toast = pendingInitialization && EMI_LOADED && !EmiIntegration.isIndexReady()
                    ? Localisation.TOAST_WAITING_FOR_EMI
                    : Localisation.TOAST_SCAN_IN_PROGRESS;
            showToast(Component.translatable(toast));
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        mc.setScreen(new MCRGBClientScreen(new ColorsGuiDescription(initialColor, lastTextureNoise, scan)));
    }

    public static void rememberPickerState(RGB color, TextureNoise noise) {
        lastPickerColor = color;
        lastTextureNoise = noise;
    }

    private static void onPartialScan(
            Map<Block, List<SpriteDetails>> partialScan,
            Map<Block, List<SpriteDetails>> partialView,
            Map<Block, List<SpriteDetails>> update) {
        Minecraft.getInstance().execute(() -> {
            if (LAST_SCAN.get() != partialView) {
                return;
            }
            partialScan.putAll(update);
            long now = System.nanoTime();
            if (now - lastPartialScreenRefresh >= PARTIAL_SCREEN_REFRESH_INTERVAL_NANOS) {
                refreshOpenColorScreen(partialView);
                lastPartialScreenRefresh = now;
            }
        });
    }

    private static void onSuccess(
            Map<Block, List<SpriteDetails>> expectedScan,
            Map<Block, List<SpriteDetails>> result) {
        if (Thread.currentThread().isInterrupted() || LAST_SCAN.get() != expectedScan) {
            return;
        }
        long snapshotStarted = System.nanoTime();
        Map<Block, List<SpriteDetails>> scan = immutableScan(result);
        log.info("Created immutable color snapshot for {} blocks in {} ms",
                scan.size(), (System.nanoTime() - snapshotStarted) / 1_000_000);
        Minecraft.getInstance().execute(() -> {
            if (LAST_SCAN.get() != expectedScan) {
                return;
            }
            LAST_SCAN.set(scan);
            refreshOpenColorScreen(scan);
            log.info("Color scan completed, {} blocks analyzed.", scan.size());
            showToast(Component.translatable(Localisation.TOAST_RELOADED));
        });
        cacheSerializer.save(scan);
    }

    public static Map<Block, List<SpriteDetails>> getLastScan() {
        return LAST_SCAN.get();
    }

    private static void setLastScan(Map<Block, List<SpriteDetails>> scan) {
        LAST_SCAN.set(immutableScan(scan));
        if (EMI_LOADED) {
            EmiIntegration.invalidateColorSearch();
        }
    }

    private static Map<Block, List<SpriteDetails>> immutableScan(Map<Block, List<SpriteDetails>> scan) {
        Map<Block, List<SpriteDetails>> snapshot = new HashMap<>(scan.size());
        scan.forEach((block, sprites) -> snapshot.put(block, List.copyOf(sprites)));
        return Map.copyOf(snapshot);
    }

    private static void onError(Map<Block, List<SpriteDetails>> expectedScan, Throwable error) {
        if (error instanceof CancellationException) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (LAST_SCAN.get() != expectedScan) {
                return;
            }
            log.error("Color scan failed", error);
            mc.gui.setOverlayMessage(Component.literal("Color scan failed."), false);
        });
    }

    private static void refreshOpenColorScreen(Map<Block, List<SpriteDetails>> scan) {
        if (EMI_LOADED) {
            EmiIntegration.invalidateColorSearch();
        }
        if (Minecraft.getInstance().screen instanceof MCRGBClientScreen screen) {
            screen.updateScan(scan);
        }
    }

    public static void showToast(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        SystemToast.add(minecraft.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                Component.translatable(Localisation.TOAST_TITLE), message);
    }

}
