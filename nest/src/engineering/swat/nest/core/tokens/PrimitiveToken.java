package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestInteger;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}

	public abstract NestInteger asInteger();
}
