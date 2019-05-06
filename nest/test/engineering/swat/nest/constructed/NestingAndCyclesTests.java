package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class NestingAndCyclesTests {
	
	@Test
	void checkCyclicNestingWorks() {
		byte[] input = "Hcdcdeefdeggh0".getBytes(StandardCharsets.US_ASCII);
		assertEquals(input.length, Start.parse(wrap(input), Context.DEFAULT).get().size().intValueExact());
		
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

		public static Optional<Start> parse(ByteStream source, Context ctx) {
			ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
			Optional<UnsignedBytes> header = source.readUnsigned(1, ctx);
			if (!header.isPresent() || !header.get().asString().get().equals("H")) {
				ctx.fail("[Start.header] {}", header);
				return Optional.empty();
			}
			Optional<Node> initial = Node.parse(source, ctx);
			if (!initial.isPresent()) {
				ctx.fail("[Start.initial] {}", initial);
				return Optional.empty();
			}
			Optional<Loop> loop = Loop.parse(source, ctx, initial.get());
			if (!loop.isPresent()) {
				ctx.fail("[Start.loop] {}", loop);
				return Optional.empty();
			}
			return Optional.of(new Start(header.get(), initial.get(), loop.get()));
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
		
		public static Optional<Node> parse(ByteStream source, Context ctx) {
			Optional<UnsignedBytes> a = source.readUnsigned(1, ctx);
			if (!a.isPresent()) {
				return Optional.empty();
			}
			Optional<UnsignedBytes> b = source.readUnsigned(1, ctx);
			if (!b.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(new Node(a.get(), b.get()));
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
		
		public static Optional<Loop> parse(ByteStream source, Context ctx, Node n) {
			Optional<Token> entry = Choice.between(source, ctx,
					(s, c) -> Loop$1.parse(s, c, n),
					(s, c) -> Loop$2.parse(s, c, n)
			);
			if (!entry.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(new Loop(entry.get()));
		}
		
		private static final class Loop$1 extends UserDefinedToken {
			private final UnsignedBytes $dummy1;
			
			public Loop$1(UnsignedBytes $dummy1) {
				this.$dummy1 = $dummy1;
			}


			public static Optional<Loop$1> parse(ByteStream source, Context ctx, Node n) {
				Optional<UnsignedBytes> $dummy1 = source.readUnsigned(1, ctx);
				if (!$dummy1.isPresent() || !($dummy1.get().asString().get().equals("0"))) {
					ctx.fail("[Loop$1._] wrong");
					return Optional.empty();
				}
				return Optional.of(new Loop$1($dummy1.get()));
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


			public static Optional<Loop$2> parse(ByteStream source, Context ctx, Node n) {
				Optional<UnsignedBytes> aRef = source.readUnsigned(1, ctx);
				if (!aRef.isPresent() || !(aRef.get().sameBytes(n.a))) {
					ctx.fail("Loop$2.aRef: {}", aRef);
					return Optional.empty();
				}
				Optional<Node> n1 = Node.parse(source, ctx);
				if (!n1.isPresent() || !n1.get().a.sameBytes(n.b)) {
					ctx.fail("Loop$2.n1: {}", n1);
					return Optional.empty();
				}
				Optional<Node> n2 = Node.parse(source, ctx);
				if (!n2.isPresent() || !n2.get().a.sameBytes(n1.get().b)) {
					ctx.fail("Loop$2.n2: {}", n2);
					return Optional.empty();
				}
				Optional<Loop> l = Loop.parse(source, ctx, n1.get());
				if (!l.isPresent()) {
					ctx.fail("Loop$2.l: {}", l);
					return Optional.empty();
				}
				return Optional.of(new Loop$2(aRef.get(), n1.get(), n2.get(), l.get()));
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
