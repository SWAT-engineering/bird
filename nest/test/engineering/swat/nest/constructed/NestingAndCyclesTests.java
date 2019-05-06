package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class NestingAndCyclesTests {
	
	@Test
	void checkCyclicNestingWorks() {
		byte[] input = "Hcdcdeefdeggh0".getBytes(StandardCharsets.US_ASCII);
		assertEquals(input.length, Start.parse(wrap(input), Context.DEFAULT).size().intValueExact());
		
	}

	// translation of nesting_and_cycles.bird
	public static final class Start extends UserDefinedToken {
		public final UnsignedBytes header;
		public final Node initial;
		public final Loop loop;
		private Start(UnsignedBytes header, Node initial, Loop loop) {
			this.header = header;
			this.initial = initial;
			this.loop = loop;
		}

		public static Start parse(ByteStream source, Context ctx) {
			ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
			UnsignedBytes header = source.readUnsigned(1, ctx);
			if (!header.asString().get().equals("H")) {
				throw new ParseError("Start.header", header);
			}
			Node initial = Node.parse(source, ctx);
			Loop loop = Loop.parse(source, ctx, initial);
			return new Start(header, initial, loop);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(header, initial, loop);
		}

		@Override
		public NestBigInteger size() {
			return header.size().add(initial.size().add(loop.size()));
		}
	}
	
	public static final class Node extends UserDefinedToken {
		public final UnsignedBytes a;
		public final UnsignedBytes b;
		private Node(UnsignedBytes a, UnsignedBytes b) {
			this.a = a;
			this.b = b;
		}
		
		public static Node parse(ByteStream source, Context ctx) {
			UnsignedBytes a = source.readUnsigned(1, ctx);
			UnsignedBytes b = source.readUnsigned(1, ctx);
			return new Node(a, b);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(a, b);
		}

		@Override
		public NestBigInteger size() {
			return a.size().add(b.size());
		}
	}
	
	public static final class Loop extends UserDefinedToken{
		public final Token entry;

		private Loop(Token entry) { this.entry = entry; }
		
		public static Loop parse(ByteStream source, Context ctx, Node n) {
			Token entry = Choice.between(source, ctx, 
					Case.of((s, c) -> Loop$1.parse(s, c, n), x -> {}),
					Case.of((s, c) -> Loop$2.parse(s, c, n), x -> {})
			);
			return new Loop(entry);
		}
		
		private static final class Loop$1 extends UserDefinedToken {
			private final UnsignedBytes $dummy1;
			
			public Loop$1(UnsignedBytes $dummy1) {
				this.$dummy1 = $dummy1;
			}


			public static Loop$1 parse(ByteStream source, Context ctx, Node n) {
				UnsignedBytes $dummy1 = source.readUnsigned(1, ctx);
				if (!($dummy1.asString().get().equals("0"))) {
					throw new ParseError("Loop$1._", $dummy1);
				}
				return new Loop$1($dummy1);
			}


			@Override
			public TrackedByteSlice getTrackedBytes() {
				return $dummy1.getTrackedBytes();
			}

			@Override
			public NestBigInteger size() {
				return $dummy1.size();
			}
		}

		private static final class Loop$2 extends UserDefinedToken {
			public final UnsignedBytes aRef;
			public final Node n1;
			public final Node n2;
			public final Loop l;
			
			private Loop$2(UnsignedBytes aRef, Node n1, Node n2, Loop l) {
				this.aRef = aRef;
				this.n1 = n1;
				this.n2 = n2;
				this.l = l;
			}


			public static Loop$2 parse(ByteStream source, Context ctx, Node n) {
				UnsignedBytes aRef = source.readUnsigned(1, ctx);
				if (!(aRef.sameBytes(n.a))) {
					throw new ParseError("Loop$2.aRef", aRef);
				}
				Node n1 = Node.parse(source, ctx);
				if (!n1.a.sameBytes(n.b)) {
					throw new ParseError("Loop$2.n1", n1);
				}
				Node n2 = Node.parse(source, ctx);
				if (!n2.a.sameBytes(n1.b)) {
					throw new ParseError("Loop$2.n2", n2);
				}
				Loop l = Loop.parse(source, ctx, n1);
				return new Loop$2(aRef, n1, n2, l);
			}
			

			@Override
			public TrackedByteSlice getTrackedBytes() {
				return buildTrackedView(aRef, n1, n2, l);
			}

			@Override
			public NestBigInteger size() {
				return aRef.size().add(n1.size()).add(n2.size()).add(l.size());
			}
			
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
