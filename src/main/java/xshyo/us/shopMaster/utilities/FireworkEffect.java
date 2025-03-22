package xshyo.us.shopMaster.utilities;

import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;

import java.util.ArrayList;
import java.util.List;

public class FireworkEffect {
    private final Type type;
    private final List<Color> colors;
    private final List<Color> fadeColors;
    private final boolean flicker;
    private final boolean trail;

    public FireworkEffect(Type type, boolean flicker, boolean trail) {
        this.type = type;
        this.flicker = flicker;
        this.trail = trail;
        this.colors = new ArrayList<>();
        this.fadeColors = new ArrayList<>();
    }

    public FireworkEffect addColor(Color color) {
        this.colors.add(color);
        return this;
    }

    public FireworkEffect addFadeColor(Color color) {
        this.fadeColors.add(color);
        return this;
    }

    public org.bukkit.FireworkEffect build() {
        return org.bukkit.FireworkEffect.builder()
                .with(type)
                .flicker(flicker)
                .trail(trail)
                .withColor(colors)
                .withFade(fadeColors)
                .build();
    }
}