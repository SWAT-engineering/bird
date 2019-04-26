package engineering.swat.bird.core.bytes;

import java.net.URI;
import engineering.swat.bird.core.bytes.source.TrackedByte;

public interface TrackedBytesView extends BytesView {
	TrackedByte getOriginal(long index);

	@Override
	default int byteAt(long index) {
		return getOriginal(index).getValue();
	}
}
