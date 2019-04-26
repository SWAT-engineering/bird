package engineering.swat.bird.core.bytes;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import engineering.swat.bird.core.nontokens.BirdInteger;
import engineering.swat.bird.core.nontokens.BirdString;

public class Context {

	private final ByteOrder endianness;
	private final Charset encoding;

	private Context(Charset encoding, ByteOrder endianness) {
		this.encoding = encoding;
		this.endianness = endianness;
	}
	
	public static Context DEFAULT_CONTEXT = new Context(StandardCharsets.US_ASCII, ByteOrder.LITTLE_ENDIAN);

	public Context setEncoding(Charset encoding) {
		return new Context(encoding, endianness);
	}

	public Context setByteOrder(ByteOrder endianness) {
		return new Context(encoding, endianness);
	}

	public BirdInteger createInteger(BytesView bytes) {
		return new BirdInteger(bytes, endianness);
	}

	public BirdString createString(BytesView bytes) {
		return new BirdString(bytes, encoding);
	}
}
