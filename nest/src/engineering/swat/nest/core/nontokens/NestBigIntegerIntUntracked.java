package engineering.swat.nest.core.nontokens;

class NestBigIntegerIntUntracked extends NestBigIntegerIntBase {

    NestBigIntegerIntUntracked(int value) {
        super(value);
    }

    private static final NestBigIntegerIntUntracked[] SMALL_INT_CACHE = new NestBigIntegerIntUntracked[(MAX_CACHE_ENTRY - MIN_CACHE_ENTRY) + 1];

    static {
        for (int i = MIN_CACHE_ENTRY; i <= MAX_CACHE_ENTRY; i++) {
            SMALL_INT_CACHE[i - MIN_CACHE_ENTRY] = new NestBigIntegerIntUntracked( i);
        }
    }

    static NestBigIntegerIntUntracked ofInt(int value) {
        if (MIN_CACHE_ENTRY < value && value < MAX_CACHE_ENTRY) {
            return SMALL_INT_CACHE[value - MIN_CACHE_ENTRY];
        }
        return new NestBigIntegerIntUntracked(value);
    }

    @Override
    public NestBigInteger add(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntBase) {
            int otherValue = ((NestBigIntegerIntBase)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value + otherValue;
            if (((otherValue ^ result) & (value ^ result)) >= 0) {
                // no overflow since sign is the same
                return ofInt(result);
            }
        }
        return val.add(this); // let the other side take care of it, since it is bigger
    }

    @Override
    public NestBigInteger subtract(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntBase) {
            int otherValue = ((NestBigIntegerIntBase)val).value;
            if (otherValue == 0) {
                return this;
            }

            int result = value - otherValue;
            if (((value ^ otherValue) & (otherValue ^ result)) >= 0) {
                return ofInt(result);
            }
        }
        return val.negate().subtract(this.negate());
    }

    @Override
    public NestBigInteger multiply(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntBase) {
            int otherValue = ((NestBigIntegerIntBase)val).value;
            if (otherValue == 0) {
                return ofInt(0);
            }

            long result = (long)otherValue * (long)value;
            if ((int)result == result) {
                return ofInt((int)result);
            }
        }
        return val.multiply(this);
    }

    @Override
    public NestBigInteger divide(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntBase) {
            return ofInt(value / ((NestBigIntegerIntBase) val).value);
        }
        return NestBigInteger.ofUntracked(toBigInteger().divide(val.toBigInteger()));
    }

    @Override
    public NestBigInteger mod(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntUntracked) {
            return ofInt(Math.floorMod(value, ((NestBigIntegerIntUntracked) val).value));
        }
        return NestBigInteger.ofUntracked(toBigInteger().mod(val.toBigInteger()));
    }

    @Override
    public NestBigInteger remainder(NestBigInteger val) {
        if (val instanceof NestBigIntegerIntUntracked) {
            return ofInt(value % ((NestBigIntegerIntUntracked) val).value);
        }
        return NestBigInteger.ofUntracked(toBigInteger().remainder(val.toBigInteger()));
    }

    @Override
    public NestBigInteger abs() {
        if (value >= 0) {
            return this;
        }
        return ofInt(-value);
    }

    @Override
    public NestBigInteger negate() {
        return ofInt(-value);
    }

    @Override
    public Origin getOrigin() {
        return Origin.EMPTY;
    }
}
