module lang::bird::nescio::PathCompiler

import lang::bird::nescio::NescioPlugin;

import lang::nescio::API;
import lang::nescio::PathCompiler;

import ParseTree;
import IO;

import analysis::graphs::LabeledGraph;

import Set;
import Map;

import lang::bird::Syntax;
import lang::bird::Checker;

/*
data Path
    = field(Path src, str fieldName)
    | rootType(str typeName)
    | fieldType(Path src, str typeName)
    | deepMatchType(Path src, str typeName)
    ;
*/

alias FieldsAndBody = tuple[list[str] fields, str body];

// TODO this is a simplifivation. It is necessary to consider the packages for multiple modules.
str(str) createBody(rootType(typeName(packages, clazz)), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) = str(str hole){
	return
		"if (root instanceof <className>$.<clazz>) {
		'	List\<<className>$.<clazz>\> nodes<depth> = Arrays.asList((<className>$.<clazz>) root);
		'	for (<className>$.<clazz> node<depth>:nodes<depth>) {
		'		<hole>
		'	}
		'}
		";
	}
	;
	
str findPath(TypeName src, TypeName tgt, StructuredGraph graph) = 
	intercalate(".", [name | typeName(_, name) <- types])
	when Graph[TypeName] g := {<t1, t2> | <t1, _, t2> <- graph},
		 list[TypeName] types := shortestPathPair(g, src, tgt);
		
str toJavaType(str className, "byte") = 
	"UnsignedBytes";

		 
default str toJavaType(str className, str clazz) = 
	"<className>$.<clazz>";	
	
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when bprintln(fields[<pclazz, fieldName>]),
		 listType(byteType()) := fields[<pclazz, fieldName>];			 	
		 
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when bprintln(fields[<pclazz, fieldName>]),
		 listType(_) !:= fields[<pclazz, fieldName>];
	
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) =
	"node<depth+1>.<fieldName>.stream().collect(Collectors.toList())"
	when bprintln(fields[<pclazz, fieldName>]),
		 listType(t) := fields[<pclazz, fieldName>],
		 byteType() !:= t;

str(str) createBody(path:field(Path src, str fieldName), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] parentTns = getTypes(src, graph);
	TypeName parentTn = getOneFrom(parentTns);
	
	set[TypeName] tns = getTypes(path, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn.name;
	str inner =
	"List\<<toJavaType(className, clazz)>\> nodes<depth> =  <toJavaExpression(className, parentTn, tn, fieldName, depth, atypes, fields)>;
	'for (<toJavaType(className, clazz)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, className, graph, depth + 1, atypes, fields))(inner);
	};
	
str(str) createBody(path:fieldType(Path src, TypeName tn0), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	set[str] fieldNames = {fieldName | <tn, fieldName, tn0> <- graph};
	str inner =
	"List\<<toJavaType(className, clazz)>\> nodes<depth> =  Arrays.asList(<intercalate(", ", ["node<depth+1>.<fieldName>" | fieldName <- fieldNames])>);
	'for (<toJavaType(className, clazz)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, className, graph, depth + 1, atypes, fields))(inner);
	};	
	
list[str] getAllSuccessors(str acc, t1:typeName(_, str type1), t2:typeName(_, str type2), LGraph[TypeName, str] g) {
	list[str] ss = [];
	for (s <- successors(g, t1)) {
		if (t2 == s) {
			ss+= ["<acc>.<f>" | <t1, f, t2> <- g];
		}
		else {
			ss+=  ([] |it  + getAllSuccessors("<acc>.<f>", s, t2, g) | <t1, f, s> <- g);
		}
	};
	return ss;
}
		 
list[str] getAllPaths(t1:typeName(_, str type1), t2:typeName(_, str type2), StructuredGraph graph, int depth, map[TypeName, AType] atypes) {
	LGraph[TypeName, str] g = graph;
	return getAllSuccessors("node<depth+1>", t1, t2, graph);
}
		 
/*
str(str) createBody(path: deepMatchType(Path src, TypeName tn0), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	list[str] paths =getAllPaths(tn,  tn0, graph, depth, atypes);
	str inner =
	"List\<<toJavaType(className, clazz)>\> nodes<depth> =  Arrays.asList(<intercalate(", ", ["(<toJavaExpression(className, tn0, atypes)>) <p>" | p <- paths])>);
	'for (<toJavaType(className, clazz)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, className, graph, depth +1, atypes))(inner);
	};	
*/	
	
str(str) createBody(path: deepMatchType(Path src, TypeName tn0), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	list[str] paths =getAllPaths(tn,  tn0, graph, depth, atypes);
	str inner =
	"List\<<toJavaType(className, clazz)>\> nodes<depth> =  node<depth+1>.accept(new DeepMatchVisitor\<<toJavaType(className, clazz)>\>(<toJavaType(className, clazz)>.class));
	'for (<toJavaType(className, clazz)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, className, graph, depth +1, atypes, fields))(inner);
	};		
	
FieldsAndBody createBody(path: fieldType(Path src, TypeName tn), str className, StructuredGraph graph, map[TypeName, AType] atypes) =
	<
		["<field>.<fieldName>" | field <- fields, <_, fieldName, tn> <- graph, tn in pathTypes]
	, 
		body
	>
	when <fields, body> := createBody(src, className, graph, atypes),
		 set[TypeName] pathTypes := getTypes(path, graph);
		 

str compile((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, str rootPackageName, list[NamedPattern] patterns, StructuredGraph graph, map[TypeName, AType] atypes, map[tuple[str className, str fieldName], AType] fields) =
	"package engineering.swat.nest.examples.formats.bird_generated.nescio;
	'
	'import java.io.IOException;
	'import java.lang.reflect.InvocationTargetException;
	'import java.lang.reflect.Method;
	'import java.net.URISyntaxException;
	'import java.nio.file.Files;
	'import java.nio.file.Path;
	'import java.nio.file.Paths;
	'import java.util.ArrayList;
	'import java.util.List;
	'import java.util.Objects;
	'import java.util.stream.Collectors;
	'import engineering.swat.nest.CommonTestHelper;
	'import engineering.swat.nest.core.bytes.ByteStream;
	'import engineering.swat.nest.core.bytes.Context;
	'import engineering.swat.nest.core.bytes.TrackedByteSlice;
	'import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
	'import engineering.swat.nest.core.nontokens.NestBigInteger;
	'import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
	'import java.util.ArrayList;
	'import java.util.Arrays;
	'import engineering.swat.nest.nescio.Location;
	'import engineering.swat.nest.nescio.util.DeepMatchVisitor;
	'import engineering.swat.nest.nescio.util.StreamUtil;
	'import <containerClassName>;
	'
	'public class <className>Matcher {
	'	<for (pattern(name, path) <- patterns){ str s = createBody(path, "<className>", graph, 0, atypes, fields)("locs.add(getLocation(node0));"); >
	'	public static List\<Location\> <name>(Object root) {
	'		List\<Location\> locs = new ArrayList\<Location\>();
	'		<s>
	'		return locs;
	'	}	
	'	<}>
	'	private static Location getLocation(UnsignedBytes bytes) {
	'		TrackedByteSlice slice = bytes.getTrackedBytes();
	'		int offset = slice.getOrigin(NestBigInteger.ZERO).getOffset().intValueExact();
	'		return new Location(offset, slice.size().intValueExact());
	'	}
	
	'	public static void main(String[] args)
	'			throws IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	'		ClassLoader context = Objects.requireNonNull(CommonTestHelper.class.getClassLoader(),
	'				\"Unexpected missing classloader\");
	'		String parserClass = args[0];
	'		String file = args[1];
	'		if (parserClass == null)
	'			throw new RuntimeException(\"Parser node must be specified\");
	'		if (file == null)
	'			throw new RuntimeException(\"File to parse must be specified\");
	'		Path path = Paths.get(context.getResource(file).toURI());
	'	
	'		ByteStream stream = new ByteStream(ByteSliceBuilder.convert(Files.newInputStream(path), path.toUri()));
	'		Class clazz = Class.forName(\"<containerClassName>$\" + parserClass);
	'		Method method = clazz.getMethod(\"parse\", ByteStream.class, Context.class);
	'		method.setAccessible(true);
	'		Object r = method.invoke(null, stream, Context.DEFAULT);
	'		<for (pattern(str name, _) <- patterns) {>
	'		System.out.println(\"<name>\");
	'		for (Location l : <name>(r)) {
	'			System.out.println(l);
	'		}
	'		<}>
	'	}
	'}
	'"
	when [dirs*, className] := [x | x <- moduleName.moduleName],
		 str packageName := ((size(dirs) == 0)? "": ("."+ intercalate(".", dirs))),
		 str absolutePackageName := "<((rootPackageName != "")?"<rootPackageName>":"")><packageName>",
		 str containerClassName := "<absolutePackageName>.<className>$";
	
public start[Program] sampleBird(str name) = parse(#start[Program], |project://bird-core/bird-src/<name>.bird|);	
	
void main() {
	start[Program] png = sampleBird("PNG");
	NamedPattern p1 = pattern("crc", field(field(rootType(typeName([], "PNG")), "end"), "crc"));
	println(compile(png.top, "engineering.swat.nest.examples.formats.bird_generated", [p1]));	
}

void compilePathTo(loc file) {
	start[Program] png = sampleBird("PNG");
	StructuredGraph graph = birdGraphCalculator(png);
	println("<graph>");
	NamedPattern p1 = pattern("crc",       field(field(rootType(typeName([], "PNG")), "end"), "crc"));
	NamedPattern p2 = pattern("crcByType", field(fieldType(rootType(typeName([], "PNG")), typeName([], "IEND")), "crc"));
	NamedPattern p3 = pattern("crcByTypeDeepMatch", field(deepMatchType(rootType(typeName([], "PNG")), typeName([], "IEND")), "crc"));
	str src = compile(png.top, "engineering.swat.nest.examples.formats.bird_generated", [p1, p2, p3], graph);
	writeFile(file, src);
}
	
void compilePath2To(loc file) {
	start[Program] jpeg = sampleBird("JPEG");
	TModel model = birdTModelFromTree(jpeg);
	map[loc, AType] types = getFacts(model);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types, isUserDefined(types[locType]));
	atypes += (typeName([""], "byte"):byteType());
	StructuredGraph graph = birdGraphCalculator(jpeg);
	println("<graph>");
	NamedPattern p1 = pattern("lengthByTypeDeepMatch", field(deepMatchType(rootType(typeName([], "Format")), typeName([], "ScanSegment")), "length"));
	str src = compile(jpeg.top, "engineering.swat.nest.examples.formats.bird_generated", [p1], graph, atypes);
	writeFile(file, src);
}

TypeName getTypeName(structType(str name, list[AType] typeActuals), loc locType, TModel model) = typeName([findModuleId(locType, model)], name);
TypeName getTypeName(listType(AType t), loc locType, TModel model) = getTypeName(t, locType, model);
TypeName getTypeName(AType t, loc locType, TModel model) = typeName([], toStr(t));

map[tuple[str className, str fieldName], AType] atypesForFields(Program p, map[loc, AType] types) {
	map[tuple[str, str], AType] res = ();
	for ((TopLevelDecl) `struct <Id id> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }` <- p.declarations) {
		for (DeclInStruct dis <- decls) {
			res += (<"<id>", "<dis.id>"> : types[dis.ty@\loc]);
		};
	};
	
	for ((TopLevelDecl) `choice <Id id> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }` <- p.declarations) {
		for (DeclInChoice dis <- decls, dis is abstract) {
			res += (<"<id>", "<dis.id>"> : types[dis.tp@\loc]);
		};
		for (DeclInChoice dis <- decls, !(dis is abstract)) {
			res += (<"<id>", "entry"> : types[dis.tp@\loc]);
		};
	};
	return res;
}

void compilePath3To(loc file) {
	start[Program] uvw = sampleBird("uvw");
	TModel model = birdTModelFromTree(uvw);
	StructuredGraph graph = birdGraphCalculator(uvw);
	map[loc, AType] types = getFacts(model);
	map[tuple[str className, str fieldName], AType]  fields = atypesForFields(uvw.top, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types);
	atypes += (typeName([""], "byte"):byteType());
	//NamedPattern p1 = pattern("findX", field(deepMatchType(rootType(typeName([], "U")), typeName([], "X")), "x1"));
	NamedPattern p1 = pattern("findX", field(field(rootType(typeName(["uvw"], "U")), "x"), "x1"));
	NamedPattern p2 = pattern("findXType", field(fieldType(rootType(typeName(["uvw"], "U")), typeName(["uvw"], "X")), "x1"));
	NamedPattern p3 = pattern("findZ", field(field(field(rootType(typeName(["uvw"], "U")), "p"), "v"), "z1"));
	NamedPattern p4 = pattern("findXDeep", field(deepMatchType(rootType(typeName(["uvw"], "U")), typeName(["uvw"], "X")), "x1"));
	str src = compile(uvw.top, "engineering.swat.nest.examples.formats.bird_generated", [p1, p2, p3, p4], graph, atypes, fields);
	writeFile(file, src);
}
	