package engineering.swat.nest.core.bytes.source;

import java.net.URI;
import java.nio.ByteBuffer;

public class ByteBufferWindow implements ByteWindow {

    private final ByteBuffer source;
    private final int offset;
    private final int limit;
    private final URI origin;

    public ByteBufferWindow(ByteBuffer source, int offset, int limit, URI origin) {
        this.source = source;
        this.offset = offset;
        this.limit = limit;
        this.origin = origin;
    }

    @Override
    public ByteWindow slice(long offset, long size) {
        return new ByteBufferWindow(source, Math.toIntExact( this.offset + offset), Math.toIntExact(this.offset + size), origin);
    }

    @Override
    public ByteOrigin getOrigin(long index) {
        return new ByteOrigin() {
            @Override
            public long getOffset() {
                return offset + index;
            }

            @Override
            public URI getSource() {
                return origin;
            }
        };
    }

    @Override
    public long size() {
        return limit - offset;
    }

    @Override
    public byte get(long index) {
        int pos = Math.toIntExact(offset + index);
        if (pos > limit) {
            throw new IndexOutOfBoundsException();
        }
        return source.get(pos);
    }
}
