package io.github.robak132.mcrgb_forge.client.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class BlockScan {

    private final Block block;
    private final List<BlockState> states;
    private final Set<Sprite> sprites = new HashSet<>();
    private int nextStateIndex;

    BlockScan(Block block) {
        this.block = block;
        states = new ArrayList<>(block.getStateDefinition().getPossibleStates());
        BlockState defaultState = block.defaultBlockState();
        if (!states.isEmpty() && states.get(0) != defaultState && states.remove(defaultState)) {
            states.add(0, defaultState);
        }
    }

    Block block() {
        return block;
    }

    Set<Sprite> sprites() {
        return sprites;
    }

    int stateCount() {
        return states.size();
    }

    boolean hasPendingState() {
        return nextStateIndex < states.size();
    }

    void collectNextState(Set<Sprite> knownSprites, List<Sprite> discoveredSprites) {
        BlockState state = states.get(nextStateIndex++);
        Sprite.visitSprites(state, sprite -> {
            sprites.add(sprite);
            if (knownSprites.add(sprite)) {
                discoveredSprites.add(sprite);
            }
        });
    }
}
