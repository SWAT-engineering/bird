package engineering.swat.nest.examples.png;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.nontokens.NestString;
import engineering.swat.nest.core.nontokens.NonTokenBytes;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class Signature extends UserDefinedToken {
	public UnsignedBytes $dummy1;
	public UnsignedBytes $dummy2;
	public UnsignedBytes $dummy3;
	
	private Signature() {}
	
	public static Signature parse(ByteStream source, Context ctx) throws ParseError {
		Signature result = new Signature();

		result.$dummy1 = source.readUnsigned(1, ctx);
		if (!(result.$dummy1.asInteger().getValue() == 0x89)) {
			throw new ParseError("Signature.$dummy1", result.$dummy1);
		}

		result.$dummy2 = source.readUnsigned(3, ctx);
		if (!(result.$dummy2.asString().equals(new NestString("PNG")))) {
			throw new ParseError("Signature.$dummy2", result.$dummy2);
		}

		result.$dummy3 = source.readUnsigned(4, ctx);
		if (!(result.$dummy3.sameBytes(NonTokenBytes.of(0x0d, 0x0a, 0x1a, 0x0a)))) {
			throw new ParseError("Signature.$dummy3", result.$dummy3);
		}
		return result;
	}
	
	@Override
	public TrackedBytesView getTrackedBytes() {
		return buildTrackedView($dummy1, $dummy2, $dummy3);
	}
	
	@Override
	public long size() {
		return $dummy1.size() + $dummy2.size() + $dummy3.size();
	}
	

}
