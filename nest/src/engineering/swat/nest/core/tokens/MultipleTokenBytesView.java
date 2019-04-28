package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import java.util.List;

final class MultipleTokenBytesView<T extends Token> implements TrackedBytesView {
	private final List<T> contents;
	private final long[] offsets;
	private final long fullSize;

	public MultipleTokenBytesView(List<T> contents, long[] offsets, long fullSize) {
		this.contents = contents;
		this.offsets = offsets;
		this.fullSize = fullSize;
	}

	@Override
	public long size() {
		return fullSize;
	}

	@Override
	public byte get(long index) {
	    int entry = findEntry(index);
	    return contents.get(entry).getTrackedBytes().get(index - offsets[entry]);
	}

	private int findEntry(long index) {
		for (int i = 0; i < offsets.length; i++) {
			if (offsets[i] > index) {
				return i - 1;
			}
		}
		return offsets.length - 1;
	}


	@Override
	public ByteOrigin getOrigin(long index) {
		int entry = findEntry(index);
		return contents.get(entry).getTrackedBytes().getOrigin(index - offsets[entry]);
	}
}