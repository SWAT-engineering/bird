package engineering.swat.nest.core.bytes;

import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Context {

	private final ByteOrder endianness;
	private final Charset encoding;

	private Context(Charset encoding, ByteOrder endianness) {
		this.encoding = encoding;
		this.endianness = endianness;
	}
	
	public static Context DEFAULT = new Context(StandardCharsets.US_ASCII, ByteOrder.BIG_ENDIAN);

	public Context setEncoding(Charset encoding) {
		return new Context(encoding, endianness);
	}

	public Context setByteOrder(ByteOrder endianness) {
		return new Context(encoding, endianness);
	}

	public ByteOrder getByteOrder() {
	    return this.endianness;
	}

	public ByteSlice getStringBytes(String value) {
	    ByteBuffer bytes = encoding.encode(value);
		return new ByteSlice() {
			@Override
			public NestBigInteger size() {
			    return NestBigInteger.of(bytes.limit());
			}

			@Override
			public byte get(NestBigInteger index) {
			    return bytes.get(index.intValueExact());
			}
		};
	}
}
