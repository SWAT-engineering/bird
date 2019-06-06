module lang::bird::nescio::NescioPlugin

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::nescio::API;
import util::Reflective;

import ParseTree;
import Map;
import Set;
import Relation;

import IO;

StructuredGraph birdGraphCalculator(str moduleName, PathConfig pcfg) {
 	start[Program] pt = sampleBird(moduleName, pcfg);
 	return birdGraphCalculator(pt);
 }
 
str findModuleId(loc typeLoc , TModel model) {
	println(typeLoc);
	loc l = typeLoc in domain(model.useDef) ? getOneFrom(model.useDef[typeLoc]) : typeLoc;
	println(l);
	while (l in model.scopes) {
		l = model.scopes[l];
	}
	println(l);
	if (<l, id, _, _, _> <- model.defines)
		return id;
	else
		return "";
}

StructuredGraph birdGraphCalculator(start[Program] pt) {
 	TModel model = birdTModelFromTree(pt);
 	map[loc, AType] types = getFacts(model);
   //alias Scopes  = map[loc inner, loc outer];                   // Syntactic containment
    Scopes scopes = model.scopes;
    map[loc, tuple[str, str]] qualifiedNames = ();
    for (loc typeLoc <- types, structDef(name, _) := types[typeLoc]) {
    	qualifiedNames += (typeLoc : <findModuleId(typeLoc, model), name>);
    }
    return calculateFields(pt.top, model, types);
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
str toStr(moduleType()) = "module";
str toStr(consType(_)) = "cons";
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
	
	
loc getTypeIdLoc((Type) `<Id id> <TypeActuals? _>`) = id@\loc;

loc getTypeIdLoc((Type) `<Type t> []`) = getTypeIdLoc(t);

default loc getTypeIdLoc(Type t) = t@\loc;
	
// TODO what about type variables?	
//alias Fields = rel[str typeName, str field, str fieldType];
StructuredGraph calculateFields((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, TModel model, map[loc, AType] types)  
	= ({} | it + calculateFields(d, moduleName, model, types) | d <- decls);
	
StructuredGraph calculateFields(current:(TopLevelDecl) `struct <Id sid> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, ModuleId currentModule, TModel model, map[loc, AType] types) =
	{ <typeName([findModuleId(current@\loc, model)], "<sid>"), "<d.id>", typeName([findModuleId(getTypeIdLoc(d.ty), model)], toStr(types[(d.ty)@\loc]))> | d <- decls,  !isAnonymousField(d), !(d.ty is anonymousType) }
	+
	{ <typeName([findModuleId(current@\loc, model)], "<sid>"), makeId(d.id), typeName([findModuleId(getTypeIdLoc(d.ty), model)], toStr(types[(d.ty)@\loc]))> | d <- decls,  isAnonymousField(d), !(d.ty is anonymousType) } 
	;
	
StructuredGraph calculateFields(current:(TopLevelDecl) `choice <Id sid> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, ModuleId currentModule, TModel model, map[loc, AType] types) =
	{ <typeName([findModuleId(current@\loc, model)], "<sid>"), "<id>", typeName([findModuleId(getTypeIdLoc(tp), model)], toStr(types[tp@\loc]))> | (DeclInChoice) `abstract <Type tp> <Id id>` <- decls }
	+ 
	{ <typeName([findModuleId(current@\loc, model)], "<sid>"), "entry", typeName([findModuleId(getTypeIdLoc(tp), model)], toStr(types[tp@\loc]))> | d:(DeclInChoice) `<Type tp> <Arguments? _> <Size? _>` <- decls, !(d.tp is anonymousType)}
	;

