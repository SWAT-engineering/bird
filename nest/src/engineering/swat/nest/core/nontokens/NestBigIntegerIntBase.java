package engineering.swat.nest.core.nontokens;

import java.math.BigInteger;
import java.nio.ByteOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class NestBigIntegerIntBase implements NestBigInteger {

    protected final int value;

    NestBigIntegerIntBase(int value) {
        this.value = value;
    }

    protected static final int MIN_CACHE_ENTRY = -255;
    protected static final int MAX_CACHE_ENTRY = 255;
    private static final BigInteger[] SMALL_CONVERSION_CACHE = new BigInteger[(MAX_CACHE_ENTRY - MIN_CACHE_ENTRY) + 1];

    static {
        for (int i = MIN_CACHE_ENTRY; i <= MAX_CACHE_ENTRY; i++) {
            SMALL_CONVERSION_CACHE[i - MIN_CACHE_ENTRY] = new BigInteger("" + i);
        }
    }

    @Override
    public int hashCode() {
        return 9 * value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NestBigIntegerIntBase) {
            return ((NestBigIntegerIntBase)obj).value == value;
        }
        return false;
    }


    @Override
    public int compareTo(NestBigInteger o) {
        if (o instanceof NestBigIntegerIntBase) {
            return Integer.compare(value, ((NestBigIntegerIntBase) o).value);
        }
        return o.compareTo(this) * -1;
    }

    @Override
    public BigInteger toBigInteger() {
        if (MIN_CACHE_ENTRY < value && value < MAX_CACHE_ENTRY) {
            return SMALL_CONVERSION_CACHE[value - MIN_CACHE_ENTRY];
        }
        return BigInteger.valueOf(value);
    }

    @Override
    public int intValueExact() {
        return value;
    }

    @Override
    public long longValueExact() {
        return value;
    }

    @Override
    public boolean fitsInt() {
        return true;
    }

    @Override
    public boolean fitsLong() {
        return true;
    }

    @Override
    public boolean isNegative() {
        return value < 0;
    }

    @Override
    public boolean isPositive() {
        return value >= 0;
    }

    @Override
    public boolean isZero() {
        return value == 0;
    }

    @Override
    public byte[] getBytes(ByteOrder order) {
        return order == ByteOrder.BIG_ENDIAN ? getBigEndianBytes() : getLittleEndianBytes();
    }

    private byte[] getLittleEndianBytes() {
        if ((value & 0x7F) == value) {
            return new byte[]{
                    (byte) (value & 0x7F)
            };
        }
        else if ((value & 0x7FF) == value) {
            return new byte[]{
                    (byte) (value & 0xFF),
                    (byte) ((value >> 8) & 0x7F)
            };
        }
        else if ((value & 0x7FFFF) == value) {
            return new byte[]{
                    (byte) (value & 0xFF),
                    (byte) ((value >> 8) & 0xFF),
                    (byte) ((value >> 16) & 0x7F)
            };
        }
        else {
            return new byte[]{
                    (byte) (value & 0xFF),
                    (byte) ((value >> 8) & 0xFF),
                    (byte) ((value >> 16) & 0xFF),
                    (byte) ((value >> 24) & 0xFF)
            };
        }
    }

    private byte[] getBigEndianBytes() {
        if ((value & 0x7F) == value) {
            return new byte[]{
                    (byte) (value & 0x7F)
            };
        }
        else if ((value & 0x7FF) == value) {
            return new byte[]{
                    (byte) ((value >> 8) & 0x7F),
                    (byte) (value & 0xFF)
            };
        }
        else if ((value & 0x7FFFF) == value) {
            return new byte[]{
                    (byte) ((value >> 16) & 0x7F),
                    (byte) ((value >> 8) & 0xFF),
                    (byte) (value & 0xFF)
            };
        }
        else {
            return new byte[]{
                    (byte) ((value >> 24) & 0xFF),
                    (byte) ((value >> 16) & 0xFF),
                    (byte) ((value >> 8) & 0xFF),
                    (byte) (value & 0xFF)
            };
        }
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public boolean lessThan(NestBigInteger other) {
        if (other instanceof NestBigIntegerIntBase) {
            return value < ((NestBigIntegerIntBase) other).value;
        }
        return compareTo(other) < 0;
    }

    @Override
    public boolean greaterThan(NestBigInteger other) {
        if (other instanceof NestBigIntegerIntBase) {
            return value > ((NestBigIntegerIntBase) other).value;
        }
        return compareTo(other) > 0;
    }
}
