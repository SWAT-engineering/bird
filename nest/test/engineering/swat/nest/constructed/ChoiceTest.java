package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.bytes.source.ByteWindowBuilder;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;

public class ChoiceTest {
	@Test
	void testChoiceAParses() throws URISyntaxException {
		assertEquals(2, AorB.parse(wrap(1), Context.DEFAULT_CONTEXT).virtualField.getValue());
	}

	@Test
	void testChoiceBParses() throws URISyntaxException {
		assertEquals(4, AorB.parse(wrap(2), Context.DEFAULT_CONTEXT).virtualField.getValue());
		assertEquals(2, AorB.parse(wrap(2), Context.DEFAULT_CONTEXT).x.asInteger().getValue());
	}

	@Test
	void testChoiceFails() throws URISyntaxException {
		assertThrows(ParseError.class, () -> {
			AorB.parse(wrap(3), Context.DEFAULT_CONTEXT).virtualField.getValue();
		});
	}

	
	private static final class AorB extends UserDefinedToken {
		public NestInteger virtualField;
		public UnsignedBytes x;
		public Token entry;
		private AorB() {}
		
		public static AorB parse(ByteStream source, Context ctx) {
			final AorB result = new AorB();
			result.entry = Choice.between(source, ctx,
					Case.of((s, c) -> A.parse(s, c), a -> {
						result.virtualField = a.virtualField;
						result.x = a.x;
					}),
					Case.of((s, c) -> B.parse(s, c), b -> {
						result.virtualField = b.virtualField;
						result.x = b.x;
					})
			);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return entry.getTrackedBytes();
		}

		@Override
		public long size() {
			return entry.size();
		}
	}

}
