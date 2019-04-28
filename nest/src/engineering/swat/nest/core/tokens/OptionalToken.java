package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import java.util.function.BiFunction;

public class OptionalToken<T extends Token> extends Token {
	
	private final T token;

	private OptionalToken(T token) {
		this.token = token;
	}
	
	public static <T extends Token> OptionalToken<T> optional(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parse) {
		ByteStream backup = source.fork();
		try {
			return new OptionalToken<>(parse.apply(source, ctx));
		}
		catch (ParseError e) {
			source.sync(backup); // restore after failure
			return new OptionalToken<>(null);
		}
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		if (token == null) {
			return new TrackedByteSlice() {
				@Override
				public ByteOrigin getOrigin(long index) {
					throw new IndexOutOfBoundsException();
				}

				@Override
				public long size() {
					return 0;
				}

				@Override
				public byte get(long index) {
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
