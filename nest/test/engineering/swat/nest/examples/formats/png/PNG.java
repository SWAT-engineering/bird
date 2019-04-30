package engineering.swat.nest.examples.formats.png;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NonTokenBytes;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PNG {
    public final static class PNG$ extends UserDefinedToken {
        private final Signature $dummy1; // unnamed fields are not available
        public final TokenList<Chunk> chunks;
        private final IEND $dummy2;

        private PNG$(Signature $dummy1, TokenList<Chunk> chunks, IEND $dummy2) {
            this.$dummy1 = $dummy1;
            this.chunks = chunks;
            this.$dummy2 = $dummy2;
        }
        
        public static PNG$ parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
            Signature $dummy1 = Signature.parse(source, ctx);
            TokenList<Chunk> chunks = TokenList.untilParseFailure(source, ctx, (s, c) -> Chunk.parse(s, c));
            IEND $dummy2 = IEND.parse(source, ctx);
            return new PNG$($dummy1, chunks, $dummy2);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView($dummy1, chunks, $dummy2);
        }

        @Override
        public NestBigInteger size() {
            return $dummy1.size().add(chunks.size()).add($dummy2.size());
        }
    }

    public static final class Signature extends UserDefinedToken {
        private final UnsignedBytes $dummy1;
        private final UnsignedBytes $dummy2;
        private final UnsignedBytes $dummy3;
        
        private Signature(UnsignedBytes $dummy1, UnsignedBytes $dummy2, UnsignedBytes $dummy3) {
            this.$dummy1 = $dummy1;
            this.$dummy2 = $dummy2;
            this.$dummy3 = $dummy3;
        }
        
        public static Signature parse(ByteStream source, Context ctx) throws ParseError {
            UnsignedBytes $dummy1 = source.readUnsigned(1, ctx);
            if (!($dummy1.getByteAt(NestBigInteger.ZERO) == 0x89)) {
                throw new ParseError("Signature.$dummy1", $dummy1);
            }

            UnsignedBytes $dummy2 = source.readUnsigned(3, ctx);
            if (!($dummy2.sameBytes(ctx.getStringBytes("PNG")))) {
                throw new ParseError("Signature.$dummy2", $dummy2);
            }

            UnsignedBytes $dummy3 = source.readUnsigned(4, ctx);
            if (!($dummy3.sameBytes(NonTokenBytes.of(0x0d, 0x0a, 0x1a, 0x0a)))) {
                throw new ParseError("Signature.$dummy3", $dummy3);
            }
            return new Signature($dummy1, $dummy2, $dummy3);
        }
        
        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView($dummy1, $dummy2, $dummy3);
        }
        
        @Override
        public NestBigInteger size() {
            return $dummy1.size().add($dummy2.size()).add($dummy3.size());
        }
        

    }

    public final static class Chunk extends UserDefinedToken {
        public final UnsignedBytes length;
        public final UnsignedBytes type;
        public final UnsignedBytes data;
        public final UnsignedBytes crc;
        
        private Chunk(UnsignedBytes length, UnsignedBytes type, UnsignedBytes data,
                UnsignedBytes crc) {
            this.length = length;
            this.type = type;
            this.data = data;
            this.crc = crc;
        }

        public static Chunk parse(ByteStream source, Context ctx)  {
            UnsignedBytes length = source.readUnsigned(4, ctx);
            UnsignedBytes type = source.readUnsigned(4, ctx);
            if (!(!type.sameBytes(ctx.getStringBytes("IEND")))) {
                throw new ParseError("Chunk.type");
            }
            UnsignedBytes data = source.readUnsigned(length.asInteger().getBigInteger(), ctx);
            UnsignedBytes crc = source.readUnsigned(4, ctx);
            if (!(UserDefinedPNG.crc32(TokenList.of(type, data)) == crc.asInteger().longValueExact())) {
                throw new ParseError("Chunk.crc");
            }
            return new Chunk(length, type, data, crc);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(length, type, data, crc);
        }
        
        @Override
        public NestBigInteger size() {
            return length.size().add(type.size()).add(data.size()).add(crc.size());
        }

    }

    public static final class IEND extends UserDefinedToken {
        public final UnsignedBytes length;
        public final UnsignedBytes type;
        public final UnsignedBytes crc;
        
        private IEND(UnsignedBytes length, UnsignedBytes type, UnsignedBytes crc) {
            this.length = length;
            this.type = type;
            this.crc = crc;
        }

        public static IEND parse(ByteStream source, Context ctx) {
            UnsignedBytes length = source.readUnsigned(4, ctx);
            if (!(length.asInteger().getBigInteger().equals(NestBigInteger.ZERO))) {
                throw new ParseError("IED.length", length);
            }
            
            UnsignedBytes type = source.readUnsigned(4, ctx);
            if (!type.sameBytes(ctx.getStringBytes("IEND"))) {
                throw new ParseError("IED.type", type);
            }
            
            UnsignedBytes crc = source.readUnsigned(4, ctx);
            if (!crc.sameBytes(NonTokenBytes.of(0xae, 0x42, 0x60, 0x82))) {
                throw new ParseError("IED.crc", crc);
            }
            return new IEND(length, type, crc);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(length, type, crc);
        }

        @Override
        public NestBigInteger size() {
            return length.size().add(type.size()).add(crc.size());
        }

    }

}

