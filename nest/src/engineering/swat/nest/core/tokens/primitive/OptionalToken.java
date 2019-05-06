package engineering.swat.nest.core.tokens.primitive;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Origin;
import engineering.swat.nest.core.nontokens.Tracked;
import engineering.swat.nest.core.tokens.PrimitiveToken;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;
import java.util.Optional;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OptionalToken<T extends Token> extends PrimitiveToken {
	
	private final @Nullable T token;

	private OptionalToken(@Nullable T token, Context ctx) {
		super(ctx);
		this.token = token;
	}
	
	public static <T extends Token> OptionalToken<T> optional(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> parse) {
		ByteStream backup = source.fork();
		try {
			return new OptionalToken<>(parse.apply(source, ctx), ctx);
		}
		catch (ParseError e) {
			ctx.fail("[OptionalToken] Parsing failed: {} after {}", e, source);
			source.sync(backup); // restore after failure
			return new OptionalToken<>(null, ctx);
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

	@Override
	public NestValue asValue() {
		if (token instanceof PrimitiveToken) {
			return ((PrimitiveToken) token).asValue();
        }
		return super.asValue();
	}

	@Override
	public Tracked<String> asString() {
		if (token instanceof PrimitiveToken) {
			return ((PrimitiveToken) token).asString();
		}
		if (token != null) {
		    return super.asString();
		}
		return new Tracked<>(Origin.EMPTY, "");
	}

	@Override
	public <T> T accept(TokenVisitor<T> visitor) {
		return visitor.visitOptionalToken(this);
	}

	public Optional<T> getToken() {
		return Optional.ofNullable(token);
	}
}
