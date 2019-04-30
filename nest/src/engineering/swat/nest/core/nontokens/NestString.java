package engineering.swat.nest.core.nontokens;

import java.nio.charset.Charset;
import engineering.swat.nest.core.bytes.ByteSlice;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NestString extends NonToken {

	private final String value;

	public NestString(ByteSlice bytes, Charset encoding) {
		this(new String(bytes.allBytes(), encoding));
	}

	public NestString(String string) {
		this.value = string;
	}

	@Override
	public ByteSlice getBytes() {
		throw new RuntimeException("Unsupported");
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof String) {
			return obj.equals(value);
		}
		if (obj instanceof NestString) {
			return ((NestString)obj).value.equals(value);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return value.toString();
	}

}
