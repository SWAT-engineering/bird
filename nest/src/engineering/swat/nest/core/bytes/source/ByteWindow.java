package engineering.swat.nest.core.bytes.source;

import engineering.swat.nest.core.bytes.TrackedBytesView;

public interface ByteWindow extends TrackedBytesView {
    ByteWindow slice(long offset, long size);
}
