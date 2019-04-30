package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestInteger;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UnsignedBytes extends PrimitiveToken {

	private final TrackedByteSlice slice;

	public UnsignedBytes(TrackedByteSlice slice, Context ctx) {
		super(ctx);
		this.slice = slice;
	}

	public int getByteAt(NestBigInteger position) {
	    return slice.getUnsigned(position);
	}

	public boolean sameBytes(NestValue other) {
		ByteSlice otherBytes = other.getBytes();
		if (!otherBytes.size().equals(slice.size())) {
			return false;
		}
		NestBigInteger size = slice.size();
		for (NestBigInteger i = NestBigInteger.ZERO; i.compareTo(size) < 0; i = i.add(NestBigInteger.ONE)) {
			if (otherBytes.get(i) != slice.get(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return slice;
	}
	
	@Override
	public NestBigInteger size() {
		return slice.size();
	}
	
	@Override
	public String toString() {
		return "Unsiged bytes:" + slice;
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj instanceof NestValue) {
			return sameBytes((NestValue) obj);
		}
		return false;
	}

	@Override
	public NestInteger asInteger() {
		int size = slice.size().intValueExact();
		if (ctx.getByteOrder() == ByteOrder.BIG_ENDIAN) {
			switch (size) {
				case 1:
					return new NestInteger(slice.get(NestBigInteger.ZERO) & 0xFF);
				case 2:
					return new NestInteger(
							(slice.get(NestBigInteger.ZERO) & 0xFF) << 8 |
									(slice.get(NestBigInteger.ONE) & 0xFF)
					);
				case 3:
					return new NestInteger(
							(slice.get(NestBigInteger.ZERO) & 0xFF) << 16 |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8 |
									(slice.get(NestBigInteger.TWO) & 0xFF)
					);
				default:
					return new NestInteger(new BigInteger(1, slice.allBytes()));
			}
		}
		else {
			switch (size) {
				case 1:
					return new NestInteger(slice.get(NestBigInteger.ZERO) & 0xFF);
				case 2:
					return new NestInteger(
							(slice.get(NestBigInteger.ZERO) & 0xFF)  |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8
					);
				case 3:
					return new NestInteger(
							(slice.get(NestBigInteger.ZERO) & 0xFF) |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8 |
									(slice.get(NestBigInteger.TWO) & 0xFF) << 16
					);
				default:
					byte[] bytes = slice.allBytes();
					ByteUtils.reverseBytes(bytes);
					return new NestInteger(new BigInteger(1, bytes));
			}
		}
	}
}
