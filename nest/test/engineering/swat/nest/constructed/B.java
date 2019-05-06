package engineering.swat.nest.constructed;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.Optional;

class B extends UserDefinedToken {
	public final NestBigInteger virtualField;
	public final UnsignedBytes x;
	private B(NestBigInteger virtualField, UnsignedBytes x) {
		this.virtualField = virtualField;
		this.x = x;
	}
	
	public static Optional<B> parse(ByteStream source, Context ctx) {
		Optional<UnsignedBytes> x = source.readUnsigned(1, ctx);

		if (!x.isPresent()) {
			ctx.fail("[B.x] could not parse");
			return Optional.empty();
		}
		if (!(x.get().asValue().sameBytes(NestValue.of(2, 1)))) {
			ctx.fail("[B.x] not valid");
			return Optional.empty();
		}
		NestBigInteger virtualField = x.get().asValue().asInteger(Sign.UNSIGNED).multiply(NestBigInteger.TWO);
		return Optional.of(new B(virtualField, x.get()));
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