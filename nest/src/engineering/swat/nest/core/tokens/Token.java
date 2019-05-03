package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.nio.ByteOrder;

public abstract class Token {
	public abstract TrackedByteSlice getTrackedBytes();
	public abstract NestBigInteger size();

	public boolean sameBytes(Token other) {
		return getTrackedBytes().sameBytes(other.getTrackedBytes());
	}

	@Override
	public boolean equals(Object obj) {
		throw new UnsupportedOperationException("Do not call equals on tokens, use sameBytes, or compare the slice from getTrackedBytes");
	}
}
