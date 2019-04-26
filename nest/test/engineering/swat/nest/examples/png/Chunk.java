package engineering.swat.nest.examples.png;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.BytesView;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class Chunk extends UserDefinedToken {
	public UnsignedBytes length;
	public UnsignedBytes type;
	public UnsignedBytes data;
	public UnsignedBytes crc;
	
	private Chunk() {}

	public static Chunk parse(ByteStream source, Context ctx)  {
		Chunk result = new Chunk();
		result.length = source.readUnsigned(4, ctx);
		result.type = source.readUnsigned(4, ctx);
		if (!(!result.type.asString().equals("IEND"))) {
			throw new ParseError("Chunk.type");
		}
		result.data = source.readUnsigned(Math.toIntExact(result.length.asInteger().getValue()), ctx);
		result.crc = source.readUnsigned(4, ctx);
		if (!(UserDefinedPNG.crc32(TokenList.of(result.type, result.data)) == result.crc.asInteger().getValue())) {
			throw new ParseError("Chunk.crc");
		}
		return result;
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return buildTrackedView(length, type, data, crc);
	}
	
	@Override
	public long size() {
		return length.size() + type.size() + data.size() + crc.size();
	}

}
