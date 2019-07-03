package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

public class TypeParameterTest {

	@Test
	void twoByteDoubleParseWithT() {
		GenT<UnsignedBytes> parsed = GenT.parse(wrap(1, 2), Context.DEFAULT, (s, c) -> s.readUnsigned(1, c));
		assertEquals(1, parsed.field1.getByteAt(NestBigInteger.ZERO));
		assertEquals(2, parsed.field2.getByteAt(NestBigInteger.ZERO));
	}
	
	@Test
	void twoByteDoubleParseWithMain() {
		Main main = Main.parse(wrap(1, 2), Context.DEFAULT);
		assertEquals(1, main.parsed.field1.getByteAt(NestBigInteger.ZERO));
		assertEquals(2, main.parsed.field2.getByteAt(NestBigInteger.ZERO));
	}
	
	@Test
	void twoByteDoubleParseWithMain2() {
		Main2 main = Main2.parse(wrap(1, 2), Context.DEFAULT);
		assertEquals(1, main.parsed.field1.a.getByteAt(NestBigInteger.ZERO));
	}

	private static class Main extends UserDefinedToken {

		public final GenT<UnsignedBytes> parsed;

		private Main(GenT<UnsignedBytes> parsed) {
			this.parsed = parsed;
		}

		public static Main parse(ByteStream source, Context ctx) {
			GenT<UnsignedBytes> parsed = GenT.parse(source, ctx, (s, c) -> s.readUnsigned(1, c));
			return new Main(parsed);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[] { parsed };
		}

	}
	
	private static class A extends UserDefinedToken {

		public final UnsignedBytes a;

		private A(UnsignedBytes a) {
			this.a = a;
		}

		public static A parse(ByteStream source, Context ctx) {
			UnsignedBytes a = source.readUnsigned(1, ctx);
			return new A(a);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[] { a };
		}

	}

	private static class Main2 extends UserDefinedToken {

		public final GenT<A> parsed;

		private Main2(GenT<A> parsed) {
			this.parsed = parsed;
		}

		public static Main2 parse(ByteStream source, Context ctx) {
			GenT<A> parsed = GenT.parse(source, ctx, (s, c) -> A.parse(s, c));
			return new Main2(parsed);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[] { parsed };
		}

	}
	
	private static class GenT<T extends Token> extends UserDefinedToken {

		public final T field1;
		public final T field2;

		private GenT(T field1, T field2) {
			this.field1 = field1;
			this.field2 = field2;
		}

		public static <T extends Token> GenT<T> parse(ByteStream source, Context ctx,
				BiFunction<ByteStream, Context, T> tParser) {
			T field1 = tParser.apply(source, ctx);
			T field2 = tParser.apply(source, ctx);
			return new GenT<>(field1, field2);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[] { field1, field2 };
		}
	}

}
