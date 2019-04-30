package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.ByteSlice;
import java.nio.ByteOrder;

public class NonTokenBytes extends NonToken {

	private final byte[] bytes;

	private NonTokenBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	public static NonTokenBytes of(byte... bytes) {
		return new NonTokenBytes(bytes);
	}
	public static NonTokenBytes of(int... bytes) {
		byte[] data = new byte[bytes.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte)bytes[i];
		}
		return of(data);
	}

	@Override
	public ByteSlice getBytes() {
		return new ByteSlice() {
			
			@Override
			public NestBigInteger size() {
				return NestBigInteger.of(bytes.length);
			}
			
			@Override
			public byte get(NestBigInteger index) {
				int actualIndex = index.intValueExact();
				if (actualIndex > bytes.length) {
					throw new IndexOutOfBoundsException();
				}
				return bytes[actualIndex];
			}
		};
	}

	@Override
	public ByteSlice getBytes(ByteOrder order) {
		return getBytes();
	}
}
