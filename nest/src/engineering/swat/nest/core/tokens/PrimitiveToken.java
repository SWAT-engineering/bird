package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestValue;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}

	public abstract NestValue asValue();
	public abstract String asString();
}
