package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

class B extends UserDefinedToken {
	public final NestBigInteger virtualField;
	public final UnsignedBytes x;
	private B(NestBigInteger virtualField, UnsignedBytes x) {
		this.virtualField = virtualField;
		this.x = x;
	}
	
	public static B parse(ByteStream source, Context ctx) {
		UnsignedBytes x = source.readUnsigned(1, ctx);
		if (!(x.getByteAt(NestBigInteger.ZERO) == 2)) {
			throw new ParseError("A.x", x);
		}
		NestBigInteger virtualField = x.asInteger().multiply(NestBigInteger.TWO);
		return new B(virtualField, x);
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return buildTrackedView(x);
	}

	@Override
	public NestBigInteger size() {
		return x.size();
	}
}