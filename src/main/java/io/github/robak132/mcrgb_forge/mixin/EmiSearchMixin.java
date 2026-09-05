package io.github.robak132.mcrgb_forge.mixin;

import dev.emi.emi.api.stack.EmiIngredient;
import io.github.robak132.mcrgb_forge.client.integration.EmiColorSearch;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "dev.emi.emi.search.EmiSearch", remap = false)
public abstract class EmiSearchMixin {

    @ModifyVariable(method = "search", at = @At("HEAD"), argsOnly = true)
    private static String mcrgb$parseColorPrefix(String query) {
        return EmiColorSearch.parseAndStripColorPrefix(query);
    }

    @ModifyVariable(method = "apply", at = @At("HEAD"), argsOnly = true)
    private static List<EmiIngredient> mcrgb$sortSearchResults(List<EmiIngredient> stacks) {
        return EmiColorSearch.sortByColor(stacks);
    }
}
