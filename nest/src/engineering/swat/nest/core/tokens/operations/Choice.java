package engineering.swat.nest.core.tokens.operations;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.Token;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Choice {

	@SafeVarargs
	public static Optional<Token> between(ByteStream source, Context ctx, Case<? extends Token>... cases) {
		ByteStream backup = source.fork();
		for (Case<?> c: cases) {
			Optional<Token> result = c.parse(source, ctx);
			if (result.isPresent()) {
				return result;
			}
			else {
			    ctx.trace("[Choice::between] failed to parse case {}", c);
				source.sync(backup); // reset the stream to before the parse
			}
		}
		ctx.fail("[Choice::between] none of the cases matched");
		return Optional.empty();
	}

	public static final class Case<T extends Token> {
		
		private BiFunction<ByteStream, Context, Optional<T>> parse;
		private Consumer<T> successHandler;

		private Case(BiFunction<ByteStream, Context, Optional<T>> parse, Consumer<T> successHandler) {
			this.parse = parse;
			this.successHandler = successHandler;
		}

		public static <T extends Token> Case<T> of(BiFunction<ByteStream, Context, Optional<T>> parse, Consumer<T> successHandler) {
			return new Case<>(parse, successHandler);
		}

		public Optional<Token> parse(ByteStream source, Context ctx) {
			Optional<T> result = parse.apply(source, ctx);
			result.ifPresent(successHandler);
			return (Optional<Token>) result;
		}

	}

}
