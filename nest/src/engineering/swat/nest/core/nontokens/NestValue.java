package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Nest Value automatically translates between implementations depending on the operation
 */
public class NestValue {
    private final Origin origin;
    private final Context context;
    private final byte[] bytes;

    public NestValue(Origin origin, Context context, byte[] bytes) {
        this.origin = origin;
        this.context = context;
        this.bytes = bytes;
    }


    public NestValue(TrackedByteSlice slice, Context ctx) {
        this(Origin.of(slice), ctx, slice.allBytes());
    }

    public static NestValue of(String s, Context ctx) {
        return new NestValue(Origin.EMPTY, ctx, s.getBytes(ctx.getEncoding()));
    }

    public static NestValue of(int value, int byteSize) {
        if (value == 0) {
            return of(new byte[byteSize]);
        }
        final byte[] bytes;
        switch (byteSize) {
            case 1:
                bytes = new byte[]{
                        (byte) value
                };
                break;
            case 2:
                bytes = new byte[]{
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 3:
                bytes = new byte[]{
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 4:
                bytes = new byte[]{
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            default:
                throw new IllegalArgumentException("Byte size should be between 1 and 4");
        }
        return of(bytes);
    }

    public static NestValue of(long value, int byteSize) {
        if (value == 0) {
            return of(new byte[byteSize]);
        }
        final byte[] bytes;
        switch (byteSize) {
            case 1:
                bytes = new byte[]{
                        (byte) value
                };
                break;
            case 2:
                bytes = new byte[]{
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 3:
                bytes = new byte[]{
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 4:
                bytes = new byte[]{
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 5:
                bytes = new byte[]{
                        (byte)(value >>> 32),
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 6:
                bytes = new byte[]{
                        (byte)(value >>> 40),
                        (byte)(value >>> 32),
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 7:
                bytes = new byte[]{
                        (byte)(value >>> 48),
                        (byte)(value >>> 40),
                        (byte)(value >>> 32),
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            case 8:
                bytes = new byte[]{
                        (byte)(value >>> 56),
                        (byte)(value >>> 48),
                        (byte)(value >>> 40),
                        (byte)(value >>> 32),
                        (byte)(value >>> 24),
                        (byte)(value >>> 16),
                        (byte)(value >>> 8),
                        (byte)(value & 0xFF)
                };
                break;
            default:
                throw new IllegalArgumentException("Byte size should be between 1 and 8");
        }
        return of(bytes);
    }

    public static NestValue of(byte[] bytes) {
        return new NestValue(Origin.EMPTY, Context.DEFAULT, bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    private NestBits getBits() {
        return NestBits.of(bytes, context.getByteOrder());
    }


    public NestValue shl(NestBigInteger amount) {
        return new NestValue(origin, context, getBits().shl(amount).getBytes(context.getByteOrder()));
    }

    public NestValue shr(NestBigInteger amount) {
        return new NestValue(origin,context, getBits().shr(amount).getBytes(context.getByteOrder()));
    }

    private NestValue and(Origin origin, NestBits val) {
        return new NestValue(this.origin.merge(origin), context, getBits().and(val).getBytes(context.getByteOrder()));
    }

    private NestValue or(Origin origin, NestBits val) {
        return new NestValue(this.origin.merge(origin), context, getBits().or(val).getBytes(context.getByteOrder()));
    }

    private NestValue xor(Origin origin,NestBits val) {
        return new NestValue(this.origin.merge(origin), context, getBits().xor(val).getBytes(context.getByteOrder()));
    }

    public NestValue and(NestValue val) {
        return and(val.origin, val.getBits());
    }

    public NestValue or(NestValue val) {
        return or(val.origin, val.getBits());
    }

    public NestValue xor(NestValue val) {
        return xor(val.origin, val.getBits());
    }

    public NestValue not() {
        return new NestValue(origin, context, getBits().not().getBytes(context.getByteOrder()));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof NestValue) {
            return sameBytes((NestValue) obj);
        }
        return false;
    }

    public boolean sameBytes(NestValue of) {
        if (of.context.getByteOrder() == context.getByteOrder()) {
            return Arrays.equals(getBytes(), of.getBytes());
        }
        else {
            int length = bytes.length;
            byte[] otherBytes = of.getBytes();
            if (otherBytes.length != length) {
                return false;
            }
            for (int i = 0; i < length; i++ ){
                if (bytes[i] != otherBytes[(length - i) - 1]) {
                    return false;
                }
            }
            return true;
        }
    }

    public NestBigInteger asInteger(Sign sign) {
        return NestBigInteger.of(origin, bytes, context.getByteOrder(), sign);
    }

    public Tracked<String> asString() {
        return new Tracked<>(origin, new String(getBytes(), context.getEncoding()));
    }
}
