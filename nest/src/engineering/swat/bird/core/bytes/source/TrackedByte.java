package engineering.swat.bird.core.bytes.source;

import java.net.URI;

public interface TrackedByte {
	long getOffset();
	URI getSource();
	int getValue();
}
