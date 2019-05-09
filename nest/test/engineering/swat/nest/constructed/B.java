package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;

class B extends UserDefinedToken {

	public final NestBigInteger virtualField;
	public final UnsignedBytes x;

	private B(NestBigInteger virtualField, UnsignedBytes x) {
		this.virtualField = virtualField;
		this.x = x;
	}

	public static B parse(ByteStream source, Context ctx) {
		UnsignedBytes x = source.readUnsigned(1, ctx);
		if (!(x.asValue().sameBytes(NestValue.of(2, 1)))) {
			throw new ParseError("B.x", x);
		}
		NestBigInteger virtualField = x.asValue().asInteger().multiply(NestBigInteger.TWO);
		return new B(virtualField, x);
	}

	@Override
	protected Token[] parsedTokens() {
		return new Token[]{x};
	}
}