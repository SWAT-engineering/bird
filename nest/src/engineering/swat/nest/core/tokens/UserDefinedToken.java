package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedBytesView;
import java.util.Arrays;

public abstract class UserDefinedToken extends Token {
	
	protected TrackedBytesView buildTrackedView(Token... tokens) {
		final long[] sizes = Arrays.stream(tokens).mapToLong(Token::size).toArray();
		final long totalSize = Arrays.stream(sizes).sum();
		return new MultipleTokenBytesView<>(Arrays.asList(tokens), sizes, totalSize);
	}

}
