package engineering.swat.nest.core.nontokens;

public class Tracked<T> {
    private final Origin origin;
    private final T value;

    public Tracked(Origin origin, T value) {
        this.origin = origin;
        this.value = value;
    }

    public Origin origin() {
        return origin;
    }

    public T get() {
        return value;
    }
}
