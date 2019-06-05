module lang::bird::nescio::NescioPlugin

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::nescio::API;
import util::Reflective;

import ParseTree;

StructuredGraph birdGraphCalculator(str moduleName, PathConfig pcfg) {
 	start[Program] pt = sampleBird(moduleName, pcfg);
 	return birdGraphCalculator(pt);
 }

StructuredGraph birdGraphCalculator(start[Program] pt) {
 	TModel model = birdTModelFromTree(pt);
 	map[loc, AType] types = getFacts(model);
    rel[loc, loc] useDefs = getUseDef(model);
    return calculateFields(pt.top, useDefs, types);
 }
  
public start[Program] sampleBird(str name) = parse(#start[Program], |project://bird-core/bird-src/<name>.bird|);

str toStr(voidType()) = "void";
str toStr(byteType()) = "byte";
str toStr(intType()) = "int";
str toStr(typeType(t)) = "typeof(<toStr(t)>)";
str toStr(strType()) = "str";
str toStr(boolType()) = "bool";
str toStr(listType(t)) = toStr(t);
str toStr(structType(name, args)) = name;
str toStr(variableType(s)) = s;
str toStr(structDef(name, formals)) = name;
str toStr(AType t){
	throw "Unknown type: <t>";
}

// Duplicate from generator to nest
str makeId(Tree t) = ("<t>" =="_")?"$anon_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.begin.column>_<lo.end.line>_<lo.end.column>":"<t>"
	when lo := t@\loc;
  
bool isAnonymousField((DeclInStruct) `<Type ty> _ <Arguments? _> <Size? _> <SideCondition? _>`)
	= true;

default bool isAnonymousField(DeclInStruct d)
	= false;
	
	
// TODO what about type variables?	
//alias Fields = rel[str typeName, str field, str fieldType];
StructuredGraph calculateFields((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types)  
	= ({} | it + calculateFields(d, moduleName, useDefs, types) | d <- decls);
	
StructuredGraph calculateFields(current:(TopLevelDecl) `struct <Id sid> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, ModuleId currentModule, rel[loc,loc] useDefs, map[loc, AType] types) =
	{ <typeName([], "<sid>"), "<d.id>", typeName([], toStr(types[(d.ty)@\loc]))> | d <- decls,  !isAnonymousField(d), !(d.ty is anonymousType) }
	+
	{ <typeName([], "<sid>"), makeId(d.id), typeName([], toStr(types[(d.ty)@\loc]))> | d <- decls,  isAnonymousField(d), !(d.ty is anonymousType) } 
	;
	
StructuredGraph calculateFields(current:(TopLevelDecl) `choice <Id sid> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, ModuleId currentModule, rel[loc,loc] useDefs, map[loc, AType] types) =
	{ <typeName([], "<sid>"), "<id>", typeName([], toStr(types[tp@\loc]))> | (DeclInChoice) `abstract <Type tp> <Id id>` <- decls }
	+ 
	{ <typeName([], "<sid>"), "entry", typeName([], toStr(types[tp@\loc]))> | d:(DeclInChoice) `<Type tp> <Arguments? _> <Size? _>` <- decls, !(d.tp is anonymousType)}
	;

