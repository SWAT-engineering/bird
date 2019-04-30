package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.math.BigInteger;
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

	public boolean sameBytes(ByteSlice other) {
		if (!other.size().equals(slice.size())) {
			return false;
		}
		NestBigInteger size = slice.size();
		for (NestBigInteger i = NestBigInteger.ZERO; i.compareTo(size) < 0; i = i.add(NestBigInteger.ONE)) {
			if (other.get(i) != slice.get(i)) {
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
		if (obj instanceof Token) {
			return sameBytes(((Token) obj).getTrackedBytes());
		}
		return false;
	}

	@Override
	public NestBigInteger asInteger() {
		int size = slice.size().intValueExact();
		if (ctx.getByteOrder() == ByteOrder.BIG_ENDIAN) {
			switch (size) {
				case 1:
					return NestBigInteger.of(slice.get(NestBigInteger.ZERO) & 0xFF);
				case 2:
					return NestBigInteger.of(
							(slice.get(NestBigInteger.ZERO) & 0xFF) << 8 |
									(slice.get(NestBigInteger.ONE) & 0xFF)
					);
				case 3:
					return NestBigInteger.of(
							(slice.get(NestBigInteger.ZERO) & 0xFF) << 16 |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8 |
									(slice.get(NestBigInteger.TWO) & 0xFF)
					);
				default:
					return NestBigInteger.of(new BigInteger(1, slice.allBytes()));
			}
		}
		else {
			switch (size) {
				case 1:
					return NestBigInteger.of(slice.get(NestBigInteger.ZERO) & 0xFF);
				case 2:
					return NestBigInteger.of(
							(slice.get(NestBigInteger.ZERO) & 0xFF)  |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8
					);
				case 3:
					return NestBigInteger.of(
							(slice.get(NestBigInteger.ZERO) & 0xFF) |
									(slice.get(NestBigInteger.ONE) & 0xFF) << 8 |
									(slice.get(NestBigInteger.TWO) & 0xFF) << 16
					);
				default:
					byte[] bytes = slice.allBytes();
					ByteUtils.reverseBytes(bytes);
					return NestBigInteger.of(new BigInteger(1, bytes));
			}
		}
	}
}
