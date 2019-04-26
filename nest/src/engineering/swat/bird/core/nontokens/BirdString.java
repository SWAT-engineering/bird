package engineering.swat.bird.core.nontokens;

import java.nio.charset.Charset;
import engineering.swat.bird.core.bytes.BytesView;

public class BirdString extends NonToken {

	private final String value;

	public BirdString(BytesView bytes, Charset encoding) {
		this(new String(bytes.allBytes(), encoding));
	}

	public BirdString(String string) {
		this.value = string;
	}

	@Override
	public BytesView getBytes() {
		throw new RuntimeException("Unsupported");
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof String) {
			return obj.equals(value);
		}
		if (obj instanceof BirdString) {
			return ((BirdString)obj).value.equals(value);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return value.toString();
	}

}
