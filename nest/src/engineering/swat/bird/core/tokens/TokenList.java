package engineering.swat.bird.core.tokens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import engineering.swat.bird.core.ParseError;
import engineering.swat.bird.core.bytes.ByteStream;
import engineering.swat.bird.core.bytes.Context;
import engineering.swat.bird.core.bytes.TrackedBytesView;

public class TokenList<T extends Token> extends Token {


	private final List<T> contents;
	private final long[] sizes;
	private final long fullSize;

	private TokenList(List<T> contents) {
		this.contents = contents; 
		this.sizes = contents.stream().mapToLong(Token::size).toArray();
		this.fullSize = Arrays.stream(sizes).sum();
	}

	public static <T extends Token> TokenList<T> untilParseFailure(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parser) {
		long consumedOffset = source.getOffset();
		List<T> result = new ArrayList<>();
		while (true) {
			try {
				T newValue = parser.apply(source, ctx);
				consumedOffset = source.getOffset();
				result.add(newValue);
			}
			catch (ParseError e) {
				source.setOffset(consumedOffset); // reset the stream to the end of the last succesfull parse
				break;
			}
		}
		return new TokenList<>(result);
	}
	
	public static <T extends Token> TokenList<T> of(T... nestedTokens) {
		return new TokenList<T>(Arrays.asList(nestedTokens));
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return new MultipleTokenBytesView<>(contents, sizes, fullSize );
	}

	@Override
	public long size() {
		return fullSize;
	}

}
