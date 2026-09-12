package io.github.robak132.mcrgb_forge.client.gui.widgets;

import io.github.robak132.libgui_forge.widget.WTextField;
import io.github.robak132.libgui_forge.widget.data.InputResult;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class WSmartTextField extends WTextField {

    private boolean wasFocused = false;
    private String lastCommittedText = "";

    private Consumer<String> onCommit;

    public WSmartTextField() {
        super();
    }

    public WSmartTextField(Component suggestion) {
        super(suggestion);
    }

    public void setCommitListener(Consumer<String> listener) {
        this.onCommit = listener;
    }

    @Override
    public InputResult onKeyPressed(int ch, int key, int modifiers) {
        if (ch == GLFW.GLFW_KEY_ENTER || ch == GLFW.GLFW_KEY_KP_ENTER) {
            commitIfChanged();
            return InputResult.PROCESSED;
        }
        return super.onKeyPressed(ch, key, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        boolean focused = this.isFocused();

        if (focused && !wasFocused) {
            wasFocused = true;
            lastCommittedText = getText();
            return;
        }

        if (!focused && wasFocused) {
            wasFocused = false;
            commitIfChanged();
        }
    }

    private void commitIfChanged() {
        String current = getText();

        if (!current.equals(lastCommittedText)) {
            lastCommittedText = current;
            if (onCommit != null) {
                onCommit.accept(current);
            }
        }
    }

    public void commit() {
        commitIfChanged();
    }
}
