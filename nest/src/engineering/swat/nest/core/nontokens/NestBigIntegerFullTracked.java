package engineering.swat.nest.core.nontokens;

import java.math.BigInteger;

class NestBigIntegerFullTracked extends NestBigIntegerFullBase {

    private final Origin origin;

    NestBigIntegerFullTracked(BigInteger value, Origin origin) {
        super(value);
        this.origin = origin;
    }

    private Origin mergeOrigins(NestBigInteger other) {
        return origin.merge(other.getOrigin());
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.add(val.toBigInteger()));
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.subtract(val.toBigInteger()));
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.multiply(val.toBigInteger()));
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        return NestBigInteger.of(mergeOrigins(val), value.remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        BigInteger result = value.abs();
        if (result == value) {
            return this;
        }
        return NestBigInteger.of(origin, result);
    }

    @Override
    public NestBigInteger negate() {
        return NestBigInteger.of(origin, value.negate());
    }


    @Override
    public Origin getOrigin() {
        return origin;
    }
}
