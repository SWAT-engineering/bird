package engineering.swat.nest.examples.formats.jpeg;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import java.nio.ByteOrder;

public class JPEG  {
	
	public static class Format extends UserDefinedToken {
        public final Header $dummy1;
        public final TokenList<SizedScan> $dummy2;
        public final Footer $dummy3;
        private Format(Header $dummy1, TokenList<SizedScan> $dummy2, Footer $dummy3) {
			this.$dummy1 = $dummy1;
			this.$dummy2 = $dummy2;
			this.$dummy3 = $dummy3;
		}

		public static Format parse(ByteStream source, Context ctx) {
			ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
			Header $dummy1 = Header.parse(source, ctx);
			TokenList<SizedScan> $dummy2 = TokenList.untilParseFailure(source, ctx, (s, c) -> SizedScan.parse(s, c));
			Footer $dummy3 = Footer.parse(source, ctx);
			return new Format($dummy1, $dummy2, $dummy3);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView($dummy1, $dummy2, $dummy3);
		}

		@Override
		public long size() {
			return $dummy1.size() + $dummy2.size() + $dummy3.size();
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
			if (!(marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("Header.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.asInteger().getValue() == 0xD8)) {
				throw new ParseError("Header.identifier", identifier);
			}
			return new Header(marker, identifier);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size();
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
		public long size() {
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

		public static SizedSegment parse(ByteStream source, Context ctx) {
			UnsignedBytes marker = source.readUnsigned(1, ctx);
			if (!(marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("SizedSegment.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.asInteger().getValue() < 0xD8L || identifier.asInteger().getValue() > 0xDAL)) {
				throw new ParseError("SizedSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source.readUnsigned(length.asInteger().getValue() - 2, ctx);
			return new SizedSegment(marker, identifier, length, payload);
		}
		

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size() + length.size() + payload.size();
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
			public final UnsignedBytes scanData;
			private ScanEscape$1(UnsignedBytes scanData) {
				this.scanData = scanData;
			}
			
			public static ScanEscape$1 parse(ByteStream source, Context ctx) {
				UnsignedBytes scanData = source.readUnsigned(1, ctx);
				if (!(scanData.asInteger().getValue() != 0xFF)) {
					throw new ParseError("ScanEscape$1.scanData", scanData);
				}
				return new ScanEscape$1(scanData);
			}

			@Override
			public TrackedByteSlice getTrackedBytes() {
				return scanData.getTrackedBytes();
			}

			@Override
			public long size() {
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
				if (!(escape.asInteger().getValue() == 0xFF00 ||
						(escape.asInteger().getValue() > 0xFFCF && escape.asInteger().getValue() < 0xFFD8))) {
					throw new ParseError("ScanEscape$2.escape", escape);
				}
				return new ScanEscape$2(escape);
			}

			@Override
			public TrackedByteSlice getTrackedBytes() {
				return escape.getTrackedBytes();
			}

			@Override
			public long size() {
				return escape.size();
			}
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
			if (!(marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("ScanSegment.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.asInteger().getValue() == 0xDA)) {
				throw new ParseError("ScanSegment.identifier", identifier);
			}
			UnsignedBytes length = source.readUnsigned(2, ctx);
			UnsignedBytes payload = source.readUnsigned(length.asInteger().getValue() - 2, ctx);
			TokenList<ScanEscape> choices = TokenList.untilParseFailure(source, ctx, ScanEscape::parse);
			return new ScanSegment(marker, identifier, length, payload, choices);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload, choices);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size() + length.size() + payload.size() + choices.size();
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
			if (!(marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("Footer.marker", marker);
			}
			UnsignedBytes identifier = source.readUnsigned(1, ctx);
			if (!(identifier.asInteger().getValue() == 0xD9)) {
				throw new ParseError("Footer.identifier", identifier);
			}
			return new Footer(marker, identifier);
		}

		@Override
		public TrackedByteSlice getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size();
		}
		
	}

}
