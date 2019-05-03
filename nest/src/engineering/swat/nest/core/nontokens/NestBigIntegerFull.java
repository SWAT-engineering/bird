package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import java.math.BigInteger;
import java.nio.ByteOrder;

class NestBigIntegerFull implements NestBigInteger {
    private final BigInteger value;

    NestBigIntegerFull(BigInteger value) {
        this.value = value;
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        return NestBigInteger.of(value.add(val.toBigInteger()));
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        return NestBigInteger.of(value.subtract(val.toBigInteger()));
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        return NestBigInteger.of(value.multiply(val.toBigInteger()));
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        return NestBigInteger.of(value.divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        return NestBigInteger.of(value.mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        return NestBigInteger.of(value.remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        BigInteger result = value.abs();
        if (result == value) {
            return this;
        }
        return NestBigInteger.of(result);
    }

    @Override
    public NestBigInteger negate() {
        return NestBigInteger.of(value.negate());
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
    public ByteSlice getBytesSlice(ByteOrder order) {
        return ByteSlice.wrap(getBytes(order));
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
    public boolean equals(Object obj) {
        if (obj instanceof NestBigIntegerFull) {
            return value.equals(((NestBigIntegerFull) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 7 * value.hashCode();
    }
}
