import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import engineering.swat.bird.generated.images.Image$.__$Image;
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
        final var fileName = "resources\\test\\truecolor.png";
        System.out.println("Parsing file " + fileName);
        
        try {
            var f = new FileInputStream(fileName);
            var bs = new ByteStream(ByteSliceBuilder.convert(f, new File(fileName).toURI()));
            
            var image = __$Image.parse(bs, Context.DEFAULT);
    
            var numBytes = image.accept(new BottomUpTokenVisitor<>(
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
            System.out.println("Parse tree has size " + image.size());
            System.out.println("Visited " + numBytes + " bytes");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
