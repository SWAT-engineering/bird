package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import org.junit.jupiter.api.Test;

public class RepTest {

	@Test
	void testUnbounded() {
		assertEquals(4, TokenList.untilParseFailure(wrap(1,1,1,1), Context.DEFAULT, A::parse).size().intValueExact());
	}

	@Test
	void testUnboundedStops() {
		assertEquals(4, TokenList.untilParseFailure(wrap(1,1,1,1,2), Context.DEFAULT, A::parse).size().intValueExact());
	}

	@Test
	void testBoundedStops() {
		assertEquals(3, TokenList.times(wrap(1,1,1,1), Context.DEFAULT, A::parse, 3).get().size().intValueExact());
	}

	@Test
	void testBoundedThrows() {
		assertFalse(TokenList.times(wrap(1,1,2), Context.DEFAULT, A::parse, 3).isPresent());
	}

	@Test
	void testBoundedContinuesAtRightPosition() {
		ByteStream source = wrap(1,1,2);
		assertEquals(2, TokenList.times(source, Context.DEFAULT, A::parse, 2).get().size().intValueExact());
		assertEquals(1, TokenList.times(source, Context.DEFAULT, B::parse, 1).get().size().intValueExact());
	}
}

