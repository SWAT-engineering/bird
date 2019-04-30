package engineering.swat.nest.core.nontokens;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import engineering.swat.nest.core.bytes.ByteSlice;

public class NestInteger extends NonToken  {

	private final NestBigInteger value;

	public NestInteger(int value) {
		this(NestBigInteger.of(value));
	}

	public NestInteger(BigInteger bigInteger) {
	    this(NestBigInteger.of(bigInteger));
	}
	public NestInteger(NestBigInteger value) {
		this.value = value;
	}


	@Override
	public ByteSlice getBytes(ByteOrder order) {
		byte[] bytes = value.getBytes(order);
		return new ByteSlice() {
			@Override
			public NestBigInteger size() {
				return NestBigInteger.of(bytes.length);
			}

			@Override
			public byte get(NestBigInteger index) {
				int pos = index.intValueExact();
				if (pos > bytes.length) {
					throw new IndexOutOfBoundsException();
				}
				return bytes[pos];
			}
		};
	}

	public NestBigInteger getBigInteger() {
		return value;
	}

	public int intValueExact() {
		return value.intValueExact();
	}
	public long longValueExact() {
		return value.longValueExact();
	}
}
