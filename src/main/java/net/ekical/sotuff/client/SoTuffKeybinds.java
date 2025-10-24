package net.ekical.sotuff.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class SoTuffKeybinds {
    private static KeyBinding openOptions;

    public static void init() {
        openOptions = new KeyBinding(
                "key.so_tuff.options",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "key.categories.so_tuff"
        );
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(openOptions);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openOptions.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SoTuffOptionsScreen(null));
                }
            }
        });
    }
}
