package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Sign;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.function.Function;

/**
 * Nest Big Integer which wraps the {@link BigInteger} class to add some extra nest specific features
 */
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
    BigInteger toBigInteger();

    Origin getOrigin();

    NestBigInteger ZERO = ofUntracked(0);
    NestBigInteger ONE = ofUntracked(1);
    NestBigInteger TWO = ofUntracked(2);
    NestBigInteger THREE = ofUntracked(3);

    static NestBigInteger of(int value) {
        return of(Origin.EMPTY, value);
    }

    static NestBigInteger of(Origin origin, int value) {
        return NestBigIntegerIntTracked.ofInt(origin, value);
    }

    static NestBigInteger ofUntracked(int value) {
        return NestBigIntegerIntUntracked.ofInt(value);
    }

    static NestBigInteger of(long value) {
        return of(Origin.EMPTY, value);
    }
    static NestBigInteger of(Origin origin, long value) {
        if ((int) value == value) {
            return NestBigIntegerIntTracked.ofInt(origin, (int)value);
        }
        return new NestBigIntegerFullTracked(BigInteger.valueOf(value), origin);
    }

    static NestBigInteger ofUntracked(long value) {
        if ((int) value == value) {
            return NestBigIntegerIntUntracked.ofInt((int)value);
        }
        return new NestBigIntegerFullUntracked(BigInteger.valueOf(value));
    }

    static NestBigInteger of(byte[] bytes, ByteOrder byteOrder, Sign sign) {
        return of(Origin.EMPTY, bytes, byteOrder, sign);
    }

    static NestBigInteger of(Origin origin, byte[] bytes, ByteOrder byteOrder, Sign sign) {
        bytes = byteOrder == ByteOrder.BIG_ENDIAN ? bytes : ByteUtils.copyReverse(bytes);
        if (sign == Sign.UNSIGNED) {
            return fromBigEndianUnsigned(bytes, i -> NestBigIntegerIntTracked.ofInt(origin, i), bi -> new NestBigIntegerFullTracked(bi, origin));
        }
        return fromBigEndianSigned(bytes, i -> NestBigIntegerIntTracked.ofInt(origin, i), bi -> new NestBigIntegerFullTracked(bi, origin));
    }

    static NestBigInteger fromBigEndianUnsigned(byte[] bytes, Function<Integer, NestBigInteger> fromInt, Function<BigInteger, NestBigInteger> fromBigInt) {
        switch (bytes.length) {
            case 1:
                return fromInt.apply(bytes[0] & 0xFF);
            case 2:
                return fromInt.apply(
                        ((bytes[0] & 0xFF) << 8) |
                                ((bytes[1] & 0xFF))
                );
            case 3:
                return fromInt.apply(
                        ((bytes[0] & 0xFF) << 16) |
                                ((bytes[1] & 0xFF) << 8) |
                                ((bytes[2] & 0xFF))
                );
            case 4:
                if ((bytes[0] & 0x80) != 0) {
                    // we have a signed integer, so we must go to the big integer
                    break;
                }
                return fromInt.apply(
                        ((bytes[0] & 0xFF) << 24) |
                                ((bytes[1] & 0xFF) << 16) |
                                ((bytes[2] & 0xFF) << 8) |
                                ((bytes[3] & 0xFF))
                );
        }
        return fromBigInt.apply(new BigInteger(1, bytes));
    }

    static NestBigInteger fromBigEndianSigned(byte[] bytes, Function<Integer, NestBigInteger> fromInt, Function<BigInteger, NestBigInteger> fromBigInt) {
        switch (bytes.length) {
            case 1:
                return fromInt.apply((int)bytes[0]);
            case 2:
                return fromInt.apply(((bytes[0]) << 8) |
                                ((bytes[1] & 0xFF))
                );
            case 3:
                return fromInt.apply(((bytes[0]) << 16) |
                                ((bytes[1] & 0xFF) << 8) |
                                ((bytes[2] & 0xFF))
                );
            case 4:
                return fromInt.apply(((bytes[0]) << 24) |
                                ((bytes[1] & 0xFF) << 16) |
                                ((bytes[2] & 0xFF) << 8) |
                                ((bytes[3] & 0xFF))
                );
        }
        return fromBigInt.apply(new BigInteger(bytes));
    }

    static NestBigInteger ofUntracked(byte[] bytes, ByteOrder byteOrder, Sign sign) {
        bytes = byteOrder == ByteOrder.BIG_ENDIAN ? bytes : ByteUtils.copyReverse(bytes);
        if (sign == Sign.UNSIGNED) {
            return fromBigEndianUnsigned(bytes, NestBigIntegerIntUntracked::new, NestBigIntegerFullUntracked::new);
        }
        return fromBigEndianSigned(bytes, NestBigIntegerIntUntracked::new, NestBigIntegerFullUntracked::new);
    }


    static NestBigInteger ofUntracked(BigInteger value) {
        if (value.bitLength() < 31) {
            try {
                return NestBigIntegerIntUntracked.ofInt(value.intValueExact());
            }
            catch (ArithmeticException e){
                // continue as if nothing happened
            }
        }
        return new NestBigIntegerFullUntracked(value);
    }

    static NestBigInteger of(BigInteger value) {
        return of(Origin.EMPTY, value);
    }

    static NestBigInteger of(Origin origin, BigInteger value) {
        if (value.bitLength() < 31) {
            try {
                return NestBigIntegerIntTracked.ofInt(origin, value.intValueExact());
            }
            catch (ArithmeticException e){
                // continue as if nothing happened
            }
        }
        return new NestBigIntegerFullTracked(value, origin);
    }

    boolean greaterThan(NestBigInteger other);
    boolean lessThan(NestBigInteger other);
}
