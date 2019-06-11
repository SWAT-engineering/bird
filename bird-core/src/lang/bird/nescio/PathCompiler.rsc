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
str(str) createBody(rootType(tn: typeName(packages, clazz)), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole){
	return
		"if (root instanceof <className>$.<clazz>) {
		'	List\<<toJavaType(tn)>\> nodes<depth> = Arrays.asList((<className>$.<clazz>) root);
		'	for (<toJavaType(tn)> node<depth>:nodes<depth>) {
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
		
/*str toJavaType(str className, "byte") = 
	"UnsignedBytes";

		 
default str toJavaType(str className, str clazz) = 
	"<className>$.<clazz>";	
*/	
	

str toJavaType(typeName([], "byte")) = 
	"UnsignedBytes";		

str toJavaType(typeName(list[str] package, str clazz)) = 
	"<intercalate(".", package)>$.<clazz>"
	when clazz !:= "byte";		

		
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when bprintln(fields[<parent, fieldName>]),
		 listType(byteType()) := fields[<parent, fieldName>];			 	
		 
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when bprintln(fields[<parent, fieldName>]),
		 listType(_) !:= fields[<parent, fieldName>];
	
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"node<depth+1>.<fieldName>.stream().collect(Collectors.toList())"
	when bprintln(fields[<parent, fieldName>]),
		 listType(t) := fields[<parent, fieldName>],
		 byteType() !:= t;

str(str) createBody(path:field(Path src, str fieldName), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
	println(src);
	println(graph);
	set[TypeName] parentTns = getTypes(src, graph);
	println(src);
	TypeName parentTn = getOneFrom(parentTns);
	
	set[TypeName] tns = getTypes(path, graph);
	println(path);
	TypeName tn = getOneFrom(tns);
	str clazz = tn.name;
	str inner =
	"List\<<toJavaType(tn)>\> nodes<depth> =  <toJavaExpression(className, parentTn, tn, fieldName, depth, atypes, fields)>;
	'for (<toJavaType(tn)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, className, graph, depth + 1, atypes, fields))(inner);
	};
	
str(str) createBody(path:fieldType(Path src, TypeName tn0), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	set[str] fieldNames = {fieldName | <tn, fieldName, tn0> <- graph};
	str inner =
	"List\<<toJavaType(tn0)>\> nodes<depth> =  Arrays.asList(<intercalate(", ", ["node<depth+1>.<fieldName>" | fieldName <- fieldNames])>);
	'for (<toJavaType(tn0)> node<depth>:nodes<depth>) {
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
	
str(str) createBody(path: deepMatchType(Path src, TypeName tn0), str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	for (line <- graph)
		println(line);
	println(path);
	println(tns);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	list[str] paths =getAllPaths(tn,  tn0, graph, depth, atypes);
	str inner =
	"List\<<toJavaType(tn0)>\> nodes<depth> =  node<depth+1>.accept(new DeepMatchVisitor\<<toJavaType(tn0)>\>(<toJavaType(tn0)>.class));
	'for (<toJavaType(tn0)> node<depth>:nodes<depth>) {
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
		 

str compile((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, str rootPackageName, list[NamedPattern] patterns, StructuredGraph graph, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType] fields) =
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
	'<for ((Import) `import <ModuleId mid>` <- imports) {>
	'import <rootPackageName>.<intercalate(".", ["<id>" | Id id <- mid.moduleName])>$;
	'import <rootPackageName>.<intercalate(".", ["<id>" | Id id <- mid.moduleName])>$.*;
	'<}>
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
	TModel model = birdTModelFromTree(png);
	StructuredGraph graph = birdGraphCalculator(png);
	map[loc, AType] types = getFacts(model);
	map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(png.top, model, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types);
	atypes += (typeName([], "byte"):byteType());
	NamedPattern p1 = pattern("crc",       field(field(rootType(typeName(["PNG"], "PNG")), "end"), "crc"));
	NamedPattern p2 = pattern("crcByType", field(fieldType(rootType(typeName(["PNG"], "PNG")), typeName(["PNG"], "IEND")), "crc"));
	NamedPattern p3 = pattern("crcByTypeDeepMatch", field(deepMatchType(rootType(typeName(["PNG"], "PNG")), typeName(["PNG"], "IEND")), "crc"));
	str src = compile(png.top, "engineering.swat.nest.examples.formats.bird_generated", [p1, p2, p3], graph, atypes, fields);
	writeFile(file, src);
}
	
void compilePath2To(loc file) {
	start[Program] jpeg = sampleBird("JPEG");
	TModel model = birdTModelFromTree(jpeg);
	StructuredGraph graph = birdGraphCalculator(jpeg);
	map[loc, AType] types = getFacts(model);
	map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(jpeg.top, model, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types, !(types[locType] is funType));
	atypes += (typeName([], "byte"):byteType());
	NamedPattern p1 = pattern("lengthByTypeDeepMatch", field(deepMatchType(rootType(typeName(["JPEG"], "Format")), typeName(["JPEG"], "ScanSegment")), "length"));
	str src = compile(jpeg.top, "engineering.swat.nest.examples.formats.bird_generated", [p1], graph, atypes, fields);
	writeFile(file, src);
}

TypeName getTypeName(structType(str name, list[AType] typeActuals), loc locType, TModel model) = typeName(findModuleId(locType, model), name);
TypeName getTypeName(listType(AType t), loc locType, TModel model) = getTypeName(t, locType, model);
TypeName getTypeName(AType t, loc locType, TModel model) = typeName([], toStr(t));

map[tuple[TypeName tn, str fieldName], AType] atypesForFields(Program p, TModel model, map[loc, AType] types) {
	map[tuple[TypeName tn, str fieldName], AType] res = ();
	map[loc structLoc, str structName] structNames = ();
	for (<_, structName, structId(), definedStructLoc, _> <- model.defines) {
		structNames += (definedStructLoc : structName);
	};
	for (<structLoc, fieldName, fieldId(), definedFieldLoc, defType(ty)> <- model.defines, !(
	types[structLoc] is funType || types[structLoc] is anonType)) {
		res += (<typeName(findModuleId(structLoc, model), structNames[structLoc]), fieldName> : model.facts[ty@\loc]);
	};
	return res;
}

void compilePath3To(loc file) {
	start[Program] uvw = sampleBird("uvw");
	TModel model = birdTModelFromTree(uvw);
	StructuredGraph graph = birdGraphCalculator(uvw);
	map[loc, AType] types = getFacts(model);
	map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(uvw.top, model, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types);
	atypes += (typeName([], "byte"):byteType());
	//NamedPattern p1 = pattern("findX", field(deepMatchType(rootType(typeName([], "U")), typeName([], "X")), "x1"));
	NamedPattern p1 = pattern("findX", field(field(rootType(typeName(["uvw"], "U")), "x"), "x1"));
	NamedPattern p2 = pattern("findXType", field(fieldType(rootType(typeName(["uvw"], "U")), typeName(["x"], "X")), "x1"));
	NamedPattern p3 = pattern("findZ", field(field(field(rootType(typeName(["uvw"], "U")), "p"), "v"), "z1"));
	NamedPattern p4 = pattern("findXDeep", field(deepMatchType(rootType(typeName(["uvw"], "U")), typeName(["x"], "X")), "x1"));
	str src = compile(uvw.top, "engineering.swat.nest.examples.formats.bird_generated", [p1, p2, p3, p4], graph, atypes, fields);
	writeFile(file, src);
}
	
void test1() {
	start[Program] uvw = sampleBird("uvw");
	TModel model = birdTModelFromTree(uvw);
	for (<scope, id, fieldId(), defined, defType(ty)> <- model.defines) {
		println("{<<scope, id, defined>>}");
		println(model.facts[ty@\loc]);
	}
}
