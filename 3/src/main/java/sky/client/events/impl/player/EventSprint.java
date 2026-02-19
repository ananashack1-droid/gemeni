package sky.client.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import sky.client.events.Event;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventSprint extends Event {
    private boolean sprinting;
}