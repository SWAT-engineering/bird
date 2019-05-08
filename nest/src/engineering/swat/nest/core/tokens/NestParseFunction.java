package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import java.util.function.BiFunction;

@FunctionalInterface
public interface NestParseFunction<T extends Token> extends BiFunction<ByteStream, Context, T> {

}
