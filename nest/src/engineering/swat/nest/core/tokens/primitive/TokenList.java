package engineering.swat.nest.core.tokens.primitive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.NestParseFunction;
import engineering.swat.nest.core.tokens.PrimitiveToken;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;

/**
 * A list of zero or more tokens
 * @param <T> type of the tokens inside the list
 */
public class TokenList<T extends Token> extends PrimitiveToken implements Iterable<T> {


	private final List<T> contents;

	private TokenList(List<T> contents, Context ctx) {
	    super(ctx);
		this.contents = Collections.unmodifiableList(contents);
	}


	/**
	 * Read from the stream until the function provided as {@code parser} parameter fails.
	 * @param source
	 * @param ctx
	 * @param parser function that should parse the token
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> TokenList<T> untilParseFailure(ByteStream source, Context ctx, NestParseFunction<T> parser) {
		ByteStream lastSuccess = source.fork();
		List<T> result = new ArrayList<>();
		while (true) {
			try {
				T newValue = parser.apply(source, ctx);
				lastSuccess = source.fork();
				result.add(newValue);
			}
			catch (ParseError e) {
				ctx.fail("[TokenList::untilParseFailure] Parsing failed: {} after {}", e, source);
                source.sync(lastSuccess); // reset the stream to the end of the last successful parse
				break;
			}
		}
		return new TokenList<>(result, ctx);
	}

	/**
	 * Construct a list that contains {@code times} entries of T
	 * @param source
	 * @param ctx
	 * @param parser
	 * @param times
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> TokenList<T> times(ByteStream source, Context ctx, NestParseFunction<T> parser, int times) {
		List<T> result = new ArrayList<>(times);
		for (int i = 0; i < times; i++) {
			result.add(parser.apply(source, ctx));
		}
		return new TokenList<>(result, ctx);
	}

	/**
	 * Construct a list that parses an entry, and then calls a predicate if it should be included in the list. The stream is reverted to the point of the last added entry.
	 * @param source
	 * @param ctx
	 * @param parser
	 * @param whileCondition
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> TokenList<T> parseWhile(ByteStream source, Context ctx, NestParseFunction<T> parser, Predicate<T> whileCondition) {
		List<T> result = new ArrayList<>();
		while (true) {
			ByteStream lastSuccess = source.fork();
			T parsedValue = parser.apply(source, ctx);
			if (whileCondition.test(parsedValue)) {
				result.add(parsedValue);
			}
			else {
				source.sync(lastSuccess);
				break;
			}
		}
		return new TokenList<>(result, ctx);
    }

	/**
	 * Create a token list to concat multiple tokens together
	 * @param ctx
	 * @param nestedTokens
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> TokenList<T> of(Context ctx, T... nestedTokens) {
		return new TokenList<T>(Arrays.asList(nestedTokens), ctx);
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
	    return MultipleTokenByteSlice.buildByteView(contents);
	}

	@Override
	public NestBigInteger size() {
		return contents.stream().map(Token::size).reduce(NestBigInteger.ZERO, NestBigInteger::add);
	}

	/**
	 * Amount of tokens in the list
	 * @return
	 */
	public int length() {
		return contents.size();

	}

	public T get(int index) {
	    return contents.get(index);
	}

	@Override
	public Iterator<T> iterator() {
		return contents.iterator();
	}
	
	public Stream<T> stream() {
		return contents.stream();
	}

	@Override
	public <T> T accept(TokenVisitor<T> visitor) {
		return visitor.visitTokenList(this);
	}
}
