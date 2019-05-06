package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Origin;
import engineering.swat.nest.core.nontokens.Tracked;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}

	public NestValue asValue() {
		return new NestValue(getTrackedBytes(), ctx);
	}

	public Tracked<String> asString() {
		TrackedByteSlice slice = getTrackedBytes();
		return new Tracked<>(Origin.of(slice), new String(slice.allBytes(), ctx.getEncoding()));
	}
}
