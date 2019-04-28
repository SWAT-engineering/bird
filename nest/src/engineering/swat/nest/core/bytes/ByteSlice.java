package engineering.swat.nest.core.bytes;

public interface ByteSlice {
	long size();
	byte get(long index);

	default ByteSlice slice(long offset, long size) {
		final ByteSlice base = this;
		final long currentOffset = offset;
		return new ByteSlice() {
			@Override
			public long size() {
				return size;
			}

			@Override
			public byte get(long index) {
				return base.get(currentOffset + index);
			}

			@Override
			public ByteSlice slice(long offset, long size) {
				return base.slice(currentOffset + offset, size);
			}
		};
	}

	default byte[] allBytes() {
		byte[] result = new byte[Math.toIntExact(size())];
		for (int i = 0; i < result.length; i++) {
			result[i] = get(i);
		}
		return result;
	}
}
