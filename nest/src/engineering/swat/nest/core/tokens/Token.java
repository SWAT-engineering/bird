package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.TrackedByteSlice;

public abstract class Token implements NestValue {
	public abstract TrackedByteSlice getTrackedBytes();
	public abstract long size();
	
	@Override
	public ByteSlice getBytes() {
		return getTrackedBytes();
	}
	
	
}
