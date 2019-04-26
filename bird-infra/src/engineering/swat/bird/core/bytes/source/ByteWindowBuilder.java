package engineering.swat.bird.core.bytes.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;

public class ByteWindowBuilder  {

	public static ByteWindow convert(InputStream stream, URI source) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[8*1024];
		int read;
		while ((read = stream.read(buffer)) > 0) {
			result.write(buffer, 0, read);
		}
		return wrap(ByteBuffer.wrap(result.toByteArray()), source);
	}

	private static ByteWindow wrap(ByteBuffer bytes, URI source) {
		return new ByteWindow() {
			final int size = bytes.capacity();
			@Override
			public long size() {
				return size;
			}
			
			@Override
			public TrackedByte read(long index) {
				if (index >= size) {
					throw new IndexOutOfBoundsException();
				}
				return new TrackedByte() {
					
					@Override
					public int getValue() {
						return bytes.get((int)index) & 0xFF;
					}
					
					@Override
					public URI getSource() {
						return source;
					}
					
					@Override
					public long getOffset() {
						return index;
					}

					@Override
					public String toString() {
						return String.format("0x%02X@%s:%d", getValue(), getSource(),  getOffset());
					}
				};
			}
		};
	}

}
