package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;

public class TokenList<T extends Token> extends Token {


	private final List<T> contents;
	private final MultipleTokenByteSlice<T> byteView;

	private TokenList(List<T> contents) {
		this.contents = contents;
		byteView = MultipleTokenByteSlice.buildByteView(contents);
	}


	public static <T extends Token> TokenList<T> untilParseFailure(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parser) {
		ByteStream lastSuccess = source.fork();
		List<T> result = new ArrayList<>();
		while (true) {
			try {
				T newValue = parser.apply(source, ctx);
				lastSuccess = source.fork();
				result.add(newValue);
			}
			catch (ParseError e) {
                source.sync(lastSuccess); // reset the stream to the end of the last succesfull parse
				break;
			}
		}
		return new TokenList<>(result);
	}

	public static <T extends Token> TokenList<T> times(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parser, int times) {
		List<T> result = new ArrayList<>(times);
		for (int i = 0; i < times; i++) {
			result.add(parser.apply(source, ctx));
		}
		return new TokenList<>(result);
	}
	
	public static <T extends Token> TokenList<T> of(T... nestedTokens) {
		return new TokenList<T>(Arrays.asList(nestedTokens));
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
	    return byteView;
	}

	@Override
	public NestBigInteger size() {
	    return byteView.size();
	}

}
