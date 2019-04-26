package engineering.swat.nest.constructed;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.source.ByteWindowBuilder;

public class CommonTestHelper {
	public static ByteStream wrap(int... bytes) {
		byte[] data = new byte[bytes.length];
		for (int i = 0; i < data.length; i++)  {
			data[i] = (byte)bytes[i];
		}
		try {
			return new ByteStream(ByteWindowBuilder.wrap(ByteBuffer.wrap(data), new URI("tmp:///test")) );
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

}
