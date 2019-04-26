package engineering.swat.nest.core.bytes;

public interface BytesView {
	long size();
	int byteAt(long index);
	
	default byte[] allBytes() {
		byte[] result = new byte[Math.toIntExact(size())];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte)byteAt(i);
		}
		return result;
	}
}
