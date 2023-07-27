package xyz.wagyourtail.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.api.classes.inventory.Inventory;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IScreen;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.6.5
 */
@Event(value = "OpenContainer", cancellable = true)
public class EventOpenContainer extends BaseEvent {
    public final Inventory<?> inventory;
    public final IScreen screen;

    public EventOpenContainer(HandledScreen<?> screen) {
        this.inventory = Inventory.create(screen);
        this.screen = (IScreen) screen;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\", \"inventory\": %s}", this.getEventName(), JsMacros.getScreenName((Screen) screen), inventory);
    }

}
