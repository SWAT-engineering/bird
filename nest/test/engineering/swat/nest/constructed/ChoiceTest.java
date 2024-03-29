package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ChoiceTest {
	@Test
	void testChoiceAParses() throws URISyntaxException {
		assertEquals(2, AorB.parse(wrap(1), Context.DEFAULT).virtualField.intValueExact());
	}

	@Test
	void testChoiceBParses() throws URISyntaxException {
		assertEquals(4, AorB.parse(wrap(2), Context.DEFAULT).virtualField.intValueExact());
		assertEquals(2, AorB.parse(wrap(2), Context.DEFAULT).x.getByteAt(NestBigInteger.ZERO));
	}

	@Test
	void testChoiceFails() throws URISyntaxException {
		assertThrows(ParseError.class, () -> {
			AorB.parse(wrap(3), Context.DEFAULT).virtualField.intValueExact();
		});
	}


	private static final class AorB extends UserDefinedToken {

		public final NestBigInteger virtualField;
		public final UnsignedBytes x;
		public final Token entry;

		private AorB(Token entry, NestBigInteger virtualField, UnsignedBytes x) {
			this.entry = entry;
			this.virtualField = virtualField;
			this.x = x;
		}

		public static AorB parse(ByteStream source, Context ctx) {
			final AtomicReference<NestBigInteger> virtualField = new AtomicReference<>();
			final AtomicReference<UnsignedBytes> x = new AtomicReference<>();
			Token entry = Choice.between(source, ctx,
					(s, c) -> {
						A result = A.parse(s, c);
						virtualField.set(result.virtualField);
						x.set(result.x);
						return result;
					},
					(s, c) -> {
						B result = B.parse(s, c);
						virtualField.set(result.virtualField);
						x.set(result.x);
						return result;
					}
			);
			return new AorB(entry, virtualField.get(), x.get());
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{entry};
		}
	}

}
