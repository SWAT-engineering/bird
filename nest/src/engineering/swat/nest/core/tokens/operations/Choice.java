package engineering.swat.nest.core.tokens.operations;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.Token;
import java.util.function.BiFunction;

public class Choice {

	@SafeVarargs
	public static Token between(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Token> ... cases) {
		ByteStream backup = source.fork();
		for (BiFunction<ByteStream, Context, Token> c: cases) {
			try {
				return c.apply(source, ctx);
			}
			catch (ParseError e) {
				ctx.fail("[Choice] failed: {}", e);
			    source.sync(backup); // reset the stream to before the parse
			}
		}
		throw new ParseError("None of the choices parsed");
	}
}
