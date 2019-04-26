package engineering.swat.nest.example.png;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedBytesView;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UserDefinedToken;

public class PNG extends UserDefinedToken {
	public Signature $dummy1;
	public TokenList<Chunk> chunks;
	public IEND $dummy2;

	private PNG() {}
	
	public static PNG parse(ByteStream source, Context ctx) {
		ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
		ctx = ctx.setByteOrder(ByteOrder.BIG_ENDIAN);
		PNG result = new PNG();
		result.$dummy1 = Signature.parse(source, ctx);
		result.chunks = TokenList.untilParseFailure(source, ctx, (s, c) -> Chunk.parse(s, c));
		result.$dummy2 = IEND.parse(source, ctx);
		return result;
	}

	@Override
	public TrackedBytesView getTrackedBytes() {
		return buildTrackedView($dummy1, chunks, $dummy2);
	}

	@Override
	public long size() {
		return $dummy1.size() + chunks.size() + $dummy2.size();
	}
}


