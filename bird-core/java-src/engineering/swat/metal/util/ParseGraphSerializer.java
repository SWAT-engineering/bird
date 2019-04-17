package engineering.swat.metal.util;

import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.data.ParseItem;

public class ParseGraphSerializer {

	public static void println(ParseGraph pg) {
		println(pg, 0);
	}
	
	public static void println(ParseGraph pg, int nesting) {
		tabs(nesting);
		if (pg.equals(ParseGraph.EMPTY)) {
			System.out.println("pg(EMPTY)");
		}
		else if (pg.isEmpty()) {
			System.out.println("pg(terminator: " + pg.definition.getClass().getSimpleName() + ")");
		}
		else {
			System.out.println("pg(");
			println(pg.head, nesting+1);
			tabs(nesting);
			System.out.println(",");
			println(pg.tail, nesting+1);
			tabs(nesting);
			System.out.println(")");
		}
	}
	
	private static void println(ParseItem head, int nesting) {
		if (head instanceof ParseGraph) {
			println((ParseGraph) head, nesting);
		}
		else {
			tabs(nesting);
			System.out.println(head);
		}
		
	}

	public static void tabs(int n) {
		for (int i = 0; i < n ; i++)
			System.out.print("|--");
	}
}
