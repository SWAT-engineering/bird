package engineering.swat.nest.core.tokens.operations;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.Token;
import java.util.Optional;
import java.util.function.BiFunction;

public class Choice {

	@SafeVarargs
	public static Optional<Token> between(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<? extends Token>>... cases) {
		ByteStream backup = source.fork();
		for (BiFunction<ByteStream, Context, Optional<? extends  Token>> c: cases) {
			Optional<Token> result = (Optional<Token>) c.apply(source, ctx);
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
}
