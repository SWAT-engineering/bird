package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

class B extends UserDefinedToken {
	public NestInteger virtualField;
	public UnsignedBytes x;
	private B() {}
	
	public static B parse(ByteStream source, Context ctx) {
		B result = new B();
		result.x = source.readUnsigned(1, ctx);
		if (!(result.x.asInteger().getValue() == 2)) {
			throw new ParseError("A.x", result.x);
		}
		result.virtualField = new NestInteger(2 * result.x.asInteger().getValue());
		return result;
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return buildTrackedView(x);
	}

	@Override
	public long size() {
		return x.size();
	}
}