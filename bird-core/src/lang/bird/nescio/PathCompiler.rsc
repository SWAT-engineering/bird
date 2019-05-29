module lang::bird::nescio::PathCompiler

import lang::bird::nescio::NescioPlugin;

import lang::nescio::API;
import lang::nescio::PathCompiler;

import ParseTree;
import IO;

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
FieldsAndBody createBody(rootType(typeName(packages, clazz)), str className, StructuredGraph graph) =
	<
		["typedRoot"]
	,
		"if (!(root instanceof <className>$.<clazz>)) {
		'	return locs;	
		'}
		'<className>$.<clazz> typedRoot = (<className>$.<clazz>) root;"
	>
	;

FieldsAndBody createBody(path: deepMatchType(Path src, TypeName tn), str className, StructuredGraph graph) =
	<
		["<field>.<fieldName>" | field <- fields, <_, fieldName, tn> <- graph, tn in pathTypes]
	, 
		body
	>
	when <fields, body> := createBody(src, className, graph),
		 set[TypeName] pathTypes := getTypes(path, graph);	

FieldsAndBody createBody(field(Path src, str fieldName), str className, StructuredGraph graph) =
	<
		["<field>.<fieldName>" | field <- fields]
	, 
		body
	>
	when <fields, body> := createBody(src, className, graph);
	
FieldsAndBody createBody(path: fieldType(Path src, TypeName tn), str className, StructuredGraph graph) =
	<
		["<field>.<fieldName>" | field <- fields, <_, fieldName, tn> <- graph, tn in pathTypes]
	, 
		body
	>
	when <fields, body> := createBody(src, className, graph),
		 set[TypeName] pathTypes := getTypes(path, graph);
		 

str compile((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, str rootPackageName, list[NamedPattern] patterns, StructuredGraph graph) =
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

	'import engineering.swat.nest.CommonTestHelper;
	'import engineering.swat.nest.core.bytes.ByteStream;
	'import engineering.swat.nest.core.bytes.Context;
	'import engineering.swat.nest.core.bytes.TrackedByteSlice;
	'import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
	'import engineering.swat.nest.core.nontokens.NestBigInteger;
	'import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
	'import engineering.swat.nest.nescio.Location;
	'import <containerClassName>;
	'
	'public class <className>Matcher {
	'	<for (pattern(name, path) <- patterns){ <fields, body> = createBody(path, "<className>", graph); >
	'	public static List\<Location\> <name>(Object root) {
	'		List\<Location\> locs = new ArrayList\<Location\>();
	'		<body>
	'		<for (field <- fields) {>
	'		locs.add(getLocation(<field>));
	'		<}>
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
	