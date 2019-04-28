package engineering.swat.nest.examples.formats.png;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.nontokens.NestString;
import engineering.swat.nest.core.nontokens.NonTokenBytes;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class PNG {
    public final static class PNG$ extends UserDefinedToken {
        public Signature $dummy1;
        public TokenList<Chunk> chunks;
        public IEND $dummy2;

        private PNG$() {}
        
        public static PNG$ parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
            PNG$ result = new PNG$();
            result.$dummy1 = Signature.parse(source, ctx);
            result.chunks = TokenList.untilParseFailure(source, ctx, (s, c) -> Chunk.parse(s, c));
            result.$dummy2 = IEND.parse(source, ctx);
            return result;
        }

        @Override
        public TrackedBytesView getTrackedBytes() {
            return buildTrackedView($dummy1, chunks, $dummy2);
        }

        @Override
        public long size() {
            return $dummy1.size() + chunks.size() + $dummy2.size();
        }
    }

    public static final class Signature extends UserDefinedToken {
        public UnsignedBytes $dummy1;
        public UnsignedBytes $dummy2;
        public UnsignedBytes $dummy3;
        
        private Signature() {}
        
        public static Signature parse(ByteStream source, Context ctx) throws ParseError {
            Signature result = new Signature();

            result.$dummy1 = source.readUnsigned(1, ctx);
            if (!(result.$dummy1.asInteger().getValue() == 0x89)) {
                throw new ParseError("Signature.$dummy1", result.$dummy1);
            }

            result.$dummy2 = source.readUnsigned(3, ctx);
            if (!(result.$dummy2.asString().equals(new NestString("PNG")))) {
                throw new ParseError("Signature.$dummy2", result.$dummy2);
            }

            result.$dummy3 = source.readUnsigned(4, ctx);
            if (!(result.$dummy3.sameBytes(NonTokenBytes.of(0x0d, 0x0a, 0x1a, 0x0a)))) {
                throw new ParseError("Signature.$dummy3", result.$dummy3);
            }
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

    public final static class Chunk extends UserDefinedToken {
        public UnsignedBytes length;
        public UnsignedBytes type;
        public UnsignedBytes data;
        public UnsignedBytes crc;
        
        private Chunk() {}

        public static Chunk parse(ByteStream source, Context ctx)  {
            Chunk result = new Chunk();
            result.length = source.readUnsigned(4, ctx);
            result.type = source.readUnsigned(4, ctx);
            if (!(!result.type.asString().equals("IEND"))) {
                throw new ParseError("Chunk.type");
            }
            result.data = source.readUnsigned(Math.toIntExact(result.length.asInteger().getValue()), ctx);
            result.crc = source.readUnsigned(4, ctx);
            if (!(UserDefinedPNG.crc32(TokenList.of(result.type, result.data)) == result.crc.asInteger().getValue())) {
                throw new ParseError("Chunk.crc");
            }
            return result;
        }

        @Override
        public TrackedBytesView getTrackedBytes() {
            return buildTrackedView(length, type, data, crc);
        }
        
        @Override
        public long size() {
            return length.size() + type.size() + data.size() + crc.size();
        }

    }

    public static final class IEND extends UserDefinedToken {
        public UnsignedBytes length;
        public UnsignedBytes type;
        public UnsignedBytes crc;
        
        private IEND() {}

        public static IEND parse(ByteStream source, Context ctx) {
            IEND result = new IEND();
            result.length = source.readUnsigned(4, ctx);
            if (!(result.length.asInteger().getValue() == 0)) {
                throw new ParseError("IED.length", result.length);
            }
            
            result.type = source.readUnsigned(4, ctx);
            if (!result.type.asString().equals(new NestString("IEND"))) {
                throw new ParseError("IED.type", result.type);
            }
            
            result.crc = source.readUnsigned(4, ctx);
            if (!result.crc.sameBytes(NonTokenBytes.of(0xae, 0x42, 0x60, 0x82))) {
                throw new ParseError("IED.crc", result.crc);
            }
            return result;
        }

        @Override
        public TrackedBytesView getTrackedBytes() {
            return buildTrackedView(length, type, crc);
        }

        @Override
        public long size() {
            return length.size() + type.size() + crc.size();
        }

    }

}

