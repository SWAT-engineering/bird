package engineering.swat.nest.core.bytes.source;

import java.net.URI;

public interface ByteOrigin {
    long getOffset();
    URI getSource();
}
