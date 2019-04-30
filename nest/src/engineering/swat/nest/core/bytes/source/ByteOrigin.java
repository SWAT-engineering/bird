package engineering.swat.nest.core.bytes.source;

import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.net.URI;

public interface ByteOrigin {
    NestBigInteger getOffset();
    URI getSource();
}
