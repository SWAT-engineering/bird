package engineering.swat.nest.core.bytes.source;

public interface ByteWindow {
	long size();
	TrackedByte read(long index);
}
