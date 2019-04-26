package engineering.swat.nest.core.tokens;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.bytes.source.TrackedByte;

public class OptionalToken<T extends Token> extends Token {
	
	private final T token;

	private OptionalToken(T token) {
		this.token = token;
	}
	
	public static <T extends Token> OptionalToken<T> optional(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parse) {
		long startOffset = source.getOffset();
		try {
			return new OptionalToken<>(parse.apply(source, ctx));
		}
		catch (ParseError e) {
			source.setOffset(startOffset);
			return new OptionalToken<>(null);
		}
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		if (token == null) {
			return new TrackedBytesView() {
				
				@Override
				public long size() {
					return 0;
				}
				
				@Override
				public TrackedByte getOriginal(long index) {
					throw new IndexOutOfBoundsException();
				}
			};
		}
		return token.getTrackedBytes();
	}

	@Override
	public long size() {
		if (token == null) {
			return 0;
		}
		return token.size();
	}

	public boolean isPresent() {
		return token != null;
	}
}
