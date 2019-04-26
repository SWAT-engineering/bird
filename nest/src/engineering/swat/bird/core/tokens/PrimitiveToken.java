package engineering.swat.bird.core.tokens;

import engineering.swat.bird.core.bytes.BytesView;
import engineering.swat.bird.core.bytes.Context;
import engineering.swat.bird.core.nontokens.BirdInteger;
import engineering.swat.bird.core.nontokens.BirdString;

public abstract class PrimitiveToken extends Token {
	protected final Context ctx;

	public PrimitiveToken(Context ctx) {
		this.ctx = ctx;
	}
	
	public BirdInteger asInteger() {
		return ctx.createInteger(getBytes());
	}

	public BirdString asString() {
		return ctx.createString(getBytes());
	}
}
