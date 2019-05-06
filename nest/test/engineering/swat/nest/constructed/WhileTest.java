package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import org.junit.jupiter.api.Test;

public class WhileTest {

    @Test
    void parseUnevenBytes() {
        WhileUnevenParse parsed = WhileUnevenParse.parse(wrap(1, 3, 5, 7, 8), Context.DEFAULT);
        assertEquals(4, parsed.contents.length());
        assertEquals(8, parsed.terminatedAt.getByteAt(NestBigInteger.ZERO));
    }


    private static class WhileUnevenParse extends UserDefinedToken {
        public final TokenList<UnsignedBytes> contents;
        public final UnsignedBytes terminatedAt;

        private WhileUnevenParse(TokenList<UnsignedBytes> contents,
                UnsignedBytes terminatedAt) {
            this.contents = contents;
            this.terminatedAt = terminatedAt;
        }

        public static WhileUnevenParse parse(ByteStream source, Context ctx) {
            TokenList<UnsignedBytes>contents = TokenList.parseWhile(source, ctx,
                    (s, c) -> s.readUnsigned(1, c),
                    ub -> ub.asValue().and(NestValue.of(0x1, 1)).sameBytes(NestValue.of(1, 1))
                    );
            UnsignedBytes terminatedAt = source.readUnsigned(1, ctx);
            return new WhileUnevenParse(contents, terminatedAt);
        }


        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(contents, terminatedAt);
        }

        @Override
        public NestBigInteger size() {
            return contents.size().add(terminatedAt.size());
        }
    }

}
