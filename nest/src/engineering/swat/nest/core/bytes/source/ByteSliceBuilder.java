package engineering.swat.nest.core.bytes.source;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;

public class ByteSliceBuilder {

	public static TrackedByteSlice convert(InputStream stream, URI source) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[8*1024];
		int read;
		while ((read = stream.read(buffer)) > 0) {
			result.write(buffer, 0, read);
		}
		return wrap(ByteBuffer.wrap(result.toByteArray()), source);
	}

	public static TrackedByteSlice wrap(ByteBuffer bytes, URI source) {
		return new ByteBufferSlice(bytes, 0, bytes.limit(), source);
	}

}
