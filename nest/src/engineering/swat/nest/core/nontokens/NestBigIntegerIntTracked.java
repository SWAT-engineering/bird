package engineering.swat.nest.core.nontokens;

class NestBigIntegerIntTracked extends NestBigIntegerIntBase {

    private final Origin origin;

    NestBigIntegerIntTracked(Origin origin, int value) {
        super(value);
        this.origin = origin;
    }

    private static final NestBigIntegerIntTracked[] SMALL_INT_CACHE = new NestBigIntegerIntTracked[(MAX_CACHE_ENTRY - MIN_CACHE_ENTRY) + 1];

    static {
        for (int i = MIN_CACHE_ENTRY; i <= MAX_CACHE_ENTRY; i++) {
            SMALL_INT_CACHE[i - MIN_CACHE_ENTRY] = new NestBigIntegerIntTracked(Origin.EMPTY, i);
        }
    }

    static NestBigInteger ofInt(Origin origin, int value) {
        if (origin == Origin.EMPTY && MIN_CACHE_ENTRY < value && value < MAX_CACHE_ENTRY) {
            return SMALL_INT_CACHE[value - MIN_CACHE_ENTRY];
        }
        return new NestBigIntegerIntTracked(origin, value);
    }

    private Origin mergeOrigins(NestBigInteger other) {
        Origin otherOrigin = other.getOrigin();
        if (origin == otherOrigin || otherOrigin == Origin.EMPTY) {
            return origin;
        }
        if (origin == Origin.EMPTY) {
            return otherOrigin;
        }
        return origin.merge(otherOrigin);
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            int otherValue = ((NestBigIntegerIntTracked)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value + otherValue;
            if (((otherValue ^ result) & (value ^ result)) >= 0) {
                // no overflow since sign is the same
                return ofInt(mergeOrigins(val), result);
            }
        }
        return val.add(this); // let the other side take care of it, since it is bigger
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            int otherValue = ((NestBigIntegerIntTracked)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value - otherValue;
            if (((value ^ otherValue) & (otherValue ^ result)) >= 0) {
                return ofInt(mergeOrigins(val), result);
            }
        }
        return val.negate().subtract(this.negate());
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            int otherValue = ((NestBigIntegerIntTracked)val).value;
            if (otherValue == 0) {
                return ofInt(mergeOrigins(val), 0);
            }

            long result = (long)otherValue * (long)value;
            if ((int)result == result) {
                return ofInt(mergeOrigins(val), (int)result);
            }
        }
        return val.multiply(this);
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            return ofInt(mergeOrigins(val), value / ((NestBigIntegerIntTracked) val).value);
        }
        return NestBigInteger.of(toBigInteger().divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            return ofInt(mergeOrigins(val), Math.floorMod(value, ((NestBigIntegerIntTracked) val).value));
        }
        return NestBigInteger.of(toBigInteger().mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntTracked) {
            return ofInt(mergeOrigins(val), value % ((NestBigIntegerIntTracked) val).value);
        }
        return NestBigInteger.of(toBigInteger().remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        if (value >= 0) {
            return this;
        }
        return ofInt(origin, -value);
    }

    @Override
    public NestBigInteger negate() {
        return ofInt(origin, -value);
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }
}
