package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.primitive.UnsignedByte;
import engineering.swat.nest.core.tokens.UserDefinedToken;

class B extends UserDefinedToken {
	public final NestBigInteger virtualField;
	public final UnsignedByte x;
	private B(NestBigInteger virtualField, UnsignedByte x) {
		this.virtualField = virtualField;
		this.x = x;
	}
	
	public static B parse(ByteStream source, Context ctx) {
		UnsignedByte x = source.readUnsigned(ctx);
		if (!(x.asValue().sameBytes(NestValue.of(2, 1)))) {
			throw new ParseError("B.x", x);
		}
		NestBigInteger virtualField = x.asValue().asInteger(Sign.UNSIGNED).multiply(NestBigInteger.TWO);
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