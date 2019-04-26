package engineering.swat.nest.core.tokens;

import java.net.URI;
import java.util.Arrays;
import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.BytesView;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.bytes.source.TrackedByte;
import engineering.swat.nest.core.nontokens.NonToken;

public class UnsignedBytes extends PrimitiveToken {

	private final TrackedByte[] data;

	public UnsignedBytes(TrackedByte[] data, Context ctx) {
		super(ctx);
		this.data = data;
	}

	public boolean sameBytes(NestValue other) {
		BytesView otherBytes = other.getBytes();
		if (otherBytes.size() != data.length) {
			return false;
		}
		for (int i=0; i < data.length; i++) {
			if (otherBytes.byteAt(i) != data[i].getValue()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return new TrackedBytesView() {
			@Override
			public long size() {
				return data.length;
			}

			@Override
			public TrackedByte getOriginal(long index) {
				if (index >= data.length) {
					throw new IllegalArgumentException("" + index + " is after: " + data.length);
				}
				return data[(int) index];
			}

		};
	}
	
	@Override
	public long size() {
		return data.length;
	}
	
	@Override
	public String toString() {
		return "read bytes: " + Arrays.toString(data);
	}
}
