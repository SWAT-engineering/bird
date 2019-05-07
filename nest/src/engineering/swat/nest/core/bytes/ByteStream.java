package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.EOSError;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;

/**
 * Read bytes from a slice
 */
public class ByteStream {
	
	private volatile NestBigInteger offset;
	private final NestBigInteger limit;
	private final TrackedByteSlice slice;

	public ByteStream(TrackedByteSlice slice) {
		this(NestBigInteger.ZERO, slice.size(), slice);
	}

	public ByteStream(Token source) {
		this(source.getTrackedBytes());
	}

	private ByteStream(NestBigInteger offset, NestBigInteger limit, TrackedByteSlice slice) {
		this.offset = offset;
		this.limit = limit;
		this.slice = slice;
	}

	/**
	 * Read size bytes from the stream
	 */
	public UnsignedBytes readUnsigned(int size, Context ctx) {
	    return readUnsigned(NestBigInteger.ofUntracked(size), ctx);
	}

	/**
	 * Read size bytes from the stream
	 */
	public UnsignedBytes readUnsigned(NestBigInteger size, Context ctx) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger newOffset = offset.add(size);
		if (newOffset.greaterThan(limit)) {
			throw new EOSError();
		}
		try {
			return new UnsignedBytes(slice.slice(offset, size), ctx);
		}
		finally {
			offset = newOffset;
		}
	}

	/**
	 * Read size bytes from the stream but create a new slice that can be used for nested parsing
	 */
	public TrackedByteSlice readSlice(NestBigInteger size) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger sliceOffset = offset;
		NestBigInteger newOffset = sliceOffset.add(size);
		if (newOffset.greaterThan(limit)) {
			throw new EOSError();
		}
		offset = newOffset;

		return new TrackedByteSlice() {
			@Override
			public ByteOrigin getOrigin(NestBigInteger index) {
			    if (index.compareTo(size) >= 0) {
			    	throw new IndexOutOfBoundsException();
				}
				return slice.getOrigin(sliceOffset.add(index));
			}

			@Override
			public NestBigInteger size() {
				return size;
			}

			@Override
			public byte get(NestBigInteger index) {
				return slice.get(sliceOffset.add(index));
			}
		};
	}

	public NestBigInteger getOffset() {
		return offset;
	}

	public boolean hasBytesRemaining() {
	    return offset.compareTo(limit) < 0;
	}

	/**
	 * Create a copy so that they can consume bytes in parallel without affecting each others index.
	 */
	public ByteStream fork() {
		return new ByteStream(offset, limit, slice);
	}

	/**
	 * Create a copy so that they can consume bytes in parallel without affecting each others index. and seek the new fork to different offset
	 */
	public ByteStream fork(NestBigInteger atOffset) {
	    if (atOffset.isNegative()) {
	    	throw new IndexOutOfBoundsException();
		}
	    if (atOffset.greaterThan(limit)) {
			throw new EOSError();
		}
		return new ByteStream(atOffset, limit, slice);
	}

	/**
	 * Sync this stream with the state of a previously forked stream.
	 */
	public void sync(ByteStream other) {
		if (other.slice != slice) {
			throw new IllegalArgumentException("These byte streams did not fork at some point");
		}
		this.offset = other.offset;
	}
}
