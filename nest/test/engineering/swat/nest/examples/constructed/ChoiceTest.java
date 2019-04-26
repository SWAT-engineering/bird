package engineering.swat.nest.examples.constructed;

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
import engineering.swat.nest.core.tokens.Choice;
import engineering.swat.nest.core.tokens.Choice.Case;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class ChoiceTest {
	@Test
	void testChoiceAParses() throws URISyntaxException {
		assertEquals(2, AorB.parse(wrap((byte)1), Context.DEFAULT_CONTEXT).virtualField.getValue());
	}

	@Test
	void testChoiceBParses() throws URISyntaxException {
		assertEquals(4, AorB.parse(wrap((byte)2), Context.DEFAULT_CONTEXT).virtualField.getValue());
	}
	@Test
	void testChoiceFails() throws URISyntaxException {
		assertThrows(ParseError.class, () -> {
			AorB.parse(wrap((byte)3), Context.DEFAULT_CONTEXT).virtualField.getValue();
		});
	}

	private ByteStream wrap(byte... bytes) throws URISyntaxException {
		return new ByteStream(ByteWindowBuilder.wrap(ByteBuffer.wrap(bytes), new URI("tmp:///test1")) );
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
	
	private static final class A extends UserDefinedToken {
		public NestInteger virtualField;
		public UnsignedBytes x;
		private A() {}
		
		public static A parse(ByteStream source, Context ctx) {
			A result = new A();
			result.x = source.readUnsigned(1, ctx);
			if (!(result.x.asInteger().getValue() == 1)) {
				throw new ParseError("A.x", result.x);
			}
			result.virtualField = new NestInteger(2 * result.x.asInteger().getValue());
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(x);
		}

		@Override
		public long size() {
			return x.size();
		}
	}

	private static final class B extends UserDefinedToken {
		public NestInteger virtualField;
		public UnsignedBytes x;
		private B() {}
		
		public static B parse(ByteStream source, Context ctx) {
			B result = new B();
			result.x = source.readUnsigned(1, ctx);
			if (!(result.x.asInteger().getValue() == 2)) {
				throw new ParseError("A.x", result.x);
			}
			result.virtualField = new NestInteger(2 * result.x.asInteger().getValue());
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(x);
		}

		@Override
		public long size() {
			return x.size();
		}
	}

}
