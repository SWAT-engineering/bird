package engineering.swat.metal;

import java.util.SortedMap;
import java.util.TreeMap;

import io.parsingdata.metal.data.ImmutableList;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.expression.value.ValueExpression;

public class StructWrapperExpression implements ValueExpression {
	private String base;
	private SortedMap<String, ValueExpression> fields;
	

	public StructWrapperExpression(String base, String[] names, ValueExpression[] ves) {
		super();
		this.base = base;
		this.fields = new TreeMap<String, ValueExpression>();
		for (int i = 0; i < names.length; i ++) {
			fields.put(names[i], ves[i]);
		}
	}


	@Override
	public ImmutableList<Value> eval(ParseState parseState, Encoding encoding) {
		throw new UnsupportedOperationException();
	}


	public ValueExpression evalField(String s, int scope) {
		return new WrapperScopedExpression(fields.get(s), scope);
	}

}