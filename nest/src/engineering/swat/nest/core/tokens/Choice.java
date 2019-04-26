package engineering.swat.nest.core.tokens;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;

public class Choice {

	@SafeVarargs
	public static Token between(ByteStream source, Context ctx, Case<? extends Token>... cases) {
		long startOffset = source.getOffset();
		for (Case<?> c: cases) {
			try {
				return c.parse(source, ctx);
			}
			catch (ParseError e) {
				source.setOffset(startOffset); // reset stream to begin
				continue;
			}
		}
		throw new ParseError("None of the choices parsed");
		
	}

	public static final class Case<T extends Token> {
		
		private BiFunction<ByteStream, Context, T> parse;
		private Consumer<T> successHandler;

		private Case(BiFunction<ByteStream, Context, T> parse, Consumer<T> successHandler) {
			this.parse = parse;
			this.successHandler = successHandler;
		}

		public static <T extends Token> Case<T> of(BiFunction<ByteStream, Context, T> parse, Consumer<T> successHandler) {
			return new Case<>(parse, successHandler);
			
		}

		public Token parse(ByteStream source, Context ctx) {
			T result = parse.apply(source, ctx);
			successHandler.accept(result);
			return result;
		}

	}

}
