package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.bytes.BytesView;

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
	public BytesView getBytes() {
		return new BytesView() {
			
			@Override
			public long size() {
				return bytes.length;
			}
			
			@Override
			public byte get(long index) {
				return bytes[Math.toIntExact(index)];
			}
		};
	}

}
