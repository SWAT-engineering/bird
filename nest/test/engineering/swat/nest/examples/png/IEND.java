package engineering.swat.nest.examples.png;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.nontokens.NestString;
import engineering.swat.nest.core.nontokens.NonTokenBytes;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class IEND extends UserDefinedToken {
	public UnsignedBytes length;
	public UnsignedBytes type;
	public UnsignedBytes crc;
	
	private IEND() {}

	public static IEND parse(ByteStream source, Context ctx) {
		IEND result = new IEND();
		result.length = source.readUnsigned(4, ctx);
		if (!(result.length.asInteger().getValue() == 0)) {
			throw new ParseError("IED.length", result.length);
		}
		
		result.type = source.readUnsigned(4, ctx);
		if (!result.type.asString().equals(new NestString("IEND"))) {
			throw new ParseError("IED.type", result.type);
		}
		
		result.crc = source.readUnsigned(4, ctx);
		if (!result.crc.sameBytes(NonTokenBytes.of(0xae, 0x42, 0x60, 0x82))) {
			throw new ParseError("IED.crc", result.crc);
		}
		return result;
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return buildTrackedView(length, type, crc);
	}

	@Override
	public long size() {
		return length.size() + type.size() + crc.size();
	}

}
