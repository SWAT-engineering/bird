package engineering.swat.nest.examples.formats.png;

import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;

import java.util.BitSet;
import java.util.zip.CRC32;
import engineering.swat.nest.core.tokens.Token;

public class TrailingZeroes {

	public static NestBigInteger trailingZeroes(Token token) {
        byte[] bytes = token.getTrackedBytes().allBytes();
        ByteUtils.reverseBytes(bytes);
        BitSet bits = BitSet.valueOf(bytes);
        if (bits.isEmpty()) {
            return NestBigInteger.of(bytes.length * 8);
        }
        return NestBigInteger.of(bits.nextSetBit(0));
    }

}
