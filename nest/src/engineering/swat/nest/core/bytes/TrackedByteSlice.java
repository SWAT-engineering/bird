package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.bytes.source.ByteOrigin;

public interface TrackedByteSlice extends ByteSlice {
	ByteOrigin getOrigin(long index);
	default TrackedByteSlice slice(long offset, long size) {
		final TrackedByteSlice base = this;
		final long currentOffset = offset;
		return new TrackedByteSlice() {
			@Override
			public ByteOrigin getOrigin(long index) {
				return base.getOrigin(currentOffset + index);
			}

			@Override
			public long size() {
				return size;
			}

			@Override
			public byte get(long index) {
				return base.get(currentOffset + index);
			}

			@Override
			public TrackedByteSlice slice(long offset, long size) {
			    // avoid extra nesting
				return base.slice(currentOffset + offset, size);
			}
		};
	}
}
