package engineering.swat.nest.core.tokens;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.bytes.source.ByteOrigin;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TerminatedToken<E extends Token, T extends Token> extends UserDefinedToken {

    private final E body;
    private final T terminator;

    private TerminatedToken(E body, T terminator) {
        this.body = body;
        this.terminator = terminator;
    }

    public E getBody() {
        return body;
    }

    public T getTerminator() {
        return terminator;
    }

    /**
     * It starts parsing from the beginning of the current position in the stream, and checks for the terminator at the intervals defined by and initialCheck stepSize.
     * If maxLength is not null, it will fail to parse if it hasn't find a terminator by that point.
     * After the terminator is found, all the preceding bytes are parsed by the entryParser.
     *
     * @param source bytes stream to start parsing from
     * @param ctx current context
     * @param initialCheck first relative offset to try and parse the terminator
     * @param stepSize at which interval to try and parse the terminator
     * @param maxLength after how many bytes to stop looking for a terminator, if null signals it continues till the end of the source
     * @param entryParser parser function for the entry (note that it receives a ByteSlice, so that the size is known)
     * @param terminatorParser parser function for the terminator
     * @param <E> type of the entry
     * @param <T> type of the terminator
     * @return a terminated token instance
     */
    public static <E extends Token, T extends Token> TerminatedToken<E,T> parseUntil(ByteStream source, Context ctx,
            NestBigInteger initialCheck, NestBigInteger stepSize, @Nullable NestBigInteger maxLength,
            BiFunction<TrackedByteSlice, Context, E> entryParser,
            BiFunction<ByteStream, Context, T> terminatorParser) {
        if (stepSize.isNegative() || stepSize.isZero()) {
            throw new IllegalArgumentException("stepSize should be positive");
        }
        if (maxLength != null && maxLength.isZero()) {
            maxLength = null;
        }
        if (maxLength != null && maxLength.isNegative()) {
            throw new IllegalArgumentException("maxLength should be positive, use null for unbounded");
        }

        if (initialCheck.isNegative()) {
            throw new IllegalArgumentException("intitialCheck should not be negative");
        }

        NestBigInteger terminatorCursor = source.getOffset().add(initialCheck);
        NestBigInteger terminatorMax = maxLength == null ? null :  source.getOffset().add(maxLength);
        while (terminatorMax == null || terminatorCursor.compareTo(terminatorMax) <= 0) {
            ByteStream terminatorStream = source.fork(terminatorCursor);
            try {
                T terminator = terminatorParser.apply(terminatorStream, ctx);
                // we have found the terminator, let's now parse E
                NestBigInteger entrySize = terminatorCursor.subtract(source.getOffset());
                E entry = entryParser.apply(source.readSlice(entrySize), ctx);
                // now forward the source stream to after the terminator
                source.sync(terminatorStream);
                return new TerminatedToken<>(entry, terminator);
            } catch (ParseError e) {
                terminatorCursor = terminatorCursor.add(stepSize);
            }
        }
        throw new ParseError("Max reached before we could parse the terminator (" + terminatorCursor + "/" + terminatorMax + ")");
    }


    @Override
    public TrackedByteSlice getTrackedBytes() {
        return MultipleTokenByteSlice.buildByteView(Arrays.asList(body, terminator));
    }

    @Override
    public NestBigInteger size() {
        return body.size().add(terminator.size());
    }
}
