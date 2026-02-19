package sky.client.events.impl.move;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import sky.client.events.Event;

@Getter
@RequiredArgsConstructor
public class EventEntitySpawn extends Event {
    private final Entity entity;
}