package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.util.Collections;

class SingleSourceOrigin implements Origin {

    private final TrackedByteSlice source;

    public SingleSourceOrigin(TrackedByteSlice source) {
        this.source = source;
    }

    @Override
    public Iterable<TrackedByteSlice> origins() {
        return Collections.singleton(source);
    }

    @Override
    public Origin merge(Origin other) {
        if (other == EMPTY) {
            return this;
        }
        if (this == EMPTY) {
            return other;
        }
        if (other instanceof SingleSourceOrigin) {
            if (((SingleSourceOrigin) other).source == source) {
                return this;
            }
            return new MultipleSourceOrigin(source, ((SingleSourceOrigin) other).source);
        }
        return other.merge(this);
    }

}
