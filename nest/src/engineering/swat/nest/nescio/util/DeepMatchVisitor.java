package engineering.swat.nest.nescio.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import engineering.swat.nest.core.tokens.BottomUpTokenVisitor;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;

public class DeepMatchVisitor<T> extends BottomUpTokenVisitor<List<T>> {
	
	static class EntryVisitor<T> implements TokenVisitor<List<T>> {
		private Class<T> clazz;

		EntryVisitor(Class<T> clazz) {
			this.clazz = clazz;
		}
		
		@Override
		public List<T> visitUserDefinedToken(UserDefinedToken value) {
			if (clazz.isInstance(value))
				return Arrays.asList((T) value);
			else
				return Arrays.asList();
		}

		@Override
		public List<T> visitOptionalToken(OptionalToken<? extends Token> value) {
			return Arrays.asList();
		}

		@Override
		public List<T> visitTerminatedToken(TerminatedToken<? extends Token, ? extends Token> value) {
			return Arrays.asList();
		}

		@Override
		public List<T> visitTokenList(TokenList<? extends Token> value) {
			return Arrays.asList();
		}

		@Override
		public List<T> visitUnsignedBytes(UnsignedBytes value) {
			return Arrays.asList();
		}		
	}
	
	public DeepMatchVisitor(Class<T> clazz) {
		super(new EntryVisitor<T>(clazz), (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()));
	}
	
}
