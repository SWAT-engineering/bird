package engineering.swat.bird.core;

import engineering.swat.bird.core.tokens.Token;

public class ParseError extends RuntimeException {

	public ParseError(String msg) {
		super("Parsing failed at: " + msg);
	}

	public ParseError(String msg, Token failed) {
		super("Parsing failed at: " + msg + " parsed: " + failed);

	}

}
