package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.primitive.MultipleTokenByteSlice;
import java.util.Arrays;

public abstract class UserDefinedToken extends Token {
	
	protected TrackedByteSlice buildTrackedView(Token... tokens) {
		return MultipleTokenByteSlice.buildByteView(Arrays.asList(tokens));
	}

	@Override
	public <T> T accept(TokenVisitor<T> visitor) {
		return visitor.visitUserDefinedToken(this);
	}

	protected abstract Token[] parsedTokens();

	/**
	 * In most cases, this is the same as the parsed tokens, but for nested parsing scenario's this should be overridden
	 * @return
	 */
	protected Token[] allTokens() {
	    return parsedTokens();
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return MultipleTokenByteSlice.buildByteView(Arrays.asList(parsedTokens()));
	}

	@Override
	public NestBigInteger size() {
		return Arrays.stream(parsedTokens()).map(Token::size).reduce(NestBigInteger.ZERO, NestBigInteger::add);
	}
}
