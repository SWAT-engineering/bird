package engineering.swat.bird.core.bytes.source;

public interface ByteWindow {
	long size();
	TrackedByte read(long index);
}
