package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import java.nio.ByteOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class Token {

	public abstract TrackedByteSlice getTrackedBytes();
	public abstract NestBigInteger size();

	public boolean sameBytes(Token other) {
		return getTrackedBytes().sameBytes(other.getTrackedBytes());
	}

	public boolean sameBytes(NestValue other) {
		return getTrackedBytes().sameBytes(other.getBytes(ByteOrder.BIG_ENDIAN));
	}

	public abstract  <T> T accept(TokenVisitor<T> visitor);


	@Override
	public boolean equals(@Nullable Object obj) {
		throw new UnsupportedOperationException("Do not call equals on tokens, use sameBytes, or compare the slice from getTrackedBytes");
	}
}
