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

data PathConfig(loc target = |cwd:///|);	

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
		"if (root instanceof <toJavaType(rootPackageName, tn)>) {
		'	List\<<toJavaType(rootPackageName, tn)>\> nodes<depth> = Arrays.asList((<toJavaType(rootPackageName, tn)>) root);
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
	"<intercalate(".", (rootPackageName == "" ? package : [rootPackageName] + package))>$.__$<clazz>"
	when clazz !:= "byte";		

		
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when //bprintln(fields[<parent, fieldName>]),
		 listType(byteType()) := fields[<parent, fieldName>];			 	
		 
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"Arrays.asList(node<depth+1>.<fieldName>)"
	when //bprintln(fields[<parent, fieldName>]),
		 listType(_) !:= fields[<parent, fieldName>];
	
str toJavaExpression(str className, parent: typeName(_, pclazz), tn:typeName(_, clazz), str fieldName, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) =
	"node<depth+1>.<fieldName>.stream().collect(Collectors.toList())"
	when //bprintln(fields[<parent, fieldName>]),
		 listType(t) := fields[<parent, fieldName>],
		 byteType() !:= t;

str(str) createBody(path:field(Path src, str fieldName),  str rootPackageName, str className, StructuredGraph graph, int depth, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType]  fields) = str(str hole) {
	println(src);
	set[TypeName] parentTns = getTypes(src, graph);
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
		 
default str toJavaValue(JavaType ty, str val) = val;

str compile((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, str rootPackageName, TypeName initialNonTerminal, Rules rules, StructuredGraph graph, map[TypeName, AType] atypes, map[tuple[TypeName tn, str fieldName], AType] fields) =
	"import java.io.File;
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
	'import <rootPrefix><intercalate(".", ["<id>" | Id id <- mid.moduleName])>$;
	'import <rootPrefix><intercalate(".", ["<id>" | Id id <- mid.moduleName])>$.*;
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
	'		if (args.length != 2) {
	'			System.out.println(\"Please provide both input and output files as arguments.\");
	'			return;
	'		}
	'		String inputFile = args[0];
	'		String outputFile = args[1];
	'
	'		Path input = Paths.get(new File(inputFile).toURI());
	'		Path output = Paths.get(new File(outputFile).toURI());
	'		Anonymizer anonymizer = new <className>Anonymizer();
	'		anonymizer.anonymize(input, output);
	'	}
	'
	'}
	'"
	when [dirs*, className] := [x | x <- moduleName.moduleName],
		 str packageName := ((size(dirs) == 0)? "": (intercalate(".", dirs))),
		 str rootPrefix := (rootPackageName == "" ? "" : "<rootPackageName>."),
		 str absolutePackageName := "<rootPrefix><packageName>",
		 str containerClassName := "<absolutePackageName>.<className>$",
		 list[NamedPattern] patterns := [pattern(ruleName, path) | str ruleName <- rules, <Path path, _> := rules[ruleName]];

TypeName getTypeName(structType(str name, list[AType] typeActuals), loc locType, set[TModel] models) = typeName(findModuleId(locType, models), name);
TypeName getTypeName(listType(AType t), loc locType, set[TModel] models) = getTypeName(t, locType, models);
TypeName getTypeName(AType t, loc locType, set[TModel] models) = typeName([], toStr(t));

map[tuple[TypeName tn, str fieldName], AType] atypesForFields(Program p, set[TModel] models, map[loc, AType] types) {
	int i = 0;
	for (TModel model <- models) {
		println("Model <i>: <size(model.useDef)> useDefs, <size(model.facts)> facts, <size(model.defines)> definitions");
		i = i + 1;
	}
	map[tuple[TypeName tn, str fieldName], AType] res = ();
	map[loc structLoc, str structName] structNames = ();
	
	Defines allDefines = ({} | it + model.defines | model <- models) ;
	
	for (<_, structName, structId(), definedStructLoc, _> <- allDefines) {
		structNames += (definedStructLoc : structName);
	}
	
	map[loc,AType] allFacts = (() | it + model.facts | model <- models);
	
	for (<structLoc, fieldName, fieldId(), definedFieldLoc, defType(ty)> <- allDefines, !(types[structLoc] is funType || types[structLoc] is anonType)) {
		res += (<typeName(findModuleId(structLoc, models), structNames[structLoc]), fieldName> : allFacts[ty@\loc]);
	}

	return res;
}

loc constructBirdLocation(loc birdDir, typeName(list[str] path, str _)) 
	= birdDir + intercalate("/", locComponents)
	when list[str] locComponents := path[0..-1] + ["<path[-1]>.bird"];
	
loc constructBirdLocationForImport(loc birdDir, typeName(list[str] path, str name)) 
	= birdDir + intercalate("/", locComponents)
	when list[str] locComponents := path + ["<name>.bird"];
	
set[TModel] getImportedModelsForNescioFile(loc birdSrcDir, Tree nescioSpec, StructuredGraph graph, PathConfig pathConf) {
	set[TModel] result = {};
	list[TypeName] importedTypes = getImported(nescioSpec, graph);
	for (TypeName imported <- importedTypes) {
		loc birdLoc = constructBirdLocationForImport(birdSrcDir, imported);
		println("constructed <birdLoc> for <imported>");
		if (exists(birdLoc)) {
			start[Program] birdProgram = parse(#start[Program], birdLoc);
			TModel birdModel = birdTModelFromTree(birdProgram, pathConf = pathConf);
			result += birdModel;
		}
	}
	return result;
}	

void compileNescioForBird(Tree nescioSpec, str basePkg, PathConfig pcfg) {
	
	println("Compiling nescio spec: <nescioSpec.top.moduleId>");
	
	loc birdLoc = |cwd:///|;
	loc birdSrcDir = |cwd:///|;
	bool found = false;
	
	StructuredGraph graph = computeAggregatedStructuredGraph(nescioSpec, buildBirdModulesComputer(pcfg.srcs), birdGraphCalculator(pcfg));
	
	TypeName rootType = getRoot(nescioSpec, graph);
	 
	for (loc birdDir <- pcfg.srcs, !found) {
		birdLoc = constructBirdLocation(birdDir, rootType);
		birdSrcDir = birdDir;
		if (exists(birdLoc)) {
			found = true;
		}
	}
	println(birdLoc);
	if (found) {
		start[Program] birdProgram = parse(#start[Program], birdLoc);
		
		set[TModel] models = getImportedModelsForNescioFile(birdSrcDir, nescioSpec, graph, pcfg) + {birdTModelFromTree(birdProgram, pathConf = pcfg)};
		
		map[loc, AType] types = ();
		
		for (TModel birdModel <- models) {
			 types += getFacts(birdModel);
		}
		
		map[tuple[TypeName tn, str fieldName], AType]  fields = atypesForFields(birdProgram.top, models, types);
		map[TypeName, AType] atypes = (getTypeName(types[locType], locType, models):types[locType] | locType <- types);
		
		atypes += (typeName([], "byte"):byteType());
		
		Rules rules = evalNescio(nescioSpec, graph);
	
		str text = compile(birdProgram.top, basePkg, rootType, rules, graph, atypes, fields);
	
		list[str] fileParts = ["<x>" | x <- birdProgram.top.moduleName.moduleName];
		path = fileParts[-1] + "Anonymizer.java";
	
    	println("Writing to: <pcfg.target + path>");
    	writeFile(pcfg.target + path, text);
    }
}

