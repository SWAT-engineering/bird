package engineering.swat.nest.core.bytes;


import engineering.swat.nest.core.nontokens.NestBigInteger;

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
}
