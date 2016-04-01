package events;

public class Event {

    private String name;
    private Object data;

    public Event(String n, Object d) {
        name = n;
        data = d;
    }

    public Object getData() {
        return data;
    }

    public String getName() {
        return name;
    }
}
