package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.TokenList;

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
		assertEquals(3, TokenList.times(wrap(1,1,1,1), Context.DEFAULT, A::parse, 3).size().intValueExact());
	}

	@Test
	void testBoundedThrows() {
		assertThrows(ParseError.class, () -> TokenList.times(wrap(1,1,2), Context.DEFAULT, A::parse, 3));
	}

	@Test
	void testBoundedContinuesAtRightPosition() {
		ByteStream source = wrap(1,1,2);
		assertEquals(2, TokenList.times(source, Context.DEFAULT, A::parse, 2).size().intValueExact());
		assertEquals(1, TokenList.times(source, Context.DEFAULT, B::parse, 1).size().intValueExact());
	}
}

