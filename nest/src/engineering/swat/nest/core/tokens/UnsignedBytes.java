package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnsignedBytes extends PrimitiveToken implements Iterable<UnsignedByte> {

	private final TrackedByteSlice slice;

	public UnsignedBytes(TrackedByteSlice slice, Context ctx) {
		super(ctx);
		this.slice = slice;
	}

	public int getByteAt(NestBigInteger position) {
	    return slice.getUnsigned(position);
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return slice;
	}
	
	@Override
	public NestBigInteger size() {
		return slice.size();
	}
	
	@Override
	public String toString() {
		return "u8[]:" + slice;
	}


	public List<UnsignedByte> bytes() {
		NestBigInteger size = slice.size();
	    List<UnsignedByte> result = new ArrayList<>(size.intValueExact());
	    for (NestBigInteger c = NestBigInteger.ZERO; c.compareTo(size) < 0; c = c.add(NestBigInteger.ONE)) {
	    	result.add(new UnsignedByte(slice.slice(c, NestBigInteger.ONE), ctx));
		}
	    return result;
	}

	@Override
	public NestValue asValue() {
		return new NestValue(slice, ctx);
	}

	@Override
	public String asString() {
		return new String(slice.allBytes(), ctx.getEncoding());
	}

	@Override
	public Iterator<UnsignedByte> iterator() {
	    return bytes().iterator();
	}
}
