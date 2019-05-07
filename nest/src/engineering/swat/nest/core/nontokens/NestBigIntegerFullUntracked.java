package engineering.swat.nest.core.nontokens;

import java.math.BigInteger;

class NestBigIntegerFullUntracked extends NestBigIntegerFullBase {

    NestBigIntegerFullUntracked(BigInteger value) {
        super(value);
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.add(val.toBigInteger()));
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.subtract(val.toBigInteger()));
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.multiply(val.toBigInteger()));
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        return NestBigInteger.ofUntracked(value.remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        BigInteger result = value.abs();
        if (result == value) {
            return this;
        }
        return NestBigInteger.ofUntracked(result);
    }

    @Override
    public NestBigInteger negate() {
        return NestBigInteger.ofUntracked(value.negate());
    }


    @Override
    public Origin getOrigin() {
        return Origin.EMPTY;
    }
}
