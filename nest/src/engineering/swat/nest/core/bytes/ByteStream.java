package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.EOSError;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;

public class ByteStream {
	
	private volatile NestBigInteger offset;
	private final NestBigInteger limit;
	private final TrackedByteSlice window;

	public ByteStream(TrackedByteSlice window) {
		this(NestBigInteger.ZERO, window.size(), window);
	}

	public ByteStream(Token source) {
		this(source.getTrackedBytes());
	}

	private ByteStream(NestBigInteger offset, NestBigInteger limit, TrackedByteSlice window) {
		this.offset = offset;
		this.limit = limit;
		this.window = window;
	}

	public UnsignedBytes readUnsigned(int size, Context ctx) {
	    return readUnsigned(NestBigInteger.of(size), ctx);
	}
	public UnsignedBytes readUnsigned(NestBigInteger size, Context ctx) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger newOffset = offset.add(size);
		if (newOffset.compareTo(limit) > 0) {
			throw new EOSError();
		}
		try {
			return new UnsignedBytes(window.slice(offset, size), ctx);
		}
		finally {
			offset = newOffset;
		}
	}

	public TrackedByteSlice readSlice(NestBigInteger size) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger sliceOffset = offset;
		NestBigInteger newOffset = sliceOffset.add(size);
		if (newOffset.compareTo(limit) > 0) {
			throw new EOSError();
		}
		offset = newOffset;

		return new TrackedByteSlice() {
			@Override
			public ByteOrigin getOrigin(NestBigInteger index) {
			    if (index.compareTo(size) >= 0) {
			    	throw new IndexOutOfBoundsException();
				}
				return window.getOrigin(sliceOffset.add(index));
			}

			@Override
			public NestBigInteger size() {
				return size;
			}

			@Override
			public byte get(NestBigInteger index) {
				return window.get(sliceOffset.add(index));
			}
		};
	}

	public NestBigInteger getOffset() {
		return offset;
	}

	public boolean hasBytesRemaining() {
	    return offset.compareTo(limit) < 0;
	}

	public ByteStream fork() {
		return new ByteStream(offset, limit, window);
	}
	public ByteStream fork(NestBigInteger atOffset) {
	    if (atOffset.isNegative()) {
	    	throw new IndexOutOfBoundsException();
		}
		return new ByteStream(atOffset, limit, window);
	}

	public void sync(ByteStream other) {
		if (other.window != window) {
			throw new IllegalArgumentException("These byte streams did not fork at some point");
		}
		this.offset = other.offset;
	}
}
