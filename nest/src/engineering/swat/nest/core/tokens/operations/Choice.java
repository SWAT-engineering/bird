package engineering.swat.nest.core.tokens.operations;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.tokens.NestParseFunction;
import engineering.swat.nest.core.tokens.Token;

public class Choice {

	public static Token between(ByteStream source, Context ctx, NestParseFunction<Token> c0) {
		ByteStream backup = source.fork();
		try {
			return c0.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		throw new ParseError("None of the choices parsed");
	}

	private static void revertAndLog(ByteStream source, Context ctx, ByteStream backup, ParseError e) {
		ctx.fail("[Choice] failed: {}", e);
		source.sync(backup); // reset the stream to before the parse
	}

	public static Token between(ByteStream source, Context ctx, NestParseFunction<Token> c0, NestParseFunction<Token> c1) {
		ByteStream backup = source.fork();
		try {
			return c0.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c1.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		throw new ParseError("None of the choices parsed");
	}

	public static Token between(ByteStream source, Context ctx, NestParseFunction<Token> c0, NestParseFunction<Token> c1, NestParseFunction<Token> c2) {
		ByteStream backup = source.fork();
		try {
			return c0.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c1.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c2.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		throw new ParseError("None of the choices parsed");
	}
	public static Token between(ByteStream source, Context ctx, NestParseFunction<Token> c0, NestParseFunction<Token> c1, NestParseFunction<Token> c2, NestParseFunction<Token> c3) {
		ByteStream backup = source.fork();
		try {
			return c0.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c1.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c2.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c3.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		throw new ParseError("None of the choices parsed");
	}

	@SafeVarargs
	public static Token between(ByteStream source, Context ctx,NestParseFunction<Token> c0, NestParseFunction<Token> c1, NestParseFunction<Token> c2, NestParseFunction<Token> c3, NestParseFunction<Token>... rest) {
		ByteStream backup = source.fork();
		try {
			return c0.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c1.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c2.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		try {
			return c3.apply(source, ctx);
		}
		catch (ParseError e) {
			revertAndLog(source, ctx, backup, e);
		}
		for (NestParseFunction<Token> c: rest) {
			try {
				return c.apply(source, ctx);
			}
			catch (ParseError e) {
				revertAndLog(source, ctx, backup, e);
			}
		}
		throw new ParseError("None of the choices parsed");
	}
}
