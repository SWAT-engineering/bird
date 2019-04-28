package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.EOSError;
import engineering.swat.nest.core.tokens.UnsignedBytes;

public class ByteStream {
	
	private long offset;
	private final long limit;
	private final TrackedByteSlice window;

	public ByteStream(TrackedByteSlice window) {
		this(0, window.size(), window);
	}

	private ByteStream(long offset, long limit, TrackedByteSlice window) {
		this.offset = offset;
		this.limit = limit;
		this.window = window;
	}

	public UnsignedBytes readUnsigned(long size, Context ctx) {
		if (offset + size > limit) {
			throw new EOSError();
		}
		try {
			return new UnsignedBytes(window.slice(offset, offset + size), ctx);
		}
		finally {
			offset += size;
		}
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public boolean hasBytesRemaining() {
		return offset < limit;
	}

	public ByteStream fork() {
		return new ByteStream(offset, limit, window);
	}

	public void sync(ByteStream other) {
		if (other.window != window) {
			throw new IllegalArgumentException("These byte streams did not fork at some point");
		}
		this.offset = other.offset;
	}


}
