package engineering.swat.nest.core.tokens.primitive;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import java.util.List;

public class MultipleTokenByteSlice<T extends Token> implements TrackedByteSlice {
	private final List<T> contents;
	private final NestBigInteger[] offsets;
	private final NestBigInteger fullSize;

	public MultipleTokenByteSlice(List<T> contents, NestBigInteger[] offsets, NestBigInteger fullSize) {
		this.contents = contents;
		this.offsets = offsets;
		this.fullSize = fullSize;
	}

	public static <T extends Token> MultipleTokenByteSlice<T> buildByteView(List<T> contents) {
		NestBigInteger[] offsets = new NestBigInteger[contents.size()];
		NestBigInteger currentOffset = NestBigInteger.ZERO;
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = currentOffset;
			currentOffset = currentOffset.add(contents.get(i).size());
		}
		return new MultipleTokenByteSlice<T>(contents, offsets, currentOffset);
	}

	@Override
	public NestBigInteger size() {
		return fullSize;
	}

	@Override
	public byte get(NestBigInteger index) {
	    int entry = findEntry(index);
	    return contents.get(entry).getTrackedBytes().get(index.subtract(offsets[entry]));
	}

	private int findEntry(NestBigInteger index) {
		for (int i = 0; i < offsets.length; i++) {
			if (offsets[i].compareTo(index) > 0) {
				return i - 1;
			}
		}
		return offsets.length - 1;
	}


	@Override
	public ByteOrigin getOrigin(NestBigInteger index) {
		int entry = findEntry(index);
		return contents.get(entry).getTrackedBytes().getOrigin(index.subtract(offsets[entry]));
	}
}