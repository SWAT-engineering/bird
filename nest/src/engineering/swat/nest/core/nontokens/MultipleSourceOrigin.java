package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class MultipleSourceOrigin implements Origin {
    private final Collection<TrackedByteSlice> sources;
    public MultipleSourceOrigin(TrackedByteSlice... sources) {
        this(Arrays.asList(sources));
    }

    public MultipleSourceOrigin(Collection<TrackedByteSlice> newSources) {
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
        newSources.addAll(sources);
        other.origins().forEach(newSources::add);
        return new MultipleSourceOrigin(newSources);
    }
}
