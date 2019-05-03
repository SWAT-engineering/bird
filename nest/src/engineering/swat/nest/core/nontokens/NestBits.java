package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.nio.ByteOrder;
import java.util.BitSet;

interface NestBits {

    NestBits shl(NestBigInteger amount);
    NestBits shr(NestBigInteger amount);
    NestBits and(NestBits val);
    NestBits or(NestBits val);
    NestBits xor(NestBits val);
    NestBits not();

    boolean isZero();
    byte[] getBytes(ByteOrder order);
    ByteSlice getByteSlice(ByteOrder order);
    NestBigInteger trailingZeroes();

    static NestBits of(byte[] bytes, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return of(BitSet.valueOf(bytes), bytes.length);
        }
        else {
            return of(BitSet.valueOf(ByteUtils.copyReverse(bytes)), bytes.length);
        }
    }

    static NestBits of(int v, int minByteSize) {
        return of(BitSet.valueOf(new long[] { v }), minByteSize);
    }

    static NestBits of(long v, int minByteSize) {
        return of(BitSet.valueOf(new long[] { v }), minByteSize);
    }

    static NestBits of(BitSet result, int minByteSize) {
        return new NestBitsFull(result, minByteSize);
    }

    int getMinByteSize();
}
