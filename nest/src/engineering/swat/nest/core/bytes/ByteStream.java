package engineering.swat.nest.core.bytes;

import java.io.InputStream;
import engineering.swat.nest.core.EOSError;
import engineering.swat.nest.core.bytes.source.ByteWindow;
import engineering.swat.nest.core.bytes.source.TrackedByte;
import engineering.swat.nest.core.tokens.UnsignedBytes;

public class ByteStream {
	
	private long offset;
	private final long limit;
	private final ByteWindow window;

	public ByteStream(ByteWindow window) {
		offset = 0;
		limit = window.size();
		this.window = window;
	}

	public UnsignedBytes readUnsigned(int size, Context ctx) {
		if (offset + size > limit) {
			throw new EOSError();
		}
		TrackedByte[] data = new TrackedByte[size];
		for (int i = 0; i < size; i++) {
			data[i] = window.read(offset++);
		}
		return new UnsignedBytes(data, ctx);
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}


}
