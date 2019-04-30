package engineering.swat.nest.core.bytes.source;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.net.URI;
import java.nio.ByteBuffer;

public class ByteBufferSlice implements TrackedByteSlice {

    private final ByteBuffer source;
    private final int offset;
    private final int limit;
    private final URI origin;

    public ByteBufferSlice(ByteBuffer source, int offset, int limit, URI origin) {
        this.source = source;
        this.offset = offset;
        this.limit = limit;
        this.origin = origin;
    }

    @Override
    public TrackedByteSlice slice(NestBigInteger offset, NestBigInteger size) {
        assert offset.intValueExact() >= 0;
        int newOffset = Math.addExact(this.offset, offset.intValueExact());
        int newLimit = Math.addExact(newOffset, size.intValueExact());
        if (newLimit > limit) {
            throw new ParseError("Invalid slice");
        }
        return new ByteBufferSlice(source, newOffset, newLimit, origin);
    }

    @Override
    public ByteOrigin getOrigin(NestBigInteger index) {
        return new ByteOrigin() {
            @Override
            public NestBigInteger getOffset() {
                return index.add(NestBigInteger.of(offset));
            }

            @Override
            public URI getSource() {
                return origin;
            }
        };
    }

    @Override
    public NestBigInteger size() {
        return NestBigInteger.of(limit - offset);
    }

    @Override
    public byte get(NestBigInteger index) {
        int pos = Math.addExact(offset, index.intValueExact());
        if (pos > limit) {
            throw new IndexOutOfBoundsException();
        }
        return source.get(pos);
    }
}
