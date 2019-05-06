package engineering.swat.nest.examples.formats;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Tracked;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Since we do not have an until concept in the bird language, this example does not have a corresponding bird file
 */
public class Strings {


    public static class ASCIIZeroTerminated extends UserDefinedToken {
        private final TerminatedToken<UnsignedBytes, UnsignedBytes> contents;
        public final Tracked<String> value;


        private ASCIIZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, Tracked<String> value) {
            this.contents = contents;
            this.value = value;
        }

        public static Optional<ASCIIZeroTerminated> parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            Optional<TerminatedToken<UnsignedBytes, UnsignedBytes>> contents = terminatedWithChar(source, ctx, new byte[] { 0 });
            if (!contents.isPresent()) {
                ctx.fail("ASCIIZeroTerminated.contents missing from {}", source);
                return Optional.empty();
            }
            Tracked<String> value = contents.get().getBody().asString();
            return Optional.of(new ASCIIZeroTerminated(contents.get(), value));
        }


        @Override
        public TrackedByteSlice getTrackedBytes() {
            return contents.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return contents.size();
        }
    }


    public static class UTF16ZeroTerminated extends UserDefinedToken {
        private final TerminatedToken<UnsignedBytes, UnsignedBytes> contents;
        public final Tracked<String> value;

        private UTF16ZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, Tracked<String> value) {
            this.contents = contents;
            this.value = value;
        }

        public static Optional<UTF16ZeroTerminated> parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.UTF_16);
            Optional<TerminatedToken<UnsignedBytes, UnsignedBytes>> contents = terminatedWithChar(source, ctx, new byte[] { 0, 0 });
            if (!contents.isPresent()) {
                ctx.fail("UTF16ZeroTerminated.contents missing from {}", source);
                return Optional.empty();
            }
            Tracked<String> value = contents.get().getBody().asString();
            return Optional.of(new UTF16ZeroTerminated(contents.get(), value));
        }


        @Override
        public TrackedByteSlice getTrackedBytes() {
            return contents.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return contents.size();
        }
    }

    private static Optional<TerminatedToken<UnsignedBytes, UnsignedBytes>> terminatedWithChar(ByteStream source, Context ctx, byte[] terminatorChar) {
        return TerminatedToken.parseUntil(source, ctx, NestBigInteger.ZERO, NestBigInteger.of(terminatorChar.length), null,
                (b,c) -> new ByteStream(b).readUnsigned(b.size(), c),
                (s,c) -> s.readUnsigned(NestBigInteger.of(terminatorChar.length), c)
                        .filter(u -> u.asValue().sameBytes(NestValue.of(terminatorChar)))
        );
    }

}
