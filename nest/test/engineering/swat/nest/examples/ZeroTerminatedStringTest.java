package engineering.swat.nest.examples;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.examples.formats.Strings.ASCIIZeroTerminated;
import engineering.swat.nest.examples.formats.Strings.UTF16ZeroTerminated;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ZeroTerminatedStringTest {

    @Test
    void utf16() {
        String input = "Hello world 42";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_16);
        byte[] zeroTerminated = new byte[bytes.length + 2];
        zeroTerminated[zeroTerminated.length - 1] = 0;
        zeroTerminated[zeroTerminated.length - 2] = 0;
        System.arraycopy(bytes, 0, zeroTerminated, 0, bytes.length);

        assertEquals(input, UTF16ZeroTerminated.parse(wrap(zeroTerminated), Context.DEFAULT).value.get());
    }

    @Test
    void ascii() {
        String input = "Hello world 42";
        byte[] bytes = input.getBytes(StandardCharsets.US_ASCII);
        byte[] zeroTerminated = new byte[bytes.length + 1];
        zeroTerminated[zeroTerminated.length - 1] = 0;
        System.arraycopy(bytes, 0, zeroTerminated, 0, bytes.length);

        assertEquals(input, ASCIIZeroTerminated.parse(wrap(zeroTerminated), Context.DEFAULT).value.get());
    }

}
