package engineering.swat.nest.examples.formats.jpeg;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import java.nio.ByteOrder;

public class JPEG  {
	
	public static class Format extends UserDefinedToken {
        public Header $dummy1;
        public TokenList<SizedScan> $dummy2;
        public Footer $dummy3;
        private Format() {}

		public static Format parse(ByteStream source, Context ctx) {
			ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
			Format result = new Format();
			result.$dummy1 = Header.parse(source, ctx);
			result.$dummy2 = TokenList.untilParseFailure(source, ctx, (s, c) -> SizedScan.parse(s, c));
			result.$dummy3 = Footer.parse(source, ctx);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView($dummy1, $dummy2, $dummy3);
		}

		@Override
		public long size() {
			return $dummy1.size() + $dummy2.size() + $dummy3.size();
		}
	}
	
	public static class Header extends UserDefinedToken {
		public UnsignedBytes marker;
		public UnsignedBytes identifier;
		private Header() {}
		
		public static Header parse(ByteStream source, Context ctx) {
			Header result = new Header();
			result.marker = source.readUnsigned(1, ctx);
			if (!(result.marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("Header.marker", result.marker);
			}
			result.identifier = source.readUnsigned(1, ctx);
			if (!(result.identifier.asInteger().getValue() == 0xD8)) {
				throw new ParseError("Header.identifier", result.identifier);
			}
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size();
		}
	}

	public static class SizedScan extends UserDefinedToken {
		private Token entry;
		private SizedScan() {}
		
		public static SizedScan parse(ByteStream source, Context ctx) {
			SizedScan result = new SizedScan();
			result.entry = Choice.between(source, ctx,
					Case.of(SizedSegment::parse, x -> {}),
					Case.of(ScanSegment::parse, x -> {})
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
	
	public static class SizedSegment extends UserDefinedToken {
		public UnsignedBytes marker;
		public UnsignedBytes identifier;
		public UnsignedBytes length;
		public UnsignedBytes payload;
		private SizedSegment() {}

		public static SizedSegment parse(ByteStream source, Context ctx) {
			SizedSegment result = new SizedSegment();
			result.marker = source.readUnsigned(1, ctx);
			if (!(result.marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("SizedSegment.marker", result.marker);
			}
			result.identifier = source.readUnsigned(1, ctx);
			if (!(result.identifier.asInteger().getValue() < 0xD8L || result.identifier.asInteger().getValue() > 0xDAL)) {
				throw new ParseError("SizedSegment.identifier", result.identifier);
			}
			result.length = source.readUnsigned(2, ctx);
			result.payload = source.readUnsigned(result.length.asInteger().getValue() - 2, ctx);
			return result;
		}
		

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size() + length.size() + payload.size();
		}
	}

	public static class ScanEscape extends UserDefinedToken {
		private Token entry;
		private ScanEscape() {}
		
		public static ScanEscape parse(ByteStream source, Context ctx) {
			ScanEscape result = new ScanEscape();
			result.entry = Choice.between(source, ctx,
					Case.of(ScanEscape$1::parse, x -> {}),
					Case.of(ScanEscape$2::parse, x -> {})
			);
			return result;
		}
		private static class ScanEscape$1 extends UserDefinedToken {
			public UnsignedBytes scanData;
			private ScanEscape$1() {}
			
			public static ScanEscape$1 parse(ByteStream source, Context ctx) {
				ScanEscape$1 result = new ScanEscape$1();
				result.scanData = source.readUnsigned(1, ctx);
				if (!(result.scanData.asInteger().getValue() != 0xFF)) {
					throw new ParseError("ScanEscape$1.scanData", result.scanData);
				}
				return result;
			}

			@Override
			public TrackedBytesView getTrackedBytes() {
				return scanData.getTrackedBytes();
			}

			@Override
			public long size() {
				return scanData.size();
			}
		}
		private static class ScanEscape$2 extends UserDefinedToken {
			public UnsignedBytes escape;
			private ScanEscape$2() {}
			
			public static ScanEscape$2 parse(ByteStream source, Context ctx) {
				ScanEscape$2 result = new ScanEscape$2();
				result.escape = source.readUnsigned(2, ctx);
				if (!(result.escape.asInteger().getValue() == 0xFF00 || 
						(result.escape.asInteger().getValue() > 0xFFCF && result.escape.asInteger().getValue() < 0xFFD8))) {
					throw new ParseError("ScanEscape$2.escape", result.escape);
				}
				return result;
			}

			@Override
			public TrackedBytesView getTrackedBytes() {
				return escape.getTrackedBytes();
			}

			@Override
			public long size() {
				return escape.size();
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

	public static class ScanSegment extends UserDefinedToken {
		public UnsignedBytes marker;
		public UnsignedBytes identifier;
		public UnsignedBytes length;
		public UnsignedBytes payload;
		public TokenList<ScanEscape> choices;
		
		private ScanSegment() {}
		
		public static ScanSegment parse(ByteStream source, Context ctx) {
			ScanSegment result = new ScanSegment();
			result.marker = source.readUnsigned(1, ctx);
			if (!(result.marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("ScanSegment.marker", result.marker);
			}
			result.identifier = source.readUnsigned(1, ctx);
			if (!(result.identifier.asInteger().getValue() == 0xDA)) {
				throw new ParseError("ScanSegment.identifier", result.identifier);
			}
			result.length = source.readUnsigned(2, ctx);
			result.payload = source.readUnsigned(result.length.asInteger().getValue() - 2, ctx);
			result.choices = TokenList.untilParseFailure(source, ctx, ScanEscape::parse);
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(marker, identifier, length, payload, choices);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size() + length.size() + payload.size() + choices.size();
		}
	}

	public static class Footer extends UserDefinedToken {
		public UnsignedBytes marker;
		public UnsignedBytes identifier;
		private Footer() {}
		
		public static Footer parse(ByteStream source, Context ctx) {
			Footer result = new Footer();
			result.marker = source.readUnsigned(1, ctx);
			if (!(result.marker.asInteger().getValue() == 0xFF)) {
				throw new ParseError("Footer.marker", result.marker);
			}
			result.identifier = source.readUnsigned(1, ctx);
			if (!(result.identifier.asInteger().getValue() == 0xD9)) {
				throw new ParseError("Footer.identifier", result.identifier);
			}
			return result;
		}

		@Override
		public TrackedBytesView getTrackedBytes() {
			return buildTrackedView(marker, identifier);
		}

		@Override
		public long size() {
			return marker.size() + identifier.size();
		}
		
	}

}
