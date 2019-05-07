package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class UntilTest {

    private static byte[] ABC_BYTES = "abc".getBytes(StandardCharsets.US_ASCII);
    private static byte[] ABCABC_BYTES = "abcabc".getBytes(StandardCharsets.US_ASCII);

    @Test
    void abcBasicTest() {
        assertEquals("ab", cTerminatedToken(wrap(ABC_BYTES), 0, 1, 3 ).getBody().asString().get());
    }

    @Test
    void abcStepSize() {
        assertEquals("ab", cTerminatedToken(wrap(ABC_BYTES), 0, 2, 3 ).getBody().asString().get());
    }

    @Test
    void abcabcStepSize() {
        assertEquals("ab", cTerminatedToken(wrap(ABCABC_BYTES), 0, 2, 6).getBody().asString().get());
    }

    @Test
    void abcabcOffset() {
        assertEquals("abcab", cTerminatedToken(wrap(ABCABC_BYTES), 3, 1, 6 ).getBody().asString().get());
    }

    @Test
    void abcabcSkipFirstTerminator() {
        assertEquals("abcab", cTerminatedToken(wrap(ABCABC_BYTES), 1, 2, 6 ).getBody().asString().get());
    }

    @Test
    void abcabcFailTerminator() {
        assertThrows(ParseError.class, () -> {
            cTerminatedToken(wrap(ABCABC_BYTES), 1, 3, 6);
        });
    }



    private static TerminatedToken<UnsignedBytes, UnsignedBytes> cTerminatedToken(ByteStream source, int startOffset, int stepSize, int maxLength) {
        return TerminatedToken.parseUntil(source, Context.DEFAULT,
                NestBigInteger.of(startOffset), NestBigInteger.of(stepSize), NestBigInteger.of(maxLength),
                (b, c) -> new ByteStream(b).readUnsigned(b.size(), c),
                (s, c) -> {
                    UnsignedBytes parsedChar = s.readUnsigned(1, c);
                    if (parsedChar.getByteAt(NestBigInteger.ZERO) != 'c') {
                        throw new ParseError("Char.terminator", parsedChar);
                    }
                    return parsedChar;
                });
    }

}
