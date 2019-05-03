package engineering.swat.nest.core.bytes;


import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.util.Arrays;

public interface ByteSlice {
	NestBigInteger size();
	byte get(NestBigInteger index);
	default int getUnsigned(NestBigInteger index) {
		return get(index) & 0xFF;
	}

	default ByteSlice slice(NestBigInteger offset, NestBigInteger size) {
		final ByteSlice base = this;
		final NestBigInteger currentOffset = offset;
		return new ByteSlice() {
			@Override
			public NestBigInteger size() {
				return size;
			}

			@Override
			public byte get(NestBigInteger index) {
				return base.get(currentOffset.add(index));
			}

			@Override
			public ByteSlice slice(NestBigInteger offset, NestBigInteger size) {
				return base.slice(currentOffset.add(offset), size);
			}
		};
	}

	default byte[] allBytes() {
		byte[] result = new byte[size().intValueExact()];
		for (int i = 0; i < result.length; i++) {
			result[i] = get(NestBigInteger.of(i));
		}
		return result;
	}

	static ByteSlice wrap(byte[] bytes) {
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

			@Override
			public byte[] allBytes() {
				byte[] copy = new byte[bytes.length];
				System.arraycopy(bytes, 0, copy, 0, bytes.length);
				return copy;
			}
		};
	}

	default boolean sameBytes(ByteSlice bytes) {
		NestBigInteger mySize = size();
		if (!mySize.equals(bytes.size())) {
			return false;
		}
		for (NestBigInteger c = NestBigInteger.ZERO; c.compareTo(mySize) < 0; c = c.add(NestBigInteger.ONE)) {
			if (get(c) != bytes.get(c)) {
				return false;
			}
		}
		return true;
	}
}
