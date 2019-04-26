package engineering.swat.bird.core.tokens;

import java.util.Arrays;
import engineering.swat.bird.core.bytes.TrackedBytesView;
import engineering.swat.bird.core.bytes.source.TrackedByte;

public abstract class UserDefinedToken extends Token {
	
	protected TrackedBytesView buildTrackedView(Token... tokens) {
		final long[] sizes = Arrays.stream(tokens).mapToLong(Token::size).toArray();
		final long totalSize = Arrays.stream(sizes).sum();
		return new MultipleTokenBytesView<>(Arrays.asList(tokens), sizes, totalSize);
	}

}
