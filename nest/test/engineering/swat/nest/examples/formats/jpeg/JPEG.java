package engineering.swat.nest.examples.formats.jpeg;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.ByteOrder;
import java.util.Optional;

public class JPEG  {
	
	public static class Format extends UserDefinedToken {
        public final Header $anon1;
        public final TokenList<SizedScan> $anon2;
        public final Footer $anon3;
        private Format(Header $anon1, TokenList<SizedScan> $anon2, Footer $anon3) {
			this.$anon1 = $anon1;
			this.$anon2 = $anon2;
			this.$anon3 = $anon3;
		}

		public static Optional<Format> parse(ByteStream source, Context ctx) {
			ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
			Optional<Header> $anon1 = Header.parse(source, ctx);
			if (!$anon1.isPresent()) {
			    ctx.fail("Format._ missing at {}", source);
				return Optional.empty();
			}
			TokenList<SizedScan> $anon2 = TokenList.untilParseFailure(source, ctx, (s, c) -> SizedScan.parse(s, c));
			Optional<Footer> $anon3 = Footer.parse(source, ctx);
			if (!$anon3.isPresent()) {
				ctx.fail("Format._ missing at {}", source);
				return Optional.empty();
			}
			return Optional.of(new Format($anon1.get(), $anon2, $anon3.get()));
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView($anon1, $anon2, $anon3);
		}

		@Override
		public NestBigInteger size() {
			return $anon1.size().add($anon2.size()).add($anon3.size());
		}
	}
	
	public static class Header extends UserDefinedToken {
		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;
		private Header(UnsignedBytes marker, UnsignedBytes identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}
		
		public static Optional<Header> parse(ByteStream source, Context ctx) {
			Optional<UnsignedBytes> marker = source.readUnsigned(NestBigInteger.ONE, ctx);
			if (!marker.isPresent() || !(marker.get().sameBytes(NestValue.of(0xFF, 1)))) {
			    ctx.fail("Header.marker {}", marker);
			    return Optional.empty();
			}
			Optional<UnsignedBytes> identifier = source.readUnsigned(NestBigInteger.ONE, ctx);
			if (!identifier.isPresent() || !(identifier.get().sameBytes(NestValue.of(0xD8, 1)))) {
				ctx.fail("Header.identifier {}", identifier);
				return Optional.empty();
			}
			return Optional.of(new Header(marker.get(), identifier.get()));
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public NestBigInteger size() {
			return marker.size().add(identifier.size());
		}
	}

	public static class SizedScan extends UserDefinedToken {
		private final Token entry;
		private SizedScan(Token entry) {
			this.entry = entry;
		}
		
		public static Optional<SizedScan> parse(ByteStream source, Context ctx) {
			Optional<Token> entry = Choice.between(source, ctx,
					Case.of(SizedSegment::parse, x -> {}),
					Case.of(ScanSegment::parse, x -> {})
			);
			if (!entry.isPresent()) {
				ctx.fail("SizedScan missing from {}", source);
				return Optional.empty();
			}
			return Optional.of(new SizedScan(entry.get()));

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

	public static class SizedSegment extends UserDefinedToken {
		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;
		public final UnsignedBytes length;
		public final UnsignedBytes payload;

		private SizedSegment(UnsignedBytes marker, UnsignedBytes identifier, UnsignedBytes length, UnsignedBytes payload) {
			this.marker = marker;
			this.identifier = identifier;
			this.length = length;
			this.payload = payload;
		}

		public static Optional<SizedSegment> parse(ByteStream source, Context ctx) {
			Optional<UnsignedBytes> marker = source.readUnsigned(1, ctx);
			if (!marker.isPresent() || !(marker.get().asValue().sameBytes(NestValue.of(0xFF, 1)))) {
				ctx.fail("SizedSegment.marker {}", marker);
				return Optional.empty();
			}
			Optional<UnsignedBytes> identifier = source.readUnsigned(1, ctx);
			if (!identifier.isPresent() || !(identifier.get().asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xD8)) < 0 || identifier.get().asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xDA)) > 0)) {
				ctx.fail("SizedSegment.identifier {}", identifier);
				return Optional.empty();
			}
			Optional<UnsignedBytes> length = source.readUnsigned(2, ctx);
			if (!length.isPresent()) {
				ctx.fail("SizedSegment.length missing from {}", source);
				return Optional.empty();
			}
			Optional<UnsignedBytes> payload = source.readUnsigned(length.get().asValue().asInteger(Sign.UNSIGNED).subtract(NestBigInteger.TWO), ctx);
			if (!payload.isPresent()) {
				ctx.fail("SizedSegment.payload missing from {}", source);
				return Optional.empty();
			}
			return Optional.of(new SizedSegment(marker.get(), identifier.get(), length.get(), payload.get()));
		}
		

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload);
		}

		@Override
		public NestBigInteger size() {
			return marker.size().add(identifier.size()).add(length.size()).add(payload.size());
		}
	}

	public static class ScanEscape extends UserDefinedToken {
		private final Token entry;
		private ScanEscape(Token entry) {
			this.entry = entry;
		}
		
		public static Optional<ScanEscape> parse(ByteStream source, Context ctx) {
			Optional<Token> entry = Choice.between(source, ctx,
					Case.of(ScanEscape$1::parse, x -> {}),
					Case.of(ScanEscape$2::parse, x -> {})
			);
			if (!entry.isPresent()) {
				ctx.fail("ScanEscape from {}", source);
				return Optional.empty();
			}
			return Optional.of(new ScanEscape(entry.get()));
		}

		private static class ScanEscape$1 extends UserDefinedToken {
			public final UnsignedBytes scanData;
			private ScanEscape$1(UnsignedBytes scanData) {
				this.scanData = scanData;
			}
			
			public static Optional<ScanEscape$1> parse(ByteStream source, Context ctx) {
				Optional<UnsignedBytes> scanData = source.readUnsigned(NestBigInteger.ONE, ctx);
				if (!scanData.isPresent() || !(!scanData.get().sameBytes(NestValue.of(0xFF, 1)))) {
					ctx.fail("ScanEscape$1.scanData", scanData);
					return Optional.empty();
				}
				return Optional.of(new ScanEscape$1(scanData.get()));
			}

			@Override
			public TrackedByteSlice getTrackedBytes() {
				return scanData.getTrackedBytes();
			}

			@Override
			public NestBigInteger size() {
				return scanData.size();
			}
		}
		private static class ScanEscape$2 extends UserDefinedToken {
			public final UnsignedBytes escape;
			private ScanEscape$2(UnsignedBytes escape) {
				this.escape = escape;
			}
			
			public static Optional<ScanEscape$2> parse(ByteStream source, Context ctx) {
				Optional<UnsignedBytes> escape = source.readUnsigned(NestBigInteger.TWO, ctx);
				if (!escape.isPresent() || !(escape.get().sameBytes(NestValue.of(0xFF00, 2)) ||
						(escape.get().asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xFFCF)) > 0 &&
								escape.get().asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xFFD8)) < 0))) {
					ctx.fail("ScanEscape$2.escape", escape);
					return Optional.empty();
				}
				return Optional.of(new ScanEscape$2(escape.get()));
			}

			@Override
			public TrackedByteSlice getTrackedBytes() {
				return escape.getTrackedBytes();
			}

			@Override
			public NestBigInteger size() {
				return escape.size();
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

	public static class ScanSegment extends UserDefinedToken {
		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;
		public final UnsignedBytes length;
		public final UnsignedBytes payload;
		public final TokenList<ScanEscape> choices;
		
		private ScanSegment(UnsignedBytes marker, UnsignedBytes identifier,
				UnsignedBytes length, UnsignedBytes payload,
				TokenList<ScanEscape> choices) {
			this.marker = marker;
			this.identifier = identifier;
			this.length = length;
			this.payload = payload;
			this.choices = choices;
		}
		
		public static Optional<ScanSegment> parse(ByteStream source, Context ctx) {
			Optional<UnsignedBytes> marker = source.readUnsigned(NestBigInteger.ONE, ctx);
			if (!marker.isPresent() || !(marker.get().sameBytes(NestValue.of(0xFF, 1)))) {
				ctx.fail("ScanSegment.marker {}", marker);
				return Optional.empty();
			}
			Optional<UnsignedBytes> identifier = source.readUnsigned(NestBigInteger.ONE, ctx);
			if (!identifier.isPresent() || !(identifier.get().sameBytes(NestValue.of(0xDA, 1)))) {
				ctx.fail("ScanSegment.identifier {}", identifier);
				return Optional.empty();
			}
			Optional<UnsignedBytes> length = source.readUnsigned(NestBigInteger.TWO, ctx);
			if (!length.isPresent()) {
				ctx.fail("ScanSegment.length missing from {}", source);
				return Optional.empty();
			}
			Optional<UnsignedBytes> payload = source.readUnsigned(length.get().asValue().asInteger(Sign.UNSIGNED).subtract(NestBigInteger.TWO), ctx);
			if (!payload.isPresent()) {
				ctx.fail("ScanSegment.payload missing from {}", source);
				return Optional.empty();
			}
			TokenList<ScanEscape> choices = TokenList.untilParseFailure(source, ctx, ScanEscape::parse);
			return Optional.of(new ScanSegment(marker.get(), identifier.get(), length.get(), payload.get(), choices));
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload, choices);
		}

		@Override
		public NestBigInteger size() {
			return marker.size().add(identifier.size()).add(length.size()).add(payload.size()).add(choices.size());
		}
	}

	public static class Footer extends UserDefinedToken {
		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;
		private Footer(UnsignedBytes marker, UnsignedBytes identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}
		
		public static Optional<Footer> parse(ByteStream source, Context ctx) {

			Optional<UnsignedBytes> marker = source.readUnsigned(NestBigInteger.ONE, ctx);

			if (!marker.isPresent() || !(marker.get().sameBytes(NestValue.of(0xFF, 1)))) {
				ctx.fail("Footer.marker {}", marker);
				return Optional.empty();
			}
			Optional<UnsignedBytes> identifier = source.readUnsigned(NestBigInteger.ONE, ctx);
			if (!identifier.isPresent() || !(identifier.get().sameBytes(NestValue.of(0xD9, 1)))) {
				ctx.fail("Footer.identifier {}", identifier);
				return Optional.empty();
			}
			return Optional.of(new Footer(marker.get(), identifier.get()));
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public NestBigInteger size() {
			return marker.size().add(identifier.size());
		}
		
	}

}
