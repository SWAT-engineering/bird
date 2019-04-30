package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;

public interface TrackedByteSlice extends ByteSlice {
	ByteOrigin getOrigin(NestBigInteger index);
	default TrackedByteSlice slice(NestBigInteger offset, NestBigInteger size) {
		final TrackedByteSlice base = this;
		final NestBigInteger currentOffset = offset;
		return new TrackedByteSlice() {
			@Override
			public ByteOrigin getOrigin(NestBigInteger index) {
				return base.getOrigin(currentOffset.add(index));
			}

			@Override
			public NestBigInteger size() {
				return size;
			}

			@Override
			public byte get(NestBigInteger index) {
				return base.get(currentOffset.add(index));
			}

			@Override
			public TrackedByteSlice slice(NestBigInteger offset, NestBigInteger size) {
			    // avoid extra nesting
				return base.slice(currentOffset.add(offset), size);
			}
		};
	}
}
