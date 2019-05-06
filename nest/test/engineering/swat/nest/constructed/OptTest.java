package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import org.junit.jupiter.api.Test;

public class OptTest {

	@Test
	void testOptSingleEmpty() {
		assertFalse(OptionalToken.optional(wrap(2), Context.DEFAULT, A::parse).isPresent());
	}
	@Test
	void testOptSingle() {
		assertTrue(OptionalToken.optional(wrap(1), Context.DEFAULT, A::parse).isPresent());
	}
	
	@Test
	void testOptNested() {
		ABA aa = ABA.parse(wrap(1,1), Context.DEFAULT);
		ABA aba = ABA.parse(wrap(1,2,1), Context.DEFAULT);
		assertFalse(aa.b.isPresent());
		assertTrue(aba.b.isPresent());
	}

    private static final class ABA extends UserDefinedToken {

        public final A a;
        public final OptionalToken<B> b;
        public final A a2;

        private ABA(A a, OptionalToken<B> b, A a2) {
            this.a = a;
            this.b = b;
            this.a2 = a2;
        }

        public static ABA parse(ByteStream source, Context ctx) {
            A a = A.parse(source, ctx);
            OptionalToken<B> b = OptionalToken.optional(source, ctx, B::parse);
            A a2 = A.parse(source, ctx);
            return new ABA(a, b, a2);
        }

        @Override
        protected Token[] parsedTokens() {
            return new Token[]{a, b, a2};
        }
    }

}

