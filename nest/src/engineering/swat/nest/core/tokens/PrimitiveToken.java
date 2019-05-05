package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Tracked;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}

	public abstract NestValue asValue();
	public abstract Tracked<String> asString();
}
