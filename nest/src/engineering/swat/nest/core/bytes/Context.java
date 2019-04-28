package engineering.swat.nest.core.bytes;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.nontokens.NestString;

public class Context {

	private final ByteOrder endianness;
	private final Charset encoding;

	private Context(Charset encoding, ByteOrder endianness) {
		this.encoding = encoding;
		this.endianness = endianness;
	}
	
	public static Context DEFAULT_CONTEXT = new Context(StandardCharsets.US_ASCII, ByteOrder.BIG_ENDIAN);

	public Context setEncoding(Charset encoding) {
		return new Context(encoding, endianness);
	}

	public Context setByteOrder(ByteOrder endianness) {
		return new Context(encoding, endianness);
	}

	public NestInteger createInteger(BytesView bytes) {
		return new NestInteger(bytes, endianness);
	}

	public NestString createString(BytesView bytes) {
		return new NestString(bytes, encoding);
	}

	public ByteOrder getByteOrder() {
	    return this.endianness;
	}
}
