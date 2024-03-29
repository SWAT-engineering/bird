package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class BottomUpTokenVisitor<T> implements TokenVisitor<T> {

    private final TokenVisitor<T> entryVisitor;
    private final BinaryOperator<T> mergeChildResults;

    public BottomUpTokenVisitor(TokenVisitor<T> entryVisitor, BinaryOperator<T> mergeChildResults) {
        this.entryVisitor = entryVisitor;
        this.mergeChildResults = mergeChildResults;

    }

    @Override
    public T visitUserDefinedToken(UserDefinedToken value) {
        Optional<T> childResult = Arrays.stream(value.allTokens()).map(t -> t.accept(this)).reduce(mergeChildResults);
        T result = entryVisitor.visitUserDefinedToken(value);
        if (childResult.isPresent()) {
            return mergeChildResults.apply(childResult.get(), result);
        }
        return result;
    }

    @Override
    public T visitOptionalToken(OptionalToken<? extends Token> value) {
        Optional<T> nestedResult = value.getToken().map(t -> t.accept(this));
        T result = entryVisitor.visitOptionalToken(value);
        if (nestedResult.isPresent()) {
            return mergeChildResults.apply(nestedResult.get(), result);
        }
        return result;
    }

    @Override
    public T visitTerminatedToken(TerminatedToken<? extends Token, ? extends Token> value) {
        T nestedBody = value.getBody().accept(this);
        T nestedTerminator = value.getTerminator().accept(this);
        T nestedResult = mergeChildResults.apply(nestedBody, nestedTerminator);
        return mergeChildResults.apply(nestedResult, entryVisitor.visitTerminatedToken(value));
    }

    @Override
    public T visitTokenList(TokenList<? extends Token> value) {
        T result = null;
        for (int i = 0; i < value.length(); i++) {
            T current = value.get(i).accept(this);
            result = result == null ? current : mergeChildResults.apply(result, current);
        }
        T token = entryVisitor.visitTokenList(value);
        if (result != null) {
            return mergeChildResults.apply(result, token);
        }
        return token;
    }

    @Override
    public T visitUnsignedBytes(UnsignedBytes value) {
        return entryVisitor.visitUnsignedBytes(value);
    }
}
