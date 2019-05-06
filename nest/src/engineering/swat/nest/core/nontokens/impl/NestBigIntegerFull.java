package engineering.swat.nest.core.nontokens.impl;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.math.BigInteger;
import java.nio.ByteOrder;

public class NestBigIntegerFull implements NestBigInteger {
    private final BigInteger value;

    NestBigIntegerFull(BigInteger value) {
        this.value = value;
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.add(val.toBigInteger()));
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.subtract(val.toBigInteger()));
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.multiply(val.toBigInteger()));
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        BigInteger result = value.abs();
        if (result == value) {
            return this;
        }
        return NestBigIntegerImplementations.of(result);
    }

    @Override
    public NestBigInteger negate() {
        return NestBigIntegerImplementations.of(value.negate());
    }

    @Override
    public NestBigInteger shr(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.shiftRight(val.intValueExact()));
    }

    @Override
    public NestBigInteger shl(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.shiftLeft(val.intValueExact()));
    }

    @Override
    public NestBigInteger and(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.and(val.toBigInteger()));
    }

    @Override
    public NestBigInteger xor(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.xor(val.toBigInteger()));
    }

    @Override
    public NestBigInteger or(NestBigInteger val) {
        return NestBigIntegerImplementations.of(value.or(val.toBigInteger()));
    }

    @Override
    public NestBigInteger not() {
        return NestBigIntegerImplementations.of(value.not());
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
    public ByteSlice getBytes(ByteOrder order) {
        byte[] bytes = value.toByteArray();
        if (order == ByteOrder.LITTLE_ENDIAN) {
            ByteUtils.reverseBytes(bytes);
        }
        return ByteSlice.wrap(bytes);
    }

    @Override
    public BigInteger toBigInteger() {
        return value;
    }

    @Override
    public int compareTo(NestBigInteger o) {
        return value.compareTo(o.toBigInteger());
    }
}
