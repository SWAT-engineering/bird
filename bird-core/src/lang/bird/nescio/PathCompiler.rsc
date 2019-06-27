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
str(str) createBody(rootType(tn: typeName(packages, clazz)),  str rootPackageName, str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole){
	return
		"if (root instanceof <className>$.<clazz>) {
		'	List\<<toJavaType(rootPackageName, tn)>\> nodes<depth> = Arrays.asList((<className>$.<clazz>) root);
		'	for (<toJavaType(rootPackageName, tn)> node<depth>:nodes<depth>) {
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
	

str toJavaType(str rootPackageName, typeName([], "byte")) = 
	"UnsignedBytes";		

str toJavaType(str rootPackageName, typeName(list[str] package, str clazz)) = 
	"<intercalate(".", (rootPackageName == "" ? package : [rootPackageName] + package))>$.<clazz>"
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

str(str) createBody(path:field(Path src, str fieldName),  str rootPackageName, str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
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
	"List\<<toJavaType(rootPackageName, tn)>\> nodes<depth> =  <toJavaExpression(className, parentTn, tn, fieldName, depth, atypes, fields)>;
	'for (<toJavaType(rootPackageName, tn)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, rootPackageName, className, graph, depth + 1, atypes, fields))(inner);
	};
	
str(str) createBody(path:fieldType(Path src, TypeName tn0),  str rootPackageName, str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
	set[TypeName] tns = getTypes(src, graph);
	TypeName tn = getOneFrom(tns);
	str clazz = tn0.name;
	println(tn);
	println(tn0);
	set[str] fieldNames = {fieldName | <tn, fieldName, tn0> <- graph};
	str inner =
	"List\<<toJavaType(rootPackageName, tn0)>\> nodes<depth> =  Arrays.asList(<intercalate(", ", ["node<depth+1>.<fieldName>" | fieldName <- fieldNames])>);
	'for (<toJavaType(rootPackageName, tn0)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, rootPackageName, className, graph, depth + 1, atypes, fields))(inner);
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
	
str(str) createBody(path: deepMatchType(Path src, TypeName tn0), str rootPackageName, str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
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
	"List\<<toJavaType(rootPackageName, tn0)>\> nodes<depth> =  node<depth+1>.accept(new DeepMatchVisitor\<<toJavaType(rootPackageName, tn0)>\>(<toJavaType(rootPackageName, tn0)>.class));
	'for (<toJavaType(rootPackageName, tn0)> node<depth>:nodes<depth>) {
	'		<hole>
	'}";
	return (createBody(src, rootPackageName, className, graph, depth +1, atypes, fields))(inner);
	};		
	
FieldsAndBody createBody(path: fieldType(Path src, TypeName tn), str rootPackageName, str className, StructuredGraph graph, map[TypeName, AType] atypes) =
	<
		["<field>.<fieldName>" | field <- fields, <_, fieldName, tn> <- graph, tn in pathTypes]
	, 
		body
	>
	when <fields, body> := createBody(src, rootPackageName, className, graph, atypes),
		 set[TypeName] pathTypes := getTypes(path, graph);
		 
str toJavaValue(javaStringType(), str val) = "\"<val>\"";
default str toJavaValue(JavaType _, str val) = val;

str compile((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, str rootPackageName, TypeName initialNonTerminal, Rules rules, StructuredGraph graph, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType] fields) =
	"package <absolutePackageName>;
	'
	'import java.io.IOException;
	'import java.lang.reflect.InvocationTargetException;
	'import java.lang.reflect.Method;
	'import java.net.URISyntaxException;
	'import java.net.URL;
	'import java.nio.file.Files;
	'import java.nio.file.Path;
	'import java.nio.file.Paths;
	'import java.util.ArrayList;
	'import java.util.List;
	'import java.util.Map;
	'import java.util.HashMap;
	'import java.util.Objects;
	'import java.util.stream.Collectors;
	'import engineering.swat.nescio.Anonymizer;
	'import engineering.swat.nescio.Range;
	'import engineering.swat.nescio.MatchingException;
	'import engineering.swat.nescio.TransformationDescription;
	'import engineering.swat.nest.CommonTestHelper;
	'import engineering.swat.nest.core.bytes.ByteStream;
	'import engineering.swat.nest.core.bytes.Context;
	'import engineering.swat.nest.core.bytes.TrackedByteSlice;
	'import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
	'import engineering.swat.nest.core.nontokens.NestBigInteger;
	'import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
	'import engineering.swat.nest.core.tokens.Token;
	'import java.util.ArrayList;
	'import java.util.Arrays;
	'import engineering.swat.nest.nescio.util.DeepMatchVisitor;
	'import <containerClassName>;
	'<for ((Import) `import <ModuleId mid>` <- imports) {>
	'import <rootPackageName>.<intercalate(".", ["<id>" | Id id <- mid.moduleName])>$;
	'import <rootPackageName>.<intercalate(".", ["<id>" | Id id <- mid.moduleName])>$.*;
	'<}>
	'public class <className>Anonymizer extends Anonymizer {
	'	<for (pattern(name, path) <- patterns){ str s = createBody(path, rootPackageName, "<className>", graph, 0, atypes, fields)("locs.add(getRange(node0));"); >
	'	public static List\<Range\> __<name>(Object root) {
	'		List\<Range\> locs = new ArrayList\<Range\>();
	'		<s>
	'		return locs;
	'	}	
	'	<}>
	'	private static Range getRange(UnsignedBytes bytes) {
	'		TrackedByteSlice slice = bytes.getTrackedBytes();
	'		int offset = slice.getOrigin(NestBigInteger.ZERO).getOffset().intValueExact();
	'		return new Range(offset, slice.size().intValueExact());
	'	}
	'
	'	private static Range getRange(Token token) {
	'		TrackedByteSlice slice = token.getTrackedBytes();
	'		int offset = slice.getOrigin(NestBigInteger.ZERO).getOffset().intValueExact();
	'		return new Range(offset, slice.size().intValueExact());
	'	}
	'
	'	@Override
	'	public Map\<String, TransformationDescription\> match(Path input) throws MatchingException{
	'		Map\<String, TransformationDescription\> trafos = new HashMap\<\>();
	'		<toJavaType(rootPackageName, initialNonTerminal)> r; 
	'		try {		
	'			ByteStream stream = new ByteStream(ByteSliceBuilder.convert(Files.newInputStream(input), input.toUri()));
	'			r= <toJavaType(rootPackageName, initialNonTerminal)>.parse(stream, Context.DEFAULT);
	'		
	'		} catch (IOException e) {
	'			throw new MatchingException(e);
	'		}
	'		List\<Range\> ranges;
	'		<for (str ruleName <- rules, <Path path, <str javaClass, lrel[JavaType, str] args>> := rules[ruleName]) {>
	'		ranges = new ArrayList\<\>();
	'		for (Range range: __<ruleName>(r)) {
	'			System.out.println(range);
	'			ranges.add(range);
	'		}
	'		trafos.put(\"<ruleName>\", 
	'			new TransformationDescription(
	'				bytes -\> <javaClass>(<intercalate(", ", ["bytes"] + [toJavaValue(typ, val) | <JavaType typ, str val> <- args])>),
	'				ranges));
	'				
	'		<}>
	'		return trafos;
	'	}
	'
	'	public static void main(String[] args) throws URISyntaxException, IOException, MatchingException {
	'		ClassLoader context = Objects.requireNonNull(CommonTestHelper.class.getClassLoader(),
	'				\"Unexpected missing classloader\");
	'		String inputFile = args[0];
	'		String outputFile = args[1];
	'		if (inputFile == null)
	'			throw new RuntimeException(\"File to parse must be specified\");
	'		if (outputFile == null)
	'			throw new RuntimeException(\"Output file must be specified\");
	'	
	'		Path input = Paths.get(context.getResource(inputFile).toURI());
	'		Path output = Paths.get(new URL(context.getResource(\".\").toURI().toURL(), outputFile).toURI());
	'		Anonymizer anonimizer = new TCP_IP_non_modularAnonymizer();
	'		anonimizer.anonymize(input, output);
	'	}
	'
	'	public static void mainOld(String[] args)
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
	'		for (Range l : __<name>(r)) {
	'			System.out.println(l);
	'		}
	'		<}>
	'	}
	'}
	'"
	when [dirs*, className] := [x | x <- moduleName.moduleName],
		 str packageName := ((size(dirs) == 0)? "": ("."+ intercalate(".", dirs))),
		 str absolutePackageName := "<((rootPackageName != "")?"<rootPackageName>":"")><packageName>",
		 str containerClassName := "<absolutePackageName>.<className>$",
		 list[NamedPattern] patterns := [pattern(ruleName, path) | str ruleName <- rules, <Path path, _> := rules[ruleName]];
	
	
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

void compilePath4To(loc file) {
	start[Program] uvw = sampleBird("network/TCP_IP_non_modular");
	TModel model = birdTModelFromTree(uvw);
	StructuredGraph graph = birdGraphCalculator(uvw);
	map[loc, AType] types = getFacts(model);
	map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(uvw.top, model, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, model):types[locType] | locType <- types);
	atypes += (typeName([], "byte"):byteType());
	NamedPattern p1 = pattern("findDNSNId", field(field(deepMatchType(rootType(typeName(["network", "TCP_IP_non_modular"], "PCAP")), typeName(["network", "TCP_IP_non_modular"], "DNS")), "header"), "identifier"));
	NamedPattern p2 = pattern("findQuestionName", field(deepMatchType(rootType(typeName(["network", "TCP_IP_non_modular"], "PCAP")), typeName(["network", "TCP_IP_non_modular"], "Question")), "questionName"));
	str src = compile(uvw.top, "engineering.swat.nest.examples.formats.bird_generated", [p1, p2], graph, atypes, fields);
	writeFile(file, src);
}

void compilePath(loc modelFile, loc nescioFile, loc outputFile) {
	start[Program] birdProgram = parse(#start[Program], modelFile);
	TModel birdModel = birdTModelFromTree(birdProgram);
	map[loc, AType] types = getFacts(birdModel);
	map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(birdProgram.top, birdModel, types);
	map[TypeName, AType] atypes = (getTypeName(types[locType], locType, birdModel):types[locType] | locType <- types);
	atypes += (typeName([], "byte"):byteType());
	
	loc initialBirdDir = |project://bird-core/bird-src/|;
	
	StructuredGraph graph = computeAggregatedStructuredGraph(nescioFile, buildBirdModulesComputer(initialBirdDir), birdGraphCalculator);
	
	println("the graph: <graph>");
	
	Rules rules = evalNescio(nescioFile, graph);
	
	TypeName root = getRoot(nescioFile, graph);
	
	// create list of named patterns from rules 
	
	str src = compile(birdProgram.top, "engineering.swat.nest.examples.formats.bird_generated", root, rules, graph, atypes, fields);
	writeFile(outputFile, src);
}