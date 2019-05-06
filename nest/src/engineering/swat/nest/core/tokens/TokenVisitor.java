package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;

public interface TokenVisitor<T> {
    T visitUserDefinedToken(UserDefinedToken value);
    T visitOptionalToken(OptionalToken<? extends Token> value);
    T visitTerminatedToken(TerminatedToken<? extends Token, ? extends Token> value);
    T visitTokenList(TokenList<? extends Token> value);
    T visitUnsignedBytes(UnsignedBytes value);
}
