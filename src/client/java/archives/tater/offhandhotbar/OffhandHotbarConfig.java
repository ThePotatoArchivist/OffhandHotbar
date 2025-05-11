package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.util.Hand;

public class OffhandHotbarConfig extends MidnightConfig {
    public enum DisplayMode {
        SIDE_BY_SIDE, STACKED, VERTICAL
    }

    @Entry
    public static DisplayMode displayMode = DisplayMode.SIDE_BY_SIDE;
    @Entry
    public static Hand scrollControls = Hand.MAIN_HAND;
    @Entry
    public static Hand keyboardControls = Hand.OFF_HAND;
}
