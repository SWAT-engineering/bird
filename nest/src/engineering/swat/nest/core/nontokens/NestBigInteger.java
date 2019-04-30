package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.impl.NestBigIntegerImplementations;
import java.math.BigInteger;
import java.nio.ByteOrder;

public interface NestBigInteger extends Comparable<NestBigInteger> {



    // arithmetic operations
    NestBigInteger add(NestBigInteger val);
    NestBigInteger subtract(NestBigInteger val);
    NestBigInteger multiply(NestBigInteger val);
    NestBigInteger divide(NestBigInteger val);
    NestBigInteger mod(NestBigInteger val);
    NestBigInteger remainder(NestBigInteger val);
    NestBigInteger abs();
    NestBigInteger negate();

    // binary operations on numbers, so keep sign around
    NestBigInteger shr(NestBigInteger val);
    NestBigInteger shl(NestBigInteger val);
    NestBigInteger and(NestBigInteger val);
    NestBigInteger xor(NestBigInteger val);
    NestBigInteger or(NestBigInteger val);
    NestBigInteger not();

    int intValueExact();
    long longValueExact();
    boolean fitsInt();
    boolean fitsLong();

    byte[] getBytes(ByteOrder order);

    BigInteger toBigInteger();

    NestBigInteger ZERO = of(0);
    NestBigInteger ONE = of(1);
    NestBigInteger TWO = of(2);

    static NestBigInteger of(int value) {
        return NestBigIntegerImplementations.of(value);
    }
    static NestBigInteger of(BigInteger value) {
        return NestBigIntegerImplementations.of(value);
    }
}
