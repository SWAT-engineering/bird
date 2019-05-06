package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TieTest {

    @Test
    void tieWorks() {
        Tie1 parsed = Tie1.parse(wrap(1, 2, 3, 4), Context.DEFAULT).get();
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data1.asValue().asInteger(Sign.UNSIGNED).intValueExact());
        assertEquals(0x03 << 8 | 0x04, parsed.other.data2.asValue().asInteger(Sign.UNSIGNED).intValueExact());
    }

    @Test
    void tieFlipWorks() {
        Tie2 parsed = Tie2.parse(wrap(1, 2, 3, 4), Context.DEFAULT).get();
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.data1.asValue().asInteger(Sign.UNSIGNED).intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data2.asValue().asInteger(Sign.UNSIGNED).intValueExact());
    }

    private static class Tie1 extends UserDefinedToken {
        public final UnsignedBytes data;
        public final OtherStruct other;

        private Tie1(UnsignedBytes data, OtherStruct other) {
            this.data = data;
            this.other = other;
        }
        public static Optional<Tie1> parse(ByteStream source, Context ctx) {
            Optional<UnsignedBytes> data = source.readUnsigned(4, ctx);
            if (!data.isPresent()) {
                ctx.fail("Tie1.data missing from {}", source);
                return Optional.empty();
            }
            // a tie is just a new bytestream that can start at any token
            ByteStream tieSource = new ByteStream(data.get());
            Optional<OtherStruct> other = OtherStruct.parse(tieSource, ctx);
            if (!other.isPresent()) {
                ctx.fail("Tie1.other missing from {}", tieSource);
                return Optional.empty();
            }
            return Optional.of(new Tie1(data.get(), other.get()));
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            // do not include the tie field to the tracked bytes view !
            return data.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return data.size();
        }
    }

    private static class Tie2 extends UserDefinedToken {
        public final UnsignedBytes data1;
        public final UnsignedBytes data2;
        public final OtherStruct other;

        private Tie2(UnsignedBytes data1, UnsignedBytes data2, OtherStruct other) {
            this.data1 = data1;
            this.data2 = data2;
            this.other = other;
        }


        public static Optional<Tie2> parse(ByteStream source, Context ctx) {
            Optional<UnsignedBytes> data1 = source.readUnsigned(2, ctx);
            if (!data1.isPresent()) {
                ctx.fail("Tie2.data1 missing from {}", source);
                return Optional.empty();
            }
            Optional<UnsignedBytes> data2 = source.readUnsigned(2, ctx);
            if (!data2.isPresent()) {
                ctx.fail("Tie2.data2 missing from {}", source);
                return Optional.empty();
            }
            // a tie is just a new bytestream that can start at any token
            ByteStream tieSource = new ByteStream(TokenList.of(ctx, data2.get(), data1.get()));
            Optional<OtherStruct> other = OtherStruct.parse(tieSource, ctx);
            if (!other.isPresent()) {
                ctx.fail("Tie2.other missing from {}", tieSource);
                return Optional.empty();
            }
            return Optional.of(new Tie2(data1.get(), data2.get(), other.get()));
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            // do not include the tie field to the tracked bytes view !
            return buildTrackedView(data1, data2);
        }

        @Override
        public NestBigInteger size() {
            return data1.size().add(data2.size());
        }
    }

    private static class OtherStruct extends UserDefinedToken {
        public final UnsignedBytes data1;
        public final UnsignedBytes data2;

        public OtherStruct(UnsignedBytes data1, UnsignedBytes data2) {
            this.data1 = data1;
            this.data2 = data2;
        }

        public static Optional<OtherStruct> parse(ByteStream source, Context ctx) {
            Optional<UnsignedBytes> data1 = source.readUnsigned(2, ctx);
            if (!data1.isPresent()) {
                ctx.fail("OtherStruct.data1 missing from {}", source);
                return Optional.empty();
            }
            Optional<UnsignedBytes> data2 = source.readUnsigned(2, ctx);
            if (!data2.isPresent()) {
                ctx.fail("OtherStruct.data2 missing from {}", source);
                return Optional.empty();
            }
            return Optional.of(new OtherStruct(data1.get(), data2.get()));

        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(data1, data2);
        }

        @Override
        public NestBigInteger size() {
            return data1.size().add(data2.size());
        }
    }
}
