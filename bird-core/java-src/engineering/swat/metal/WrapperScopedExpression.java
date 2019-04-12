package engineering.swat.metal;

import static io.parsingdata.metal.Shorthand.con;
import static io.parsingdata.metal.Shorthand.scope;

import io.parsingdata.metal.data.ImmutableList;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.expression.value.ValueExpression;

public class WrapperScopedExpression implements ValueExpression {
	private ValueExpression ve;
	private int scope;

	public WrapperScopedExpression(ValueExpression ve, int scope) {
		super();
		this.ve = digForExpression(ve);
		this.scope = scope + getAccumulatedScope(ve);
	}

	public ValueExpression getValueExpression() {
		return ve;
	}

	public int getScope() {
		return scope;
	}

	public WrapperScopedExpression nestMore() {
		return new WrapperScopedExpression(ve, scope + 2);

	}

	@Override
	public ImmutableList<Value> eval(ParseState parseState, Encoding encoding) {
		ImmutableList<Value> vs = scope(ve, con(scope)).eval(parseState, encoding);
		return vs;
	}

	public ValueExpression digForExpression(ValueExpression ve) {
		if (ve instanceof WrapperScopedExpression)
			return digForExpression(((WrapperScopedExpression) ve).ve);
		else
			return ve;
	}

	public int getAccumulatedScope(ValueExpression ve) {
		if (ve instanceof WrapperScopedExpression)
			return getAccumulatedScope(((WrapperScopedExpression) ve).ve) + ((WrapperScopedExpression) ve).scope;
		else
			return 0;
	}

	public ValueExpression getField(String s) {
		ValueExpression v = digForExpression(ve);
		if (v instanceof StructWrapperExpression) {
			StructWrapperExpression sv = (StructWrapperExpression) v;
			return sv.evalField(s, scope);
		} else {
			throw new RuntimeException("Operation not permitted on non-struct references");
		}
	}

}