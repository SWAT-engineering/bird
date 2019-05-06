package engineering.swat.nest.core.tokens.primitive;

import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Origin;
import engineering.swat.nest.core.nontokens.Tracked;
import engineering.swat.nest.core.tokens.PrimitiveToken;
import java.util.ArrayList;
import java.util.List;

public class UnsignedBytes extends PrimitiveToken  {

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


	public List<Integer> bytes() {
		NestBigInteger size = slice.size();
	    List<Integer> result = new ArrayList<>(size.intValueExact());
	    for (NestBigInteger c = NestBigInteger.ZERO; c.compareTo(size) < 0; c = c.add(NestBigInteger.ONE)) {
	    	result.add(slice.getUnsigned(c));
		}
	    return result;
	}

	@Override
	public NestValue asValue() {
		return new NestValue(slice, ctx);
	}

	@Override
	public Tracked<String> asString() {
		return new Tracked<>(Origin.of(slice), new String(slice.allBytes(), ctx.getEncoding()));
	}

}
