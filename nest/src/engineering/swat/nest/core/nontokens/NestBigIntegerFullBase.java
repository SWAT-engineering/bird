package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteUtils;
import java.math.BigInteger;
import java.nio.ByteOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class NestBigIntegerFullBase implements NestBigInteger {

    protected final BigInteger value;

    public NestBigIntegerFullBase(BigInteger value) {
        this.value = value;
    }

    @Override
    public int intValueExact() {
        throw new ArithmeticException("Value does not fit in an int");
    }

    @Override
    public long longValueExact() {
        return value.longValueExact();
    }

    @Override
    public boolean fitsInt() {
        return false;
    }

    @Override
    public boolean fitsLong() {
        return value.bitLength() <= 63;
    }

    @Override
    public boolean isNegative() {
        return value.signum() < 0;
    }

    @Override
    public boolean isPositive() {
        return !isNegative();
    }

    @Override
    public boolean isZero() {
        return value.signum() == 0;
    }

    @Override
    public byte[] getBytes(ByteOrder order) {
        byte[] bytes = value.toByteArray();
        if (order == ByteOrder.LITTLE_ENDIAN) {
            ByteUtils.reverseBytes(bytes);
        }
        return bytes;
    }

    @Override
    public BigInteger toBigInteger() {
        return value;
    }

    @Override
    public int compareTo(NestBigInteger o) {
        return value.compareTo(o.toBigInteger());
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof NestBigIntegerFullBase) {
            return value.equals(((NestBigIntegerFullBase) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 7 * value.hashCode();
    }
}
