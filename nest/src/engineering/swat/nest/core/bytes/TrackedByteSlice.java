package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;

public interface TrackedByteSlice  {
	NestBigInteger size();
	byte get(NestBigInteger index);
	ByteOrigin getOrigin(NestBigInteger index);

	default int getUnsigned(NestBigInteger index) {
		return get(index) & 0xFF;
	}

	default byte[] allBytes() {
		byte[] result = new byte[size().intValueExact()];
		for (int i = 0; i < result.length; i++) {
			result[i] = get(NestBigInteger.ofUntracked(i));
		}
		return result;
	}

	default boolean sameBytes(TrackedByteSlice bytes) {
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

	default boolean sameBytes(byte[] bytes) {
		NestBigInteger mySize = size();
		if (!mySize.fitsInt() || mySize.intValueExact() != bytes.length) {
			return false;
		}
		for (int c = 0; c < bytes.length; c++) {
			if (get(NestBigInteger.ofUntracked(c)) != bytes[c]) {
				return false;
			}
		}
		return true;

	}

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
