package engineering.swat.nest.examples.formats.bird_generated;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class while1$ {
	private while1$(){}
	
   public static final class WhileUnevenParse extends UserDefinedToken {
   	
    	public final TokenList<UnsignedBytes> contents;
   	public final UnsignedBytes terminatedAt;
   	
   	private WhileUnevenParse(TokenList<UnsignedBytes> contents, UnsignedBytes terminatedAt){
   		this.contents = contents;
   		this.terminatedAt = terminatedAt;
   		
   	}
   
   	public static WhileUnevenParse parse(ByteStream source, Context ctx) throws ParseError {
   		
   	
   		TokenList<UnsignedBytes> contents = TokenList.parseWhile(source, ctx,
   			(s, c) -> s.readUnsigned(1, c),
   			it -> (((it).asValue().and(NestValue.of(0x1, 1)).sameBytes(NestValue.of(1, 1))))
   		);
   		UnsignedBytes terminatedAt = source.readUnsigned(1, ctx);
   
   		return new WhileUnevenParse(contents, terminatedAt);
   	}
   
   	@Override
       protected Token[] parsedTokens() {
           return new Token[]{contents, terminatedAt};
       }
   
       @Override
       protected Token[] allTokens() {
           return new Token[]{contents, terminatedAt};
       }
   }
	
}