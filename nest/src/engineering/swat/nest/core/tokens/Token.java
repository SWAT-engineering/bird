package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.nio.ByteOrder;

public abstract class Token {
	public abstract TrackedByteSlice getTrackedBytes();
	public abstract NestBigInteger size();
}
