package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestInteger;
import java.nio.ByteBuffer;

public class UnsignedBytes extends PrimitiveToken {

	private final TrackedByteSlice slice;

	public UnsignedBytes(TrackedByteSlice slice, Context ctx) {
		super(ctx);
		this.slice = slice;
	}

	public boolean sameBytes(NestValue other) {
		ByteSlice otherBytes = other.getBytes();
		if (otherBytes.size() != slice.size()) {
			return false;
		}
		for (int i=0; i < slice.size(); i++) {
			if (otherBytes.get(i) != slice.get(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TrackedByteSlice getTrackedBytes() {
		return slice;
	}
	
	@Override
	public long size() {
		return slice.size();
	}
	
	@Override
	public String toString() {
		return "Unsiged bytes:" + slice;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NestValue) {
			return sameBytes((NestValue) obj);
		}
		return false;
	}

	@Override
	public NestInteger asInteger() {
		long size = slice.size();
		if (size == 1) {
			return new NestInteger( slice.get(0) & 0xFF);
		}
		ByteBuffer buf = ByteBuffer.allocate(Math.toIntExact(size));
		for (int i = 0; i < size; i++) {
			buf.put(slice.get(i));
		}
		buf.flip();
		buf.order(ctx.getByteOrder());
		long result;
		switch (Math.toIntExact(size)) {
			case 2: result = buf.getShort() & 0xFFFFL; break;
			case 4: result = buf.getInt() & 0xFFFF_FFFFL; break;
			case 8: result = buf.getLong(); break; // TODO: this is actually not an unsigned long
			default: throw new RuntimeException("Non standard int size (" + size + ") not implemented");
		}
		return new NestInteger(result);
	}
}
