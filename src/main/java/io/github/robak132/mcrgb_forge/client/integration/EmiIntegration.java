package io.github.robak132.mcrgb_forge.client.integration;

import static io.github.robak132.mcrgb_forge.MCRGBMod.MOD_ID;
import static io.github.robak132.mcrgb_forge.client.utils.Utils.elapsedMillis;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.search.EmiSearch;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@Slf4j(topic = MOD_ID)
public final class EmiIntegration {

    private static final long SEARCH_REFRESH_INTERVAL_NANOS = 500_000_000L;
    private static boolean colorDataChanged;
    private static long lastSearchRefresh;
    private static boolean loggedWaitingForIndex;

    private EmiIntegration() {
    }

    public static boolean isIndexReady() {
        boolean ready = EmiReloadManager.isLoaded() || EmiReloadManager.getStatus() < 0;
        if (!ready) {
            if (!loggedWaitingForIndex) {
                log.debug("EMI index not ready yet; status={}", EmiReloadManager.getStatus());
                loggedWaitingForIndex = true;
            }
        } else if (loggedWaitingForIndex) {
            log.debug("EMI index became ready; status={}", EmiReloadManager.getStatus());
            loggedWaitingForIndex = false;
        }
        return ready;
    }

    public static void invalidateColorSearch() {
        colorDataChanged = true;
        log.debug("Marked EMI color search data dirty");
    }

    public static void refreshColorSearch() {
        if (!colorDataChanged || !EmiReloadManager.isLoaded() || EmiSearch.searchThread != null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastSearchRefresh < SEARCH_REFRESH_INTERVAL_NANOS) {
            return;
        }
        colorDataChanged = false;
        String query = EmiApi.getSearchText();
        if (EmiColorSearch.hasColorPrefix(query)) {
            lastSearchRefresh = now;
            log.debug("Refreshing EMI color search for query '{}'", query);
            EmiSearch.search(query);
        }
    }

    public static List<Block> retainIndexedBlocks(Collection<Block> blocks) {
        if (!EmiReloadManager.isLoaded()) {
            log.debug("EMI index not loaded; retaining all {} candidate blocks", blocks.size());
            return List.copyOf(blocks);
        }
        long started = System.nanoTime();
        Set<Block> indexedBlocks = new HashSet<>();
        for (EmiStack stack : EmiApi.getIndexStacks()) {
            ItemStack itemStack = stack.getItemStack();
            if (!itemStack.isEmpty()
                    && itemStack.getItem() instanceof BlockItem blockItem
                    && !EmiHidden.isDisabled(stack)
                    && !EmiHidden.isHidden(stack)) {
                indexedBlocks.add(blockItem.getBlock());
            }
        }
        List<Block> retained = blocks.stream()
                .filter(indexedBlocks::contains)
                .toList();
        log.debug("Filtered blocks against EMI index in {} ms: candidates={}, indexed={}, retained={}",
                elapsedMillis(started), blocks.size(), indexedBlocks.size(), retained.size());
        return retained;
    }

    public static void openColorSearch(String hexColor, int noise, int spatial, String filter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        String color = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        String query = "^" + color + ":" + noise + ":" + spatial
                + (filter.isBlank() ? "" : " " + filter);
        EmiApi.setSearchText(query);
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }
}
