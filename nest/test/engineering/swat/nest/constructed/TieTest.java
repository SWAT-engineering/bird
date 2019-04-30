package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import org.junit.jupiter.api.Test;

public class TieTest {

    @Test
    void tieWorks() {
        Tie1 parsed = Tie1.parse(wrap(1, 2, 3, 4), Context.DEFAULT);
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data1.asInteger().intValueExact());
        assertEquals(0x03 << 8 | 0x04, parsed.other.data2.asInteger().intValueExact());
    }

    @Test
    void tieFlipWorks() {
        Tie2 parsed = Tie2.parse(wrap(1, 2, 3, 4), Context.DEFAULT);
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.data1.asInteger().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data2.asInteger().intValueExact());
    }

    private static class Tie1 extends UserDefinedToken {
        public final UnsignedBytes data;
        public final OtherStruct other;

        private Tie1(UnsignedBytes data, OtherStruct other) {
            this.data = data;
            this.other = other;
        }
        public static Tie1 parse(ByteStream source, Context ctx) {
            UnsignedBytes data = source.readUnsigned(4, ctx);
            // a tie is just a new bytestream that can start at any token
            OtherStruct other = OtherStruct.parse(new ByteStream(data), ctx);
            return new Tie1(data, other);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            // do not include the tie field to the tracked bytes view !
            return buildTrackedView(data);
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


        public static Tie2 parse(ByteStream source, Context ctx) {
            UnsignedBytes data1 = source.readUnsigned(2, ctx);
            UnsignedBytes data2 = source.readUnsigned(2, ctx);
            // a tie is just a new bytestream that can start at any token
            OtherStruct other = OtherStruct.parse(new ByteStream(TokenList.of(data2, data1)), ctx);
            return new Tie2(data1, data2, other);
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

        public static OtherStruct parse(ByteStream source, Context ctx) {
            UnsignedBytes data1 = source.readUnsigned(2, ctx);
            UnsignedBytes data2 = source.readUnsigned(2, ctx);
            return new OtherStruct(data1, data2);

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
