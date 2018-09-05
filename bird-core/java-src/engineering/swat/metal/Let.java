package engineering.swat.metal;

import static io.parsingdata.metal.Shorthand.def;
import static io.parsingdata.metal.Shorthand.len;
import static io.parsingdata.metal.Shorthand.tie;

import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.expression.value.ValueExpression;
import io.parsingdata.metal.token.Token;

public class Let {
	/**
	 * Assign a name to a given single value. This value is added to the {@link ParseGraph}.
	 * If more than one value is given, the result will fail.
	 *
	 * @param name the name of the value
	 * @param value the given value
	 * @return a token parsing given value and assigning a name to it
	 */
	public static Token let(final String name, final ValueExpression value) {
	    return tie(def(name, len(value)), value);
	}
}
