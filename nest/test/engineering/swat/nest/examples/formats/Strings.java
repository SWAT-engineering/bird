package engineering.swat.nest.examples.formats;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.nontokens.Tracked;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.charset.StandardCharsets;

public class Strings {


    public static class ASCIIZeroTerminated extends UserDefinedToken {

        private final TerminatedToken<UnsignedBytes, UnsignedBytes> contents;
        public final Tracked<String> value;


        private ASCIIZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, Tracked<String> value) {
            this.contents = contents;
            this.value = value;
        }

        public static ASCIIZeroTerminated parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.US_ASCII);
            TerminatedToken<UnsignedBytes, UnsignedBytes> contents = terminatedWithChar(source, ctx, new byte[]{0});
            Tracked<String> value = contents.getBody().asString();
            return new ASCIIZeroTerminated(contents, value);
        }


        @Override
        protected Token[] parsedTokens() {
            return new Token[]{contents};
        }
    }


    public static class UTF16ZeroTerminated extends UserDefinedToken {

        private final TerminatedToken<UnsignedBytes, UnsignedBytes> contents;
        public final Tracked<String> value;

        private UTF16ZeroTerminated(TerminatedToken<UnsignedBytes, UnsignedBytes> contents, Tracked<String> value) {
            this.contents = contents;
            this.value = value;
        }

        public static UTF16ZeroTerminated parse(ByteStream source, Context ctx) {
            ctx = ctx.setEncoding(StandardCharsets.UTF_16);
            TerminatedToken<UnsignedBytes, UnsignedBytes> contents = terminatedWithChar(source, ctx, new byte[]{0, 0});
            Tracked<String> value = contents.getBody().asString();
            return new UTF16ZeroTerminated(contents, value);

        }


        @Override
        protected Token[] parsedTokens() {
            return new Token[]{contents};
        }
    }

    private static TerminatedToken<UnsignedBytes, UnsignedBytes> terminatedWithChar(ByteStream source, Context ctx, byte[] terminatorChar) {
        return TerminatedToken.parseUntil(source, ctx, NestBigInteger.ZERO, NestBigInteger.of(terminatorChar.length), null,
                (b,c) -> new ByteStream(b).readUnsigned(b.size(), c),
                (s,c) -> {
                    UnsignedBytes result = s.readUnsigned(NestBigInteger.of(terminatorChar.length), c);
                    if (!result.sameBytes(NestValue.of(terminatorChar))) {
                        throw new ParseError("Terminator", result);
                    }
                    return result;
                }
        );
    }

}
