package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class A extends UserDefinedToken {
    public final NestInteger virtualField;
    public final UnsignedBytes x;
    private A(NestInteger virtualField, UnsignedBytes x) {
        this.virtualField = virtualField;
        this.x = x;
    }
    
    public static A parse(ByteStream source, Context ctx) {
        UnsignedBytes x = source.readUnsigned(1, ctx);
        if (!(x.asInteger().getValue() == 1)) {
            throw new ParseError("A.x", x);
        }
        NestInteger virtualField = new NestInteger(2 * x.asInteger().getValue());
        return new A(virtualField, x);
    }

    @Override
    public TrackedByteSlice getTrackedBytes() {
        return buildTrackedView(x);
    }

    @Override
    public long size() {
        return x.size();
    }
}