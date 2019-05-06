package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ChoiceTest {
	@Test
	void testChoiceAParses()  {
		assertEquals(2, AorB.parse(wrap(1), Context.DEFAULT).get().virtualField.intValueExact());
	}

	@Test
	void testChoiceBParses()  {
		assertEquals(4, AorB.parse(wrap(2), Context.DEFAULT).get().virtualField.intValueExact());
		assertEquals(2, AorB.parse(wrap(2), Context.DEFAULT).get().x.getByteAt(NestBigInteger.ZERO));
	}

	@Test
	void testChoiceFails()  {
	    assertFalse(AorB.parse(wrap(3), Context.DEFAULT).isPresent());
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
		
		public static Optional<AorB> parse(ByteStream source, Context ctx) {
			final AtomicReference<NestBigInteger> virtualField = new AtomicReference<>();
			final AtomicReference<UnsignedBytes> x = new AtomicReference<>();
			Optional<Token> entry = Choice.between(source, ctx,
					(s, c) -> {
						Optional<A> result = A.parse(s, c);
						if (result.isPresent()) {
							virtualField.set(result.get().virtualField);
							x.set(result.get().x);
						}
						return result;
					},
					(s, c) -> {
						Optional<B> result = B.parse(s, c);
						if (result.isPresent()) {
							virtualField.set(result.get().virtualField);
							x.set(result.get().x);
						}
						return result;
					}
			);
			if (!entry.isPresent()) {
				ctx.fail("[AorB] optional failed to parse");
				return Optional.empty();
			}
			return Optional.of(new AorB(entry.get(), virtualField.get(), x.get()));
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return entry.getTrackedBytes();
		}

		@Override
		public NestBigInteger size() {
			return entry.size();
		}
	}

}
