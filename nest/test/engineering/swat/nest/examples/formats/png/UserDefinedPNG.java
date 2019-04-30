package engineering.swat.nest.examples.formats.png;

import java.util.zip.CRC32;
import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.tokens.Token;

public class UserDefinedPNG {

	public static long crc32(Token data) {
		CRC32 hasher = new CRC32();
		ByteSlice bytes = data.getBytes();
		for (long i = 0; i < bytes.size(); i++) {
			hasher.update(bytes.get(i) & 0xFF);
		}
		return hasher.getValue();
	}

}
