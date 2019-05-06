package engineering.swat.nest.examples;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.examples.formats.Varint.LEB128;
import engineering.swat.nest.examples.formats.Varint.PrefixVarint;
import java.io.ByteArrayOutputStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VarIntTests {

    private static final long[] TEST_VALUES = {1, 32, 128, 255, 0x1FF, 0xFFFF, 1L << 18, 1L << 60, 0xAAABBCCCCL, 0xABCDEF0123L, 5463458053L};

    private static LongStream getTestValues() {
        return LongStream.of(TEST_VALUES);
    }

    @ParameterizedTest
    @MethodSource("getTestValues")
    void leb128Test(long value) {
        NestBigInteger result = LEB128.parse(wrap(leb64(value)), Context.DEFAULT).get().value;
        assertEquals(NestBigInteger.of(value), result, String.format("Expected: 0x%x got 0x%x", value, result.longValueExact()));
    }

    @ParameterizedTest
    @Disabled
    @MethodSource("getTestValues")
    void prefixTest(long value) {
        NestBigInteger result = PrefixVarint.parse(wrap(prefix(value)), Context.DEFAULT).get().value;
        assertEquals(NestBigInteger.of(value), result, String.format("Expected: 0x%x got 0x%x", value, result.longValueExact()));
    }

    private static byte[] leb64(long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        do {
            int newByte = (int)(value & 0x7F) ;
            value >>>= 7;
            if (value != 0)
                newByte |= 0x80; // set high order bit to signal more bytes comint
            output.write(newByte);
        } while (value != 0);
        return output.toByteArray();
    }

    private static byte[] prefix(long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int bits = 64 - Long.numberOfLeadingZeros(value | 1);

        int bytes = 1 + (bits - 1) / 7;

        if (bits > 56) {
            output.write(0);
            bytes = 8;
        } else {
            value = (2 * value + 1) << (bytes - 1);
        }
        for (int n = 0; n < bytes; n++) {
            output.write((int)(value & 0xff));
            value >>>= 8;
        }
        return output.toByteArray();
    }

}
