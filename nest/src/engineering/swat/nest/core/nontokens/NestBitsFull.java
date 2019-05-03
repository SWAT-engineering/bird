package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import org.checkerframework.checker.nullness.qual.Nullable;

class NestBitsFull implements NestBits {

    private final BitSet value;
    private final int minByteSize;

    NestBitsFull(BitSet value, int minByteSize) {
        this.value = value;
        this.minByteSize = minByteSize;
    }

    @Override
    public NestBits shl(NestBigInteger amount) {
        int shiftAmount = amount.intValueExact();
        BitSet result = new BitSet(value.length() + shiftAmount);
        for (int i = 0; i < value.length(); i++) {
            // TODO: optimize, only set the true bits
            result.set(i + shiftAmount, value.get(i));
        }
        return NestBits.of(result, Math.max(minByteSize, result.length() / 8));
    }

    @Override
    public NestBits shr(NestBigInteger amount) {
        int shiftAmount = amount.intValueExact();
        int remainingSize = Math.max(0, value.length() - shiftAmount);
        if (remainingSize == 0) {
            return NestBits.of(new BitSet(0), minByteSize);
        }
        return NestBits.of(value.get(shiftAmount, shiftAmount + remainingSize + 1), minByteSize);
    }


    @Override
    public NestBits and(NestBits val) {
        BitSet result = (BitSet) value.clone();
        result.and(getBitSet(val));
        return NestBits.of(result, Math.max(val.getMinByteSize(), minByteSize));
    }

    private static BitSet getBitSet(NestBits val) {
        BitSet valSet;
        if (val instanceof NestBitsFull) {
            valSet = ((NestBitsFull) val).value;
        }
        else {
            valSet = BitSet.valueOf(val.getBytes(ByteOrder.LITTLE_ENDIAN));
        }
        return valSet;
    }

    @Override
    public NestBits or(NestBits val) {
        BitSet result = (BitSet) value.clone();
        result.or(getBitSet(val));
        return NestBits.of(result, Math.max(val.getMinByteSize(), minByteSize));
    }

    @Override
    public NestBits xor(NestBits val) {
        BitSet result = (BitSet) value.clone();
        result.xor(getBitSet(val));
        return NestBits.of(result, Math.max(val.getMinByteSize(), minByteSize));
    }

    @Override
    public NestBits not() {
        BitSet result = (BitSet) value.clone();
        result.flip(0, minByteSize * 8);
        return NestBits.of(result, minByteSize);
    }

    @Override
    public byte[] getBytes(ByteOrder order) {
        byte[] bytes = value.toByteArray();
        if (order != ByteOrder.LITTLE_ENDIAN) {
            ByteUtils.reverseBytes(bytes);
        }

        if (bytes.length < minByteSize) {
            bytes = Arrays.copyOfRange(bytes, 0, minByteSize - bytes.length);
        }
        return bytes;
    }

    @Override
    public int getMinByteSize() {
        return minByteSize;
    }

    @Override
    public ByteSlice getByteSlice(ByteOrder order) {
        return ByteSlice.wrap(getBytes(order));
    }

    @Override
    public NestBigInteger trailingZeroes() {
        if (value.isEmpty()) {
            return NestBigInteger.ZERO;
        }
        return NestBigInteger.of(value.nextSetBit(0));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NestBitsFull) {
            return minByteSize == ((NestBitsFull) obj).minByteSize && value.equals(((NestBitsFull) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 11 * value.hashCode() + 13 * minByteSize;
    }

    @Override
    public boolean isZero() {
        return value.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("NB: %s (%d)", value, minByteSize);
    }
}
