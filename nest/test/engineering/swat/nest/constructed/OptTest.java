package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.tokens.OptionalToken;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class OptTest {

	@Test
	void testOptSingleEmpty() {
		assertFalse(OptionalToken.optional(wrap(2), Context.DEFAULT_CONTEXT, A::parse).isPresent());
	}
	@Test
	void testOptSingle() {
		assertTrue(OptionalToken.optional(wrap(1), Context.DEFAULT_CONTEXT, A::parse).isPresent());
	}
	
	@Test
	void testOptNested() {
		ABA aa = ABA.parse(wrap(1,1), Context.DEFAULT_CONTEXT);
		ABA aba = ABA.parse(wrap(1,2,1), Context.DEFAULT_CONTEXT);
		assertFalse(aa.b.isPresent());
		assertTrue(aba.b.isPresent());
	}
	
	private static final class ABA extends UserDefinedToken {
		public A a;
		public OptionalToken<B> b;
		public A a2;
		
		private ABA() {}
		
		public static ABA parse(ByteStream source, Context ctx) {
			ABA result = new ABA();
			result.a = A.parse(source, ctx);
			result.b = OptionalToken.optional(source, ctx, B::parse);
			result.a2 = A.parse(source, ctx);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(a, b, a2);
		}

		@Override
		public long size() {
			return a.size() + b.size() + a2.size();
		}
	}

}

