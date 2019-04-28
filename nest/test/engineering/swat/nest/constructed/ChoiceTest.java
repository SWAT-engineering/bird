package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
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
		public final NestInteger virtualField;
		public final UnsignedBytes x;
		public final Token entry;
		private AorB(Token entry, NestInteger virtualField, UnsignedBytes x) {
			this.entry = entry;
			this.virtualField = virtualField;
			this.x = x;
		}
		
		public static AorB parse(ByteStream source, Context ctx) {
			final AtomicReference<NestInteger> virtualField = new AtomicReference<>();
			final AtomicReference<UnsignedBytes> x = new AtomicReference<>();
			Token entry = Choice.between(source, ctx,
					Case.of((s, c) -> A.parse(s, c), a -> {
						virtualField.set(a.virtualField);
						x.set(a.x);
					}),
					Case.of((s, c) -> B.parse(s, c), b -> {
						virtualField.set(b.virtualField);
						x.set(b.x);
					})
			);
			return new AorB(entry, virtualField.get(), x.get());
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return entry.getTrackedBytes();
		}

		@Override
		public long size() {
			return entry.size();
		}
	}

}
