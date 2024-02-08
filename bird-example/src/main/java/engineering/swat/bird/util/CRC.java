package engineering.swat.bird.util; 

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.util.zip.CRC32;
import engineering.swat.nest.core.tokens.Token;

public class CRC {
    
	public static NestBigInteger crc32(Token data) {
		CRC32 hasher = new CRC32();
		TrackedByteSlice bytes = data.getTrackedBytes();
		NestBigInteger size = bytes.size();
		for (NestBigInteger i = NestBigInteger.ZERO; i.compareTo(size)  < 0; i = i.add(NestBigInteger.ONE)) {
			hasher.update(bytes.getUnsigned(i));
		}
		return NestBigInteger.of(hasher.getValue());
	}

}
