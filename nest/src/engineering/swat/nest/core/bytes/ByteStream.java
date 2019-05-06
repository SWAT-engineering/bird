package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.Optional;

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

	public Optional<UnsignedBytes> readUnsigned(int size, Context ctx) {
	    return readUnsigned(NestBigInteger.ofUntracked(size), ctx);
	}


	public Optional<UnsignedBytes> readUnsigned(NestBigInteger size, Context ctx) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger newOffset = offset.add(size);
		if (newOffset.compareTo(limit) > 0) {
			ctx.fail("End of Stream reached {} {}", window, newOffset);
			return Optional.empty();
		}
		try {
			return Optional.of(new UnsignedBytes(window.slice(offset, size), ctx));
		}
		finally {
			offset = newOffset;
		}
	}

	public Optional<TrackedByteSlice> readSlice(NestBigInteger size, Context context) {
		assert size.compareTo(NestBigInteger.ZERO) >= 0;
		NestBigInteger sliceOffset = offset;
		NestBigInteger newOffset = sliceOffset.add(size);
		if (newOffset.compareTo(limit) > 0) {
			context.fail("End of Stream reached {} {}", window, newOffset);
			return Optional.empty();
		}
		offset = newOffset;

		return Optional.of(new TrackedByteSlice() {
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
		});
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
	public Optional<ByteStream> fork(NestBigInteger atOffset) {
	    if (atOffset.isNegative()) {
	    	throw new IndexOutOfBoundsException();
		}
	    if (atOffset.compareTo(limit) <= 0) {
			return Optional.of(new ByteStream(atOffset, limit, window));
		}
	    return Optional.empty();
	}

	public void sync(ByteStream other) {
		if (other.window != window) {
			throw new IllegalArgumentException("These byte streams did not fork at some point");
		}
		this.offset = other.offset;
	}
}
