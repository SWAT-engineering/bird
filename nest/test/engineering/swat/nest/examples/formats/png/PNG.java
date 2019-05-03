package engineering.swat.nest.examples.formats.png;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedByte;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PNG {
    public final static class PNG$ extends UserDefinedToken {
        private final Signature $anon1; // unnamed fields are not available
        public final TokenList<Chunk> chunks;
        private final IEND $anon2;

        private PNG$(Signature $anon1, TokenList<Chunk> chunks, IEND $anon2) {
            this.$anon1 = $anon1;
            this.chunks = chunks;
            this.$anon2 = $anon2;
        }
        
        public static PNG$ parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
            Signature $anon1 = Signature.parse(source, ctx);
            TokenList<Chunk> chunks = TokenList.untilParseFailure(source, ctx, (s, c) -> Chunk.parse(s, c));
            IEND $anon2 = IEND.parse(source, ctx);
            return new PNG$($anon1, chunks, $anon2);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView($anon1, chunks, $anon2);
        }

        @Override
        public NestBigInteger size() {
            return $anon1.size().add(chunks.size()).add($anon2.size());
        }
    }

    public static final class Signature extends UserDefinedToken {
        private final UnsignedByte $anon1;
        private final UnsignedBytes $anon2;
        private final UnsignedBytes $anon3;
        
        private Signature(UnsignedByte $anon1, UnsignedBytes $anon2, UnsignedBytes $anon3) {
            this.$anon1 = $anon1;
            this.$anon2 = $anon2;
            this.$anon3 = $anon3;
        }
        
        public static Signature parse(ByteStream source, Context ctx) throws ParseError {
            UnsignedByte $anon1 = source.readUnsigned( ctx);
            if (!($anon1.asValue().sameBytes(NestValue.of(0x89, 1)))) {
                throw new ParseError("Signature.$anon1", $anon1);
            }

            UnsignedBytes $anon2 = source.readUnsigned(3, ctx);
            if (!($anon2.asString().equals("PNG"))) {
                throw new ParseError("Signature.$anon2", $anon2);
            }

            UnsignedBytes $anon3 = source.readUnsigned(4, ctx);
            if (!($anon3.asValue().sameBytes(NestValue.of(new byte[] {0x0d, 0x0a, 0x1a, 0x0a})))) {
                throw new ParseError("Signature.$anon3", $anon3);
            }
            return new Signature($anon1, $anon2, $anon3);
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
            if (type.asString().equals("IEND")) {
                throw new ParseError("Chunk.type");
            }
            UnsignedBytes data = source.readUnsigned(length.asValue().asInteger(Sign.UNSIGNED), ctx);
            UnsignedBytes crc = source.readUnsigned(4, ctx);
            if (!(crc.asValue().asInteger(Sign.UNSIGNED).equals(UserDefinedPNG.crc32(TokenList.of(ctx, type, data))))) {
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
            if (!(length.asValue().asInteger(Sign.UNSIGNED).equals(NestBigInteger.ZERO))) {
                throw new ParseError("IED.length", length);
            }
            
            UnsignedBytes type = source.readUnsigned(4, ctx);
            if (!type.asString().equals("IEND")) {
                throw new ParseError("IED.type", type);
            }
            
            UnsignedBytes crc = source.readUnsigned(4, ctx);
            if (!crc.asValue().sameBytes(NestValue.of(new byte[] {(byte) 0xae, 0x42, 0x60, (byte) 0x82}))) {
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

