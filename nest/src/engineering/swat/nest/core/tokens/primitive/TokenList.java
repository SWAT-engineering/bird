package engineering.swat.nest.core.tokens.primitive;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.PrimitiveToken;
import engineering.swat.nest.core.tokens.Token;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TokenList<T extends Token> extends PrimitiveToken implements Iterable<T> {
	private final List<T> contents;

	private TokenList(List<T> contents, Context ctx) {
	    super(ctx);
		this.contents = Collections.unmodifiableList(contents);
	}


	/**
	 *
	 * @param source
	 * @param ctx
	 * @param parser
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> TokenList<T> untilParseFailure(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<T>> parser) {
		ByteStream lastSuccess = source.fork();
		List<T> result = new ArrayList<>();
		Optional<T> newValue;
		while ((newValue = parser.apply(source, ctx)).isPresent()) {
			ctx.trace("[TokenList::untilParseFailure] success parse at: {}", source);
            lastSuccess = source.fork();
            result.add(newValue.get());
		}
		ctx.trace("[TokenList::untilParseFailure] failed after: {}", source);
		source.sync(lastSuccess); // reset the stream to the end of the last successful parse
		return new TokenList<>(result, ctx);
	}

	/**
	 *
	 * @param source
	 * @param ctx
	 * @param parser
	 * @param times
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> Optional<TokenList<T>> times(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<T>> parser, int times) {
		List<T> result = new ArrayList<>(times);
		for (int i = 0; i < times; i++) {
			Optional<T> newEntry = parser.apply(source, ctx);
			if (newEntry.isPresent()) {
				ctx.trace("[TokenList::times] success parse at: {} ({}-nth)", source, i);
				result.add(newEntry.get());
			}
			else {
			    ctx.fail("[TokenList::times] Failed to parse at {}", i);
				return Optional.empty();
			}
		}
		return Optional.of(new TokenList<>(result, ctx));
	}

	/**
	 *
	 * @param source
	 * @param ctx
	 * @param parser
	 * @param whileCondition
	 * @param <T>
	 * @return
	 */
	public static <T extends Token> Optional<TokenList<T>> parseWhile(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<T>> parser, Predicate<T> whileCondition) {
		List<T> result = new ArrayList<>();
		while (true) {
			ByteStream lastSuccess = source.fork();
			Optional<T> parsedValue = parser.apply(source, ctx);
			if (parsedValue.isPresent()) {
				T actualValue = parsedValue.get();
			    ctx.trace("[TokenList::parseWhile] parse successful, checking if matched: {}",  actualValue);
				if (whileCondition.test(actualValue)) {
					result.add(actualValue);
				} else {
					ctx.trace("[TokenList::parseWhile] predicate returned false");
					source.sync(lastSuccess);
					break;
				}
			}
			else {
				ctx.fail("[TokenList::parseWhile] failed to parse {} at {}", parser, source);
				return Optional.empty();
			}
		}
		return Optional.of(new TokenList<>(result, ctx));
    }

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
}
