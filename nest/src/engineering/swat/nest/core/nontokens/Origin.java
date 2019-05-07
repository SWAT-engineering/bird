package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.util.Collection;
import java.util.Collections;

public interface Origin {


    Iterable<TrackedByteSlice> origins();
    Origin merge(Origin other);

    static Origin of(TrackedByteSlice source) {
        return new SingleSourceOrigin(source);
    }

    static Origin of(Collection<TrackedByteSlice> sources) {
        return new MultipleSourceOrigin(sources);
    }

    static Origin of(TrackedByteSlice... sources) {
        return new MultipleSourceOrigin(sources);
    }

    Origin EMPTY = new Origin() {
        @Override
        public Iterable<TrackedByteSlice> origins() {
            return Collections.emptyList();
        }

        @Override
        public Origin merge(Origin other) {
            return other;
        }
    };

}
