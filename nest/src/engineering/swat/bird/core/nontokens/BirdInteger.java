package engineering.swat.bird.core.nontokens;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import engineering.swat.bird.core.bytes.BytesView;

public class BirdInteger extends NonToken {

	private final long value;

	public BirdInteger(BytesView bytes, ByteOrder endianness) {
		ByteBuffer buf = ByteBuffer.wrap(bytes.allBytes());
		buf.order(endianness);
		switch (buf.remaining()) {
			case 1: this.value = buf.get() & 0xFFL; break;
			case 2: this.value = buf.getShort() & 0xFFFFL; break;
			case 4: this.value = buf.getInt() & 0xFFFF_FFFFL; break;
			case 8: this.value = buf.getLong(); break;
			default: throw new IllegalArgumentException("Cannot construct an integer from: " + buf.remaining() + "bytes");
		}
	}

	@Override
	public BytesView getBytes() {
		throw new RuntimeException("Unsupported operation");
	}

	public long getValue() {
		return value;
	}

}
