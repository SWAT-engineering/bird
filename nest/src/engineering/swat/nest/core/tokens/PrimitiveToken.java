package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.BytesView;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.nontokens.NestString;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}
	
	public NestInteger asInteger() {
		return ctx.createInteger(getBytes());
	}

	public NestString asString() {
		return ctx.createString(getBytes());
	}
}
