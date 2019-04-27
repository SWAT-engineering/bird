package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.Assert.assertEquals;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;

public class NestingAndCyclesTests {
	
	@Test
	void checkCyclicNestingWorks() {
		byte[] input = "Hcdcdeefdeggh0".getBytes(StandardCharsets.US_ASCII);
		assertEquals(input.length, Start.parse(wrap(input), Context.DEFAULT_CONTEXT).size());
		
	}

	// translation of nesting_and_cycles.bird
	private static final class Start extends UserDefinedToken {
		public UnsignedBytes header;
		public Node initial;
		public Loop loop;
		private Start() {}

		public static Start parse(ByteStream source, Context ctx) {
			ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
			Start result = new Start();
			result.header = source.readUnsigned(1, ctx);
			if (!result.header.asString().equals("H")) {
				throw new ParseError("Start.header", result.header);
			}
			result.initial = Node.parse(source, ctx);
			result.loop = Loop.parse(source, ctx, result.initial);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(header, initial, loop);
		}

		@Override
		public long size() {
			return header.size() + initial.size() + loop.size();
		}
	}
	
	private static final class Node extends UserDefinedToken {
		public UnsignedBytes a;
		public UnsignedBytes b;
		private Node() {}
		
		public static Node parse(ByteStream source, Context ctx) {
			final Node result = new Node();
			result.a = source.readUnsigned(1, ctx);
			result.b = source.readUnsigned(1, ctx);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(a, b);
		}

		@Override
		public long size() {
			return a.size() + b.size();
		}
	}
	
	private static final class Loop extends UserDefinedToken{
		public Token entry;

		private Loop() {}
		
		public static Loop parse(ByteStream source, Context ctx, Node n) {
			Loop result = new Loop();
			result.entry = Choice.between(source, ctx, 
					Case.of((s, c) -> Loop$1.parse(s, c, n), x -> {}),
					Case.of((s, c) -> Loop$2.parse(s, c, n), x -> {})
			);
			return result;
		}
		
		private static final class Loop$1 extends UserDefinedToken {
			private UnsignedBytes $dummy1;
			
			public static Loop$1 parse(ByteStream source, Context ctx, Node n) {
				Loop$1 result = new Loop$1();
				result.$dummy1 = source.readUnsigned(1, ctx);
				if (!(result.$dummy1.asString().equals("0"))) {
					throw new ParseError("Loop$1._", result.$dummy1);
				}
				return result;
			}


			@Override
			public TrackedBytesView getTrackedBytes() {
				return $dummy1.getTrackedBytes();
			}

			@Override
			public long size() {
				return $dummy1.size();
			}
		}

		private static final class Loop$2 extends UserDefinedToken {
			public UnsignedBytes aRef;
			public Node n1;
			public Node n2;
			public Loop l;
			
			public static Loop$2 parse(ByteStream source, Context ctx, Node n) {
				Loop$2 result = new Loop$2();
				result.aRef = source.readUnsigned(1, ctx);
				if (!(result.aRef.equals(n.a))) {
					throw new ParseError("Loop$2.aRef", result.aRef);
				}
				result.n1 = Node.parse(source, ctx);
				if (!result.n1.a.equals(n.b)) {
					throw new ParseError("Loop$2.n1", result.n1);
				}
				result.n2 = Node.parse(source, ctx);
				if (!result.n2.a.equals(result.n1.b)) {
					throw new ParseError("Loop$2.n2", result.n2);
				}
				result.l = Loop.parse(source, ctx, result.n1);
				return result;
			}
			

			@Override
			public TrackedBytesView getTrackedBytes() {
				return buildTrackedView(aRef, n1, n2, l);
			}

			@Override
			public long size() {
				return aRef.size() + n1.size() + n2.size() + l.size();
			}
			
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
