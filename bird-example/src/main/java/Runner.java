import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import engineering.swat.bird.generated.images.JPEG$.__$Format;
import engineering.swat.bird.generated.images.PNG$.__$PNG;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.BottomUpTokenVisitor;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;

public class Runner {

    public static void main(String[] args) {
        System.out.println();

        final String fileName = "resources\\test\\truecolor.png";
        FileInputStream f = null;
        ByteStream bs = null;
        try {
            f = new FileInputStream(fileName);
            bs = new ByteStream(ByteSliceBuilder.convert(f, new File(fileName).toURI()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        __$PNG png = parsePNG(fileName, bs);
        __$Format jpeg = parseJPEG(fileName, bs);
        
        UserDefinedToken parseTree = null;
        if (png != null) {
            parseTree = png;
        } else if (jpeg != null) {
            parseTree = jpeg;
        }

        if (parseTree != null) {
            System.out.println();
            var numBytes = parseTree.accept(new BottomUpTokenVisitor<>(
                    new TokenVisitor<NestBigInteger>() {
                        @Override
                        public NestBigInteger visitUserDefinedToken(UserDefinedToken value) {
                            if (value.size().greaterThan(NestBigInteger.TWO)) {
                                System.out.println("Found token " + value.getClass().getSimpleName().substring(3)
                                        + " of size " + value.size());
                            }
                            return NestBigInteger.ZERO;
                        }

                        @Override
                        public NestBigInteger visitOptionalToken(OptionalToken<? extends Token> value) {
                            return NestBigInteger.ZERO;
                        }

                        @Override
                        public NestBigInteger visitTerminatedToken(
                                TerminatedToken<? extends Token, ? extends Token> value) {
                            return NestBigInteger.ZERO;
                        }

                        @Override
                        public NestBigInteger visitTokenList(TokenList<? extends Token> value) {
                            return NestBigInteger.ZERO;
                        }

                        @Override
                        public NestBigInteger visitUnsignedBytes(UnsignedBytes value) {
                            return value.size();
                        }
                    }, NestBigInteger::add));
            System.out.println("----------------------------------");
            System.out.println("Parse tree has size " + parseTree.size());
            System.out.println("Visited " + numBytes + " bytes");
        }
    }

    private static __$PNG parsePNG(String fileName, ByteStream bs) {
        try {
            __$PNG parseTree = __$PNG.parse(bs, Context.DEFAULT);
            System.out.println("Successful parse as PNG");
            return parseTree;
        } catch (ParseError e) {
            System.out.println("Could not parse as PNG");
            return null;
        }
    }

    private static __$Format parseJPEG(String fileName, ByteStream bs) {
        try {
            __$Format parseTree = __$Format.parse(bs, Context.DEFAULT);
            System.out.println("Successful parse as JPEG");
            return parseTree;
        } catch (ParseError e) {
            System.out.println("Could not parse as JPEG");
            return null;
        }
    }
}
