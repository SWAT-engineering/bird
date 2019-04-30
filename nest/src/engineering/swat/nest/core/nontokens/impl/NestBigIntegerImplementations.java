package engineering.swat.nest.core.nontokens.impl;

import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.math.BigInteger;

public class NestBigIntegerImplementations {

    public static NestBigInteger of(int value) {
        return NestBigIntegerInt.ofInt(value);
    }

    public static NestBigInteger of(long value) {
        if ((int) value == value) {
            return NestBigIntegerInt.ofInt((int)value);
        }
        return new NestBigIntegerFull(BigInteger.valueOf(value));
    }

    public static NestBigInteger of(BigInteger value) {
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
