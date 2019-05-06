package engineering.swat.nest.examples.formats.png;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
        
        public static Optional<PNG$> parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
            Optional<Signature> $anon1 = Signature.parse(source, ctx);
            if (!$anon1.isPresent()) {
                ctx.fail("PNG._ missing from {}", source);
                return Optional.empty();
            }
            TokenList<Chunk> chunks = TokenList.untilParseFailure(source, ctx, (s, c) -> Chunk.parse(s, c));
            Optional<IEND> $anon2 = IEND.parse(source, ctx);
            if (!$anon2.isPresent()) {
                ctx.fail("PNG._ missing from {}", source);
                return Optional.empty();
            }
            return Optional.of(new PNG$($anon1.get(), chunks, $anon2.get()));
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
        private final UnsignedBytes $anon1;
        private final UnsignedBytes $anon2;
        private final UnsignedBytes $anon3;
        
        private Signature(UnsignedBytes $anon1, UnsignedBytes $anon2, UnsignedBytes $anon3) {
            this.$anon1 = $anon1;
            this.$anon2 = $anon2;
            this.$anon3 = $anon3;
        }
        
        public static Optional<Signature> parse(ByteStream source, Context ctx) {
            Optional<UnsignedBytes> $anon1 = source.readUnsigned(1, ctx);
            if (!$anon1.isPresent() || !($anon1.get().asValue().sameBytes(NestValue.of(0x89, 1)))) {
                ctx.fail("Signature.$anon1 {}", $anon1);
                return Optional.empty();
            }

            Optional<UnsignedBytes> $anon2 = source.readUnsigned(3, ctx);
            if (!$anon2.isPresent() || !($anon2.get().asString().get().equals("PNG"))) {
                ctx.fail("Signature.$anon2 {}", $anon2);
                return Optional.empty();
            }

            Optional<UnsignedBytes> $anon3 = source.readUnsigned(4, ctx);
            if (!$anon3.isPresent() || !($anon3.get().asValue().sameBytes(NestValue.of(new byte[] {0x0d, 0x0a, 0x1a, 0x0a})))) {
                ctx.fail("Signature.$anon3 {}", $anon3);
                return Optional.empty();
            }
            return Optional.of(new Signature($anon1.get(), $anon2.get(), $anon3.get()));
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

        public static Optional<Chunk> parse(ByteStream source, Context ctx)  {
            Optional<UnsignedBytes> length = source.readUnsigned(4, ctx);
            if (!length.isPresent()) {
                ctx.fail("Chunk.length missing from {}", source);
                return Optional.empty();
            }
            Optional<UnsignedBytes> type = source.readUnsigned(4, ctx);
            if (!type.isPresent() || type.get().asString().get().equals("IEND")) {
                ctx.fail("Chunk.type {}", type);
                return Optional.empty();
            }
            Optional<UnsignedBytes> data = source.readUnsigned(length.get().asValue().asInteger(Sign.UNSIGNED), ctx);
            if (!data.isPresent()) {
                ctx.fail("Chunk.data missing from {}", source);
                return Optional.empty();
            }
            Optional<UnsignedBytes> crc = source.readUnsigned(4, ctx);
            if (!crc.isPresent() || !(crc.get().asValue().asInteger(Sign.UNSIGNED).equals(UserDefinedPNG.crc32(TokenList.of(ctx, type.get(), data.get()))))) {
                ctx.fail("Chunk.crc {}", crc);
                return Optional.empty();
            }
            return Optional.of(new Chunk(length.get(), type.get(), data.get(), crc.get()));
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

        public static Optional<IEND> parse(ByteStream source, Context ctx) {
            Optional<UnsignedBytes> length = source.readUnsigned(4, ctx);
            if (!length.isPresent() || !(length.get().asValue().asInteger(Sign.UNSIGNED).equals(NestBigInteger.ZERO))) {
                ctx.fail("IED.length {}", length);
                return Optional.empty();
            }
            
            Optional<UnsignedBytes> type = source.readUnsigned(4, ctx);
            if (!type.isPresent() || !type.get().asString().get().equals("IEND")) {
                ctx.fail("IED.type {}", type);
                return Optional.empty();
            }
            
            Optional<UnsignedBytes> crc = source.readUnsigned(4, ctx);
            if (!crc.isPresent() || !crc.get().asValue().sameBytes(NestValue.of(new byte[] {(byte) 0xae, 0x42, 0x60, (byte) 0x82}))) {
                ctx.fail("IED.crc {}", crc);
                return Optional.empty();
            }
            return Optional.of(new IEND(length.get(), type.get(), crc.get()));
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

