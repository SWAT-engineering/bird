package engineering.swat.nest.examples.formats.jpeg;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
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
		protected Token[] parsedTokens() {
			return new Token[]{$anon1, $anon2, $anon3};
		}
	}

	public static class Header extends UserDefinedToken {

		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;

		private Header(UnsignedBytes marker, UnsignedBytes identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}

		public static Header parse(ByteStream source, Context ctx) {
			UnsignedBytes marker = source.readUnsigned(1, ctx);
			if (!(marker.sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("Header.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.sameBytes(NestValue.of(0xD8, 1)))) {
				throw new ParseError("Header.identifier", identifier);
			}
			return new Header(marker, identifier);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{marker, identifier};
		}
	}

	public static class SizedScan extends UserDefinedToken {

		private final Token entry;

		private SizedScan(Token entry) {
			this.entry = entry;
		}

		public static SizedScan parse(ByteStream source, Context ctx) {
			Token entry = Choice.between(source, ctx,
					SizedSegment::parse,
					ScanSegment::parse
			);
			return new SizedScan(entry);

		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{entry};
		}

	}

	public static class SizedSegment extends UserDefinedToken {

		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;
		public final UnsignedBytes length;
		public final UnsignedBytes payload;

		private SizedSegment(UnsignedBytes marker, UnsignedBytes identifier, UnsignedBytes length,
				UnsignedBytes payload) {
			this.marker = marker;
			this.identifier = identifier;
			this.length = length;
			this.payload = payload;
		}

		public static SizedSegment parse(ByteStream source, Context ctx) {
			UnsignedBytes marker = source.readUnsigned(1, ctx);
			if (!(marker.sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("SizedSegment.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.asValue().asInteger().compareTo(NestBigInteger.of(0xD8)) < 0
					|| identifier.asValue().asInteger().compareTo(NestBigInteger.of(0xDA)) > 0)) {
				throw new ParseError("SizedSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source
					.readUnsigned(length.asValue().asInteger().subtract(NestBigInteger.TWO), ctx);
			return new SizedSegment(marker, identifier, length, payload);
		}


		@Override
		protected Token[] parsedTokens() {
			return new Token[]{marker, identifier, length, payload};
		}
	}

	public static class ScanEscape extends UserDefinedToken {

		private final Token entry;

		private ScanEscape(Token entry) {
			this.entry = entry;
		}

		public static ScanEscape parse(ByteStream source, Context ctx) {
			Token entry = Choice.between(source, ctx,
					ScanEscape$1::parse,
					ScanEscape$2::parse
			);
			return new ScanEscape(entry);
		}

		private static class ScanEscape$1 extends UserDefinedToken {

			public final UnsignedBytes scanData;

			private ScanEscape$1(UnsignedBytes scanData) {
				this.scanData = scanData;
			}

			public static ScanEscape$1 parse(ByteStream source, Context ctx) {
				UnsignedBytes scanData = source.readUnsigned(1, ctx);
				if (!(!scanData.sameBytes(NestValue.of(0xFF, 1)))) {
					throw new ParseError("ScanEscape$1.scanData", scanData);
				}
				return new ScanEscape$1(scanData);
			}

			@Override
			protected Token[] parsedTokens() {
				return new Token[]{scanData};
			}
		}

		private static class ScanEscape$2 extends UserDefinedToken {

			public final UnsignedBytes escape;

			private ScanEscape$2(UnsignedBytes escape) {
				this.escape = escape;
			}

			public static ScanEscape$2 parse(ByteStream source, Context ctx) {
				UnsignedBytes escape = source.readUnsigned(2, ctx);
				if (!(escape.sameBytes(NestValue.of(0xFF00, 2)) ||
						(escape.asValue().asInteger().compareTo(NestBigInteger.of(0xFFCF)) > 0 &&
								escape.asValue().asInteger().compareTo(NestBigInteger.of(0xFFD8)) < 0))) {
					throw new ParseError("ScanEscape$2.escape", escape);
				}
				return new ScanEscape$2(escape);
			}

			@Override
			protected Token[] parsedTokens() {
				return new Token[]{escape};
			}
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{entry};
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

		public static ScanSegment parse(ByteStream source, Context ctx) {
			UnsignedBytes marker = source.readUnsigned(1, ctx);

			if (!(marker.sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("ScanSegment.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.sameBytes(NestValue.of(0xDA, 1)))) {
				throw new ParseError("ScanSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source
					.readUnsigned(length.asValue().asInteger().subtract(NestBigInteger.TWO), ctx);
			TokenList<ScanEscape> choices = TokenList.untilParseFailure(source, ctx, ScanEscape::parse);
			return new ScanSegment(marker, identifier, length, payload, choices);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{marker, identifier, length, payload, choices};
		}
	}

	public static class Footer extends UserDefinedToken {

		public final UnsignedBytes marker;
		public final UnsignedBytes identifier;

		private Footer(UnsignedBytes marker, UnsignedBytes identifier) {
			this.marker = marker;
			this.identifier = identifier;
		}

		public static Footer parse(ByteStream source, Context ctx) {

			UnsignedBytes marker = source.readUnsigned(1, ctx);

			if (!(marker.sameBytes(NestValue.of(0xFF, 1)))) {
				throw new ParseError("Footer.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.sameBytes(NestValue.of(0xD9, 1)))) {
				throw new ParseError("Footer.identifier", identifier);
			}
			return new Footer(marker, identifier);
		}

		@Override
		protected Token[] parsedTokens() {
			return new Token[]{marker, identifier};
		}

	}

}
