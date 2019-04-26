package engineering.swat.bird.core.tokens;

import java.util.List;
import engineering.swat.bird.core.bytes.TrackedBytesView;
import engineering.swat.bird.core.bytes.source.TrackedByte;

final class MultipleTokenBytesView<T extends Token> implements TrackedBytesView {
	private final List<T> contents;
	private final long[] sizes;
	private final long fullSize;

	public MultipleTokenBytesView(List<T> contents, long[] sizes, long fullSize) {
		this.contents = contents;
		this.sizes = sizes;
		this.fullSize = fullSize;
	}

	@Override
	public long size() {
		return fullSize;
	}

	@Override
	public TrackedByte getOriginal(long index) {
		if (index >= fullSize) {
			throw new IndexOutOfBoundsException();
		}
		long skipped = 0;
		int subsetIndex = 0;
		while (skipped <= index) {
			skipped += sizes[subsetIndex++];
		}
		if (skipped > 0) {
			skipped -= sizes[--subsetIndex]; // rewind the last skipped, since we found our block
		}
		return contents.get(subsetIndex).getTrackedBytes().getOriginal(index - skipped);
	}
}