package engineering.swat.nest.examples.formats.jpeg;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedByte;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import java.nio.ByteOrder;

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

		public static Format parse(ByteStream source, Context ctx) {
			ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
			Header $anon1 = Header.parse(source, ctx);
			TokenList<SizedScan> $anon2 = TokenList.untilParseFailure(source, ctx, (s, c) -> SizedScan.parse(s, c));
			Footer $anon3 = Footer.parse(source, ctx);
			return new Format($anon1, $anon2, $anon3);
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
		public final UnsignedByte marker;
		public final UnsignedByte identifier;
		private Header(UnsignedByte marker, UnsignedByte identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}
		
		public static Header parse(ByteStream source, Context ctx) {
			UnsignedByte marker = source.readUnsigned( ctx);
			if (!(marker.asValue().sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("Header.marker", marker);
			}
			UnsignedByte identifier = source.readUnsigned( ctx);
			if (!(identifier.asValue().sameBytes(NestValue.of(0xD8, 1)))) {
				throw new ParseError("Header.identifier", identifier);
			}
			return new Header(marker, identifier);
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
		
		public static SizedScan parse(ByteStream source, Context ctx) {
			Token entry = Choice.between(source, ctx,
					Case.of(SizedSegment::parse, x -> {}),
					Case.of(ScanSegment::parse, x -> {})
			);
			return new SizedScan(entry);

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
		public final UnsignedByte marker;
		public final UnsignedByte identifier;
		public final UnsignedBytes length;
		public final UnsignedBytes payload;

		private SizedSegment(UnsignedByte marker, UnsignedByte identifier, UnsignedBytes length, UnsignedBytes payload) {
			this.marker = marker;
			this.identifier = identifier;
			this.length = length;
			this.payload = payload;
		}

		public static SizedSegment parse(ByteStream source, Context ctx) {
			UnsignedByte marker = source.readUnsigned(ctx);
			if (!(marker.asValue().sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("SizedSegment.marker", marker);
			}
			UnsignedByte identifier = source.readUnsigned( ctx);
			if (!(identifier.asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xD8)) < 0 || identifier.asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xDA)) > 0)) {
				throw new ParseError("SizedSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source.readUnsigned(length.asValue().asInteger(Sign.UNSIGNED).subtract(NestBigInteger.TWO), ctx);
			return new SizedSegment(marker, identifier, length, payload);
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
		
		public static ScanEscape parse(ByteStream source, Context ctx) {
			Token entry = Choice.between(source, ctx,
					Case.of(ScanEscape$1::parse, x -> {}),
					Case.of(ScanEscape$2::parse, x -> {})
			);
			return new ScanEscape(entry);
		}
		private static class ScanEscape$1 extends UserDefinedToken {
			public final UnsignedByte scanData;
			private ScanEscape$1(UnsignedByte scanData) {
				this.scanData = scanData;
			}
			
			public static ScanEscape$1 parse(ByteStream source, Context ctx) {
				UnsignedByte scanData = source.readUnsigned( ctx);
				if (!(!scanData.asValue().sameBytes(NestValue.of(0xFF, 1)))) {
					throw new ParseError("ScanEscape$1.scanData", scanData);
				}
				return new ScanEscape$1(scanData);
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
			
			public static ScanEscape$2 parse(ByteStream source, Context ctx) {
				UnsignedBytes escape = source.readUnsigned(2, ctx);
				if (!(escape.asValue().sameBytes(NestValue.of(0xFF00, 2)) ||
						(escape.asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xFFCF)) > 0 &&
								escape.asValue().asInteger(Sign.UNSIGNED).compareTo(NestBigInteger.of(0xFFD8)) < 0))) {
					throw new ParseError("ScanEscape$2.escape", escape);
				}
				return new ScanEscape$2(escape);
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
		public final UnsignedByte marker;
		public final UnsignedByte identifier;
		public final UnsignedBytes length;
		public final UnsignedBytes payload;
		public final TokenList<ScanEscape> choices;
		
		private ScanSegment(UnsignedByte marker, UnsignedByte identifier,
				UnsignedBytes length, UnsignedBytes payload,
				TokenList<ScanEscape> choices) {
			this.marker = marker;
			this.identifier = identifier;
			this.length = length;
			this.payload = payload;
			this.choices = choices;
		}
		
		public static ScanSegment parse(ByteStream source, Context ctx) {
			UnsignedByte marker = source.readUnsigned(ctx);

			if (!(marker.asValue().sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("ScanSegment.marker", marker);
			}
			UnsignedByte identifier = source.readUnsigned(ctx);
			if (!(identifier.asValue().sameBytes(NestValue.of(0xDA, 1)))) {
				throw new ParseError("ScanSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source.readUnsigned(length.asValue().asInteger(Sign.SIGNED).subtract(NestBigInteger.TWO), ctx);
			TokenList<ScanEscape> choices = TokenList.untilParseFailure(source, ctx, ScanEscape::parse);
			return new ScanSegment(marker, identifier, length, payload, choices);
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
		public final UnsignedByte marker;
		public final UnsignedByte identifier;
		private Footer(UnsignedByte marker, UnsignedByte identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}
		
		public static Footer parse(ByteStream source, Context ctx) {

			UnsignedByte marker = source.readUnsigned(ctx);

			if (!(marker.asValue().sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("Footer.marker", marker);
			}
			UnsignedByte identifier = source.readUnsigned(ctx);
			if (!(identifier.asValue().sameBytes(NestValue.of(0xD9, 1)))) {
				throw new ParseError("Footer.identifier", identifier);
			}
			return new Footer(marker, identifier);
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
