package engineering.swat.nest.constructed;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.nontokens.NestInteger;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

class A extends UserDefinedToken {
    public NestInteger virtualField;
    public UnsignedBytes x;
    private A() {}
    
    public static A parse(ByteStream source, Context ctx) {
        A result = new A();
        result.x = source.readUnsigned(1, ctx);
        if (!(result.x.asInteger().getValue() == 1)) {
            throw new ParseError("A.x", result.x);
        }
        result.virtualField = new NestInteger(2 * result.x.asInteger().getValue());
        return result;
    }

    @Override
    public TrackedBytesView getTrackedBytes() {
        return buildTrackedView(x);
    }

    @Override
    public long size() {
        return x.size();
    }
}