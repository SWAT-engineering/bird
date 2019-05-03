package engineering.swat.nest.core.nontokens;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.ByteOrder;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class NestBitsTests {

    @Test
    void testOrWorks() {
        NestBits one = NestBits.of(0b1, 1);
        NestBits two = NestBits.of(0b10, 1);
        assertEquals(NestBits.of(0b11, 1), one.or(two));
    }

    @Test
    void testOrGrows() {
        NestBits one = NestBits.of(0b1, 1);
        NestBits two = NestBits.of(0b10, 2);
        assertNotEquals(NestBits.of(0b11, 1), one.or(two), "Min size should have grown");
        assertEquals(NestBits.of(0b11, 2), one.or(two));
    }

    @Test
    void testAndWorks() {
        NestBits one = NestBits.of(0b1, 1);
        NestBits two = NestBits.of(0b11, 1);
        assertEquals(NestBits.of(0b1, 1), one.and(two));
    }

    @Test
    void testAndGrows() {
        NestBits one = NestBits.of(0b1, 1);
        NestBits two = NestBits.of(0b11, 2);
        assertNotEquals(NestBits.of(0b1, 1), one.and(two), "Min size should have grown");
        assertEquals(NestBits.of(0b1, 2), one.and(two));
    }


    @Test
    void testShrWorks() {
        NestBits one = NestBits.of(0b100, 1);
        assertEquals(NestBits.of(0b1, 1), one.shr(NestBigInteger.TWO));

        one = NestBits.of(0b10100, 1);
        assertEquals(NestBits.of(0b101, 1), one.shr(NestBigInteger.TWO));
    }

    @Test
    void testShrDoesntShrink() {
        NestBits one = NestBits.of(0b100, 2);
        assertEquals(NestBits.of(0b1, 2), one.shr(NestBigInteger.TWO));

        NestBits b = NestBits.of(0b1 << 16, 3);
        assertEquals(NestBits.of(0b1 << 8, 3), b.shr(NestBigInteger.of(8)));
    }

    @Test
    void testShlWorks() {
        NestBits one = NestBits.of(0b1, 1);
        assertEquals(NestBits.of(0b100, 1), one.shl(NestBigInteger.TWO));

        one = NestBits.of(0b101, 1);
        assertEquals(NestBits.of(0b10100, 1), one.shl(NestBigInteger.TWO));
    }

    @Test
    void testShlGrows() {
        NestBits one = NestBits.of(0b1, 2);
        assertEquals(NestBits.of(0b1 << 15, 2), one.shl(NestBigInteger.of(15)));

        NestBits b = NestBits.of(0b1, 3);
        assertEquals(NestBits.of(0b1 << 15, 3), b.shl(NestBigInteger.of(15)));
    }


    private static Stream<byte[]> testBytes() {
        return Stream.of(
                new byte[] { 1, 2},
                new byte[] { 3, 1},
                new byte[] { 1, 2, 3},
                new byte[] { 1, 2, 3, 4},
                new byte[] { 1, 2, 3, 4, 5},
                new byte[] { 1, 1, 1, 1, 1},
                new byte[] { 21, 85, 119, -103},
                new byte[] { 1, 2, 3, 4 , -120}
                );
    }

    @ParameterizedTest
    @MethodSource("testBytes")
    void shlAndOrTest(byte[] data) {
        assertArrayEquals(data, NestBits.of(data, ByteOrder.BIG_ENDIAN).getBytes(ByteOrder.BIG_ENDIAN));
    }

}
