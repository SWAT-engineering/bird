package engineering.swat.bird.core.tokens;

import engineering.swat.bird.core.BirdValue;
import engineering.swat.bird.core.bytes.BytesView;
import engineering.swat.bird.core.bytes.TrackedBytesView;

public abstract class Token implements BirdValue {
	public abstract TrackedBytesView getTrackedBytes();
	public abstract long size();
	
	@Override
	public BytesView getBytes() {
		return getTrackedBytes();
	}
	
	
}
