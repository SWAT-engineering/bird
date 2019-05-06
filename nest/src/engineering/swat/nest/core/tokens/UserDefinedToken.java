package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.tokens.primitive.MultipleTokenByteSlice;
import java.util.Arrays;

public abstract class UserDefinedToken extends Token {
	
	protected TrackedByteSlice buildTrackedView(Token... tokens) {
		return MultipleTokenByteSlice.buildByteView(Arrays.asList(tokens));
	}

}
