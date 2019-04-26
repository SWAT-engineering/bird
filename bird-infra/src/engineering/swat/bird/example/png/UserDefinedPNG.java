package engineering.swat.bird.example.png;

import java.util.zip.CRC32;
import engineering.swat.bird.core.bytes.BytesView;
import engineering.swat.bird.core.tokens.Token;
import engineering.swat.bird.core.tokens.UnsignedBytes;

public class UserDefinedPNG {

	public static long crc32(Token data) {
		CRC32 hasher = new CRC32();
		BytesView bytes = data.getBytes();
		for (long i = 0; i < bytes.size(); i++) {
			hasher.update(bytes.byteAt(i));
		}
		return hasher.getValue();
	}

}
