package engineering.swat.formats;

import static io.parsingdata.metal.Shorthand.EMPTY;
import static io.parsingdata.metal.Shorthand.con;
import static io.parsingdata.metal.Shorthand.def;
import static io.parsingdata.metal.Shorthand.eq;
import static io.parsingdata.metal.Shorthand.ref;
import static io.parsingdata.metal.Shorthand.seq;

import engineering.swat.metal.StructWrapperExpression;
import engineering.swat.metal.WrapperScopedExpression;
import io.parsingdata.metal.expression.value.ValueExpression;
import io.parsingdata.metal.token.Token;

public class parameters4b {
	private parameters4b() {
	}

	public static final Token A = seq("A", def("x", con(1)), EMPTY);

	public static final Token B(WrapperScopedExpression a) {

		a = a.nestMore();

		return seq("B", def("y", con(1), eq(a.getField("x"))), EMPTY);
	}

	public static final Token S = seq("S", seq("a", A, EMPTY),
			seq("b", B(new WrapperScopedExpression(new StructWrapperExpression("S.a.A.", new String[] { "x" }, new ValueExpression[] { new WrapperScopedExpression(ref("S.a.A.x"),0) }), 0)), EMPTY));
}