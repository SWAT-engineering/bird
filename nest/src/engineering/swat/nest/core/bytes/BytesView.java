package engineering.swat.nest.core.bytes;

public interface BytesView {
	long size();
	byte get(long index);
	
	default byte[] allBytes() {
		byte[] result = new byte[Math.toIntExact(size())];
		for (int i = 0; i < result.length; i++) {
			result[i] = get(i);
		}
		return result;
	}
}
