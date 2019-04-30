package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;

public abstract class Token implements NestValue {
	public abstract TrackedByteSlice getTrackedBytes();
	public abstract NestBigInteger size();
	
	@Override
	public ByteSlice getBytes() {
		return getTrackedBytes();
	}
	
	
}
