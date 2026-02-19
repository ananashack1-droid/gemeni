package sky.client.events.impl.input;


import sky.client.events.Event;

public class EventKey extends Event {
    public int key;

    public EventKey(int key) {
        this.key = key;
    }
}
