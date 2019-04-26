package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.BytesView;
import engineering.swat.nest.core.bytes.TrackedBytesView;

public abstract class Token implements NestValue {
	public abstract TrackedBytesView getTrackedBytes();
	public abstract long size();
	
	@Override
	public BytesView getBytes() {
		return getTrackedBytes();
	}
	
	
}
