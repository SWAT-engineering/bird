package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Sign;
import java.math.BigInteger;
import java.nio.ByteOrder;

public interface NestBigInteger extends Comparable<NestBigInteger> {
    NestBigInteger add(NestBigInteger val);
    NestBigInteger subtract(NestBigInteger val);
    NestBigInteger multiply(NestBigInteger val);
    NestBigInteger divide(NestBigInteger val);
    NestBigInteger mod(NestBigInteger val);
    NestBigInteger remainder(NestBigInteger val);
    NestBigInteger abs();
    NestBigInteger negate();

    int intValueExact();
    long longValueExact();
    boolean fitsInt();
    boolean fitsLong();

    boolean isNegative();
    boolean isZero();
    boolean isPositive();

    byte[] getBytes(ByteOrder order);
    ByteSlice getBytesSlice(ByteOrder order);
    BigInteger toBigInteger();

    NestBigInteger ZERO = of(0);
    NestBigInteger ONE = of(1);
    NestBigInteger TWO = of(2);
    NestBigInteger THREE = of(3);

    static NestBigInteger of(int value) {
        return NestBigIntegerInt.ofInt(value);
    }
    static NestBigInteger of(long value) {
        if ((int) value == value) {
            return NestBigIntegerInt.ofInt((int)value);
        }
        return new NestBigIntegerFull(BigInteger.valueOf(value));
    }


    static NestBigInteger of(byte[] bytes, ByteOrder byteOrder, Sign sign) {
        bytes = byteOrder == ByteOrder.BIG_ENDIAN ? bytes : ByteUtils.copyReverse(bytes);
        if (sign == Sign.UNSIGNED) {
            return fromBigEndianUnsigned(bytes);
        }
        return fromBigEndianSigned(bytes);
    }

    static NestBigInteger fromBigEndianUnsigned(byte[] bytes) {
        switch (bytes.length) {
            case 1:
                return NestBigInteger.of(bytes[0] & 0xFF);
            case 2:
                return NestBigInteger.of(
                        ((bytes[0] & 0xFF) << 8) |
                                ((bytes[1] & 0xFF))
                );
            case 3:
                return NestBigInteger.of(
                        ((bytes[0] & 0xFF) << 16) |
                                ((bytes[1] & 0xFF) << 8) |
                                ((bytes[2] & 0xFF))
                );
            case 4:
                if ((bytes[0] & 0x80) != 0) {
                    // we have a signed integer, so we must go to the big integer
                    break;
                }
                return NestBigInteger.of(
                        ((bytes[0] & 0xFF) << 24) |
                                ((bytes[1] & 0xFF) << 16) |
                                ((bytes[2] & 0xFF) << 8) |
                                ((bytes[3] & 0xFF))
                );
        }
        return NestBigInteger.of(new BigInteger(1, bytes));
    }

    static NestBigInteger fromBigEndianSigned(byte[] bytes) {
        switch (bytes.length) {
            case 1:
                return NestBigInteger.of(bytes[0]);
            case 2:
                return NestBigInteger.of(
                        ((bytes[0]) << 8) |
                                ((bytes[1] & 0xFF))
                );
            case 3:
                return NestBigInteger.of(
                        ((bytes[0]) << 16) |
                                ((bytes[1] & 0xFF) << 8) |
                                ((bytes[2] & 0xFF))
                );
            case 4:
                return NestBigInteger.of(
                        ((bytes[0]) << 24) |
                                ((bytes[1] & 0xFF) << 16) |
                                ((bytes[2] & 0xFF) << 8) |
                                ((bytes[3] & 0xFF))
                );
        }
        return NestBigInteger.of(new BigInteger(bytes));
    }

    static NestBigInteger of(BigInteger value) {
        if (value.bitLength() < 31) {
            try {
                return NestBigIntegerInt.ofInt(value.intValueExact());
            }
            catch (ArithmeticException e){
                // continue as if nothing happened
            }
        }
        return new NestBigIntegerFull(value);
    }
}
