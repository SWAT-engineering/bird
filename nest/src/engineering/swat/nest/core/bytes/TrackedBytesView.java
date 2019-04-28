package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.bytes.source.ByteOrigin;

public interface TrackedBytesView extends BytesView {
	ByteOrigin getOrigin(long index);
}
