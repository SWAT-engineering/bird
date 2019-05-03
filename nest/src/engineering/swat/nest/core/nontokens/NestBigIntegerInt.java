package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import java.math.BigInteger;
import java.nio.ByteOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

class NestBigIntegerInt implements NestBigInteger {

    private final int value;

    NestBigIntegerInt(int value) {
        this.value = value;
    }

    private static final int MIN_CACHE_ENTRY = -255;
    private static final int MAX_CACHE_ENTRY = 255;
    private static final NestBigIntegerInt[] SMALL_INT_CACHE = new NestBigIntegerInt[(MAX_CACHE_ENTRY - MIN_CACHE_ENTRY) + 1];
    private static final BigInteger[] SMALL_CONVERSION_CACHE = new BigInteger[(MAX_CACHE_ENTRY - MIN_CACHE_ENTRY) + 1];

    static {
        for (int i = MIN_CACHE_ENTRY; i <= MAX_CACHE_ENTRY; i++) {
            SMALL_INT_CACHE[i - MIN_CACHE_ENTRY] = new NestBigIntegerInt(i);
            SMALL_CONVERSION_CACHE[i - MIN_CACHE_ENTRY] = new BigInteger("" + i);
        }
    }

    static NestBigInteger ofInt(int value) {
        if (MIN_CACHE_ENTRY < value && value < MAX_CACHE_ENTRY) {
            return SMALL_INT_CACHE[value - MIN_CACHE_ENTRY];
        }
        return new NestBigIntegerInt(value);
    }

    @Override
    public int hashCode() {
        return 9 * value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NestBigIntegerInt) {
            return ((NestBigIntegerInt)obj).value == value;
        }
        return false;
    }


    @Override
    public int compareTo(NestBigInteger o) {
        if (o instanceof NestBigIntegerInt) {
            return Integer.compare(value, ((NestBigIntegerInt) o).value);
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
    public NestBigInteger add(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            int otherValue = ((NestBigIntegerInt)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value + otherValue;
            if (((otherValue ^ result) & (value ^ result)) >= 0) {
                // no overflow since sign is the same
                return ofInt(result);
            }
        }
        return val.add(this); // let the other side take care of it, since it is bigger
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            int otherValue = ((NestBigIntegerInt)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value - otherValue;
            if (((value ^ otherValue) & (otherValue ^ result)) >= 0) {
                return ofInt(result);
            }
        }
        return val.negate().subtract(this.negate());
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            int otherValue = ((NestBigIntegerInt)val).value;
            if (otherValue == 0) {
                return ofInt(0);
            }

            long result = (long)otherValue * (long)value;
            if ((int)result == result) {
                return ofInt((int)result);
            }
        }
        return val.multiply(this);
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            return ofInt(value / ((NestBigIntegerInt) val).value);
        }
        return NestBigInteger.of(toBigInteger().divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            return ofInt(Math.floorMod(value, ((NestBigIntegerInt) val).value));
        }
        return NestBigInteger.of(toBigInteger().mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        if (val instanceof NestBigIntegerInt) {
            return ofInt(value % ((NestBigIntegerInt) val).value);
        }
        return NestBigInteger.of(toBigInteger().remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        if (value >= 0) {
            return this;
        }
        return ofInt(-value);
    }

    @Override
    public NestBigInteger negate() {
        return ofInt(-value);
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

    @Override
    public ByteSlice getBytesSlice(ByteOrder order) {
        return ByteSlice.wrap(getBytes(order));
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



}
