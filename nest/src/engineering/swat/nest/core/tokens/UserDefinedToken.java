package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.primitive.MultipleTokenByteSlice;
import java.util.Arrays;

/**
 * All user defined tokens should derive from this class, and at the very least implement {@link #parsedTokens()} so that the other operations work as expected
 */
public abstract class UserDefinedToken extends Token {
	
	@Override
	public <T> T accept(TokenVisitor<T> visitor) {
		return visitor.visitUserDefinedToken(this);
	}

	/**
	 * Return all tokens that are parsed directly from the source stream, in the order that they are parsed
	 */
	protected abstract Token[] parsedTokens();

	/**
	 * In most cases, this is the same as the parsed tokens, but for nested parsing scenario's this should be overridden and those fields added
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
