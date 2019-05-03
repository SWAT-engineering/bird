package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Origin;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UnsignedByte extends PrimitiveToken {

	private final int value;
	private final TrackedByteSlice slice;


	public UnsignedByte(TrackedByteSlice slice, Context ctx) {
		super(ctx);
		assert slice.size().equals(NestBigInteger.ONE);
		this.value = slice.getUnsigned(NestBigInteger.ZERO);
		this.slice = slice;
	}

	@Override
	public boolean sameBytes(Token other) {
		if (other instanceof UnsignedByte) {
			return value == ((UnsignedByte) other).value;
		}
		if (other instanceof UnsignedBytes) {
			return other.size().equals(NestBigInteger.ONE) && value == ((UnsignedBytes) other).getByteAt(NestBigInteger.ZERO);
		}
		return super.sameBytes(other);
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return slice;
	}
	
	@Override
	public NestBigInteger size() {
		return NestBigInteger.ONE;
	}
	
	@Override
	public String toString() {
		return String.format("u8: %2x (%s)", value, slice);
	}


	public int get() {
		return value;
	}

	@Override
	public NestValue asValue() {
		return new NestValue(Origin.of(slice), ctx, new byte[]{(byte)value});
	}

	@Override
	public String asString() {
		return new String(new byte[] { (byte)value }, ctx.getEncoding());
	}
}
