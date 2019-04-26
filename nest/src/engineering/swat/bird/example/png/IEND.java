package engineering.swat.bird.example.png;

import engineering.swat.bird.core.ParseError;
import engineering.swat.bird.core.bytes.ByteStream;
import engineering.swat.bird.core.bytes.Context;
import engineering.swat.bird.core.bytes.TrackedBytesView;
import engineering.swat.bird.core.nontokens.BirdString;
import engineering.swat.bird.core.nontokens.NonTokenBytes;
import engineering.swat.bird.core.tokens.UnsignedBytes;
import engineering.swat.bird.core.tokens.UserDefinedToken;

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
		if (!result.type.asString().equals(new BirdString("IEND"))) {
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
