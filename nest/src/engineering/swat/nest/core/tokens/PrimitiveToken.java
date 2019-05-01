package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}

	public abstract NestBigInteger asInteger();
	public abstract String asString();
}
