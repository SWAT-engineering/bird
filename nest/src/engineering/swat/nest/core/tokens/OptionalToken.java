package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OptionalToken<T extends Token> extends Token {
	
	private final @Nullable T token;

	private OptionalToken(@Nullable T token) {
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
				public ByteOrigin getOrigin(NestBigInteger index) {
					throw new IndexOutOfBoundsException();
				}

				@Override
				public NestBigInteger size() {
					return NestBigInteger.ZERO;
				}

				@Override
				public byte get(NestBigInteger index) {
					throw new IndexOutOfBoundsException();
				}
			};
		}
		return token.getTrackedBytes();
	}

	@Override
	public NestBigInteger size() {
		if (token == null) {
			return NestBigInteger.ZERO;
		}
		return token.size();
	}

	public boolean isPresent() {
		return token != null;
	}
}
