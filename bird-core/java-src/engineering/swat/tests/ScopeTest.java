package engineering.swat.tests;

import static engineering.swat.metal.Let.let;
import static io.parsingdata.metal.Shorthand.EMPTY;
import static io.parsingdata.metal.Shorthand.add;
import static io.parsingdata.metal.Shorthand.con;
import static io.parsingdata.metal.Shorthand.def;
import static io.parsingdata.metal.Shorthand.eq;
import static io.parsingdata.metal.Shorthand.first;
import static io.parsingdata.metal.Shorthand.ref;
import static io.parsingdata.metal.Shorthand.scope;
import static io.parsingdata.metal.Shorthand.seq;
import static io.parsingdata.metal.Shorthand.tie;
import static io.parsingdata.metal.util.EncodingFactory.enc;
import static io.parsingdata.metal.util.EnvironmentFactory.env;
import static io.parsingdata.metal.util.ParseStateFactory.stream;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import engineering.swat.metal.StructWrapperExpression;
import engineering.swat.metal.util.ParseGraphSerializer;
import io.parsingdata.metal.data.ParseState;
import io.parsingdata.metal.expression.value.ValueExpression;
import io.parsingdata.metal.token.Token;

@FunctionalInterface
interface TriFunction<X,Y,Z,R>{
	R apply(X x, Y y, Z z);
}

class Pair<T, U> {
	
	private T first;
	private U second;

	public Pair(T first, U second) {
		this.first = first;
		this.second = second;
		
	}

	public T getFirst() {
		return first;
	}

	public U getSecond() {
		return second;
	}
	
}

public class ScopeTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void parseGraphWithAdditionSameType() {
		final Token A = seq("A", def("x", con(1), eq(con(1))), EMPTY);
		final Token B = seq("B", def("x", con(1), eq(con(0))), EMPTY);
		final Token C = seq("C", seq("a", A, EMPTY), seq("b", B, EMPTY), tie(def("derived", con(1)),
				add(first(scope(ref("C.a.A.x"), con(2))), first(scope(ref("C.a.A.x"), con(2))))));
		final Optional<ParseState> result = C.parse(env(stream(1, 0), enc()));
		assertTrue(result.isPresent());
		System.out.println("parseGraphWithAdditionSameType");
		System.out.println("-------------------------------");
		ParseGraphSerializer.println(result.get().order);
		System.out.println();

	}
	
	@Test
	public void structArgument1() {
		
		final Token A = seq("A", def("x", con(1)), EMPTY);

		final BiFunction<StructWrapperExpression, ValueExpression, Token> B = (StructWrapperExpression a, ValueExpression y) -> {
			return seq("B", def("y", con(1), eq(a.getField("x"))), EMPTY);
		};

		final Token S = seq("S", seq("a", A, EMPTY), let("x", ref("a.A.x")),
				seq("b", B.apply(new StructWrapperExpression(new String[] { "x"}, new ValueExpression[] {ref("S.a.A.x")}), ref("S.x")), EMPTY), EMPTY); 
		
		final Optional<ParseState> result = S.parse(env(stream(0, 0), enc()));
		assertTrue(result.isPresent());
		System.out.println("parseGraphWithAdditionSameType");
		System.out.println("-------------------------------");
		ParseGraphSerializer.println(result.get().order);
		System.out.println();
	}
	
	@Test
	/**
	 * 
	 * struct A {
	 * 		u8 x
	 * }
	 * 
	 * struct B(A a, int y) {
	 * 		u8 x ?(== a.x)
	 * }
	 * 
	 * struct S {
	 * 		A a
	 * 		int x = a.x
	 * 		B b(a, x)
	 * }
	 * 
	 */
	
	public void structArgument2b() {
		
		final Function<String, Token> A = base -> seq("A", def("x", con(1)), EMPTY);
		final Function<String, StructWrapperExpression> A_ = (base) ->
			new StructWrapperExpression(new String[] { "x" }, new ValueExpression[] {ref(base + "A.x")}); 

		final TriFunction<String, StructWrapperExpression, ValueExpression, Token> B = (String base, StructWrapperExpression a, ValueExpression y) -> 
			seq("B", def("x", con(1), eq(a.getField("x"))), EMPTY);
			;
			
		final Function<String, StructWrapperExpression> B_ = (base) ->
			new StructWrapperExpression(new String[] { "x" }, new ValueExpression[] {
					ref(base + "B.x") 
			} );
		
		final Function<String, StructWrapperExpression> S_ = (base) ->
			new StructWrapperExpression(new String[] {"a", "x", "b"}, new ValueExpression[] {
					A_.apply(base + "S.a."), ref(base + "S.x"), B_.apply(base + "S.b.")});
			
		final Function<String, Token> S = base -> seq("S", seq("a", A.apply(base + "S.a."), EMPTY), let("x", A_.apply(base+"S.a.").getField("x")),
				seq("b", B.apply(base + "S.b.", A_.apply(base+"S.a."), ref(base + "S.x")), EMPTY), EMPTY);
		
		
		final Optional<ParseState> result = S.apply("").parse(env(stream(0, 0), enc()));
		assertTrue(result.isPresent());
		
		System.out.println("parseGraphWithAdditionSameType");
		System.out.println("-------------------------------");
		ParseGraphSerializer.println(result.get().order);
		System.out.println();
	}
	public void structArgument2a() {
		
		final Function<String, Token> A = base -> seq("A", def("x", con(1)), EMPTY);
		final Function<String, StructWrapperExpression> A_ = (base) ->
			new StructWrapperExpression(new String[] { "x" }, new ValueExpression[] {ref(base + "A.x")}); 

		final TriFunction<String, StructWrapperExpression, ValueExpression, Token> B = (String base, StructWrapperExpression a, ValueExpression y) -> 
			seq("B", def("x", con(1), eq(a.getField("x"))), EMPTY);
			;
			
		final TriFunction<String, StructWrapperExpression, ValueExpression, StructWrapperExpression> B_ = (base, a, y) ->
			new StructWrapperExpression(new String[] { "a", "y", "x" }, new ValueExpression[] {
					a, y, ref(base + "B.x") 
			} );
		
		final Function<String, StructWrapperExpression> S_ = (base) ->
			new StructWrapperExpression(new String[] {"a", "x", "b"}, new ValueExpression[] {
					A_.apply(base + "S.a."), ref(base + "S.x"), B_.apply(base + "S.b.", A_.apply(base+"S.a."), ref(base + "S.x"))});
			
		final Function<String, Token> S = base -> seq("S", seq("a", A.apply(base + "S.a."), EMPTY), let("x", A_.apply(base+"S.a.").getField("x")),
				seq("b", B.apply(base + "S.b.", A_.apply(base+"S.a."), ref(base + "S.x")), EMPTY), EMPTY);
		
		
		final Optional<ParseState> result = S.apply("").parse(env(stream(0, 0), enc()));
		assertTrue(result.isPresent());
		
		System.out.println("parseGraphWithAdditionSameType");
		System.out.println("-------------------------------");
		ParseGraphSerializer.println(result.get().order);
		System.out.println();
	}

	
	@Test
	public void structArgument3() {
		
		final Function<String, Token> A = base -> seq("A", def("x", con(1)), EMPTY);
		final Function<String, StructWrapperExpression> A_ = (base) ->
			new StructWrapperExpression(new String[] { "x" }, new ValueExpression[] {ref(base + "A.x")}); 

		final TriFunction<String, StructWrapperExpression, ValueExpression, Token> B = (String base, StructWrapperExpression a, ValueExpression y) -> 
			seq("B", def("y", con(1), eq(a.getField("x"))), EMPTY);
			;
			
		final Function<String, StructWrapperExpression> B_ = (base) ->
			new StructWrapperExpression(new String[] {}, new ValueExpression[] {} );
		
		final Function<String, StructWrapperExpression> S_ = (base) ->
			new StructWrapperExpression(new String[] {"a", "x", "b"}, new ValueExpression[] {
					A_.apply(base + "S."), ref(base + ".S.x"), B_.apply(base + "S.")});
			
		final Function<String, Token> S = base -> seq("S", seq("a", A.apply(base + "S.a."), EMPTY), let("x", ref(base + "S.a.A.x")),
				seq("b", B.apply(base + "S.", A_.apply(base+"S.a."), ref(base + "S.x")), EMPTY), EMPTY);
		
		
		final Function<String, StructWrapperExpression> Z_ = (base) ->
			new StructWrapperExpression(new String[] {"s", "x"}, new ValueExpression[] {
				S_.apply(base + "Z."), ref(base + "Z.x")});
		
		final Function<String, Token> Z = base -> seq("Z", seq("s", S.apply(base + "Z.s."), EMPTY),EMPTY);
	
		final Optional<ParseState> result = Z.apply("").parse(env(stream(0, 0), enc()));
		
		assertTrue(result.isPresent());
		
		System.out.println("parseGraphWithAdditionSameType");
		System.out.println("-------------------------------");
		ParseGraphSerializer.println(result.get().order);
		System.out.println();
	}


}