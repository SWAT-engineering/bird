package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class MultipleSourceOrigin implements Origin {
    private final Iterable<TrackedByteSlice> sources;
    public MultipleSourceOrigin(TrackedByteSlice... sources) {
        this(Arrays.asList(sources));
    }

    public MultipleSourceOrigin(Iterable<TrackedByteSlice> newSources) {
        this.sources = newSources;
    }

    @Override
    public Iterable<TrackedByteSlice> origins() {
        return sources;
    }

    @Override
    public Origin merge(Origin other) {
        if (other == EMPTY) {
            return this;
        }
        List<TrackedByteSlice> newSources = new ArrayList<>();
        sources.forEach(newSources::add);
        other.origins().forEach(newSources::add);
        return new MultipleSourceOrigin(newSources);
    }
}
