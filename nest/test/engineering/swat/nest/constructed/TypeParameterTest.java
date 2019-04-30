package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

public class TypeParameterTest {

    @Test
    void twoByteDoubleParseWithT() {
        GenT<UnsignedBytes> parsed = GenT.parse(wrap(1, 2), Context.DEFAULT, (s,c) -> s.readUnsigned(1, c));
        assertEquals(1, parsed.field1.asInteger().intValueExact());
        assertEquals(2, parsed.field2.asInteger().intValueExact());
    }


    private static class GenT<T extends Token>  extends UserDefinedToken {
        public final T field1;
        public final T field2;
        private GenT(T field1, T field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public static <T extends Token> GenT<T> parse(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> tParser) {
            T field1 = tParser.apply(source, ctx);
            T field2 = tParser.apply(source, ctx);
            return new GenT<>(field1, field2);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return field1.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return field1.size();
        }
    }

}
