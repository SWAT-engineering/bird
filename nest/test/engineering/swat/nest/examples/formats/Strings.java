package engineering.swat.nest.examples.formats;

import engineering.swat.nest.core.bytes.ByteSlice;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.TerminatedToken;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class Strings {


    public static class ASCIIZeroTerminated extends UserDefinedToken {
        private final TerminatedToken<UnsignedBytes, UnsignedBytes> contents;
        public final String value;


        private ASCIIZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, String value) {
            this.contents = contents;
            this.value = value;
        }

        public static ASCIIZeroTerminated parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            TerminatedToken<UnsignedBytes, UnsignedBytes> contents = terminatedWithChar(source, ctx, new byte[] { 0 });
            String value = contents.getBody().asString();
            return new ASCIIZeroTerminated(contents, value);
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
        public final String value;

        private UTF16ZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, String value) {
            this.contents = contents;
            this.value = value;
        }

        public static UTF16ZeroTerminated parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.UTF_16);
            TerminatedToken<UnsignedBytes, UnsignedBytes> contents = terminatedWithChar(source, ctx, new byte[] { 0 , 0});
            String value = contents.getBody().asString();
            return new UTF16ZeroTerminated(contents, value);

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

    private static TerminatedToken<UnsignedBytes, UnsignedBytes> terminatedWithChar(ByteStream source, Context ctx, byte[] terminatorChar) {
        return TerminatedToken.parseUntil(source, ctx, NestBigInteger.ZERO, NestBigInteger.of(terminatorChar.length), null,
                (b,c) -> new ByteStream(b).readUnsigned(b.size(), c),
                (s,c) -> Optional.of(s.readUnsigned(NestBigInteger.of(terminatorChar.length), c))
                        .filter(u -> u.asValue().sameBytes(NestValue.of(terminatorChar)))
        );
    }

}
