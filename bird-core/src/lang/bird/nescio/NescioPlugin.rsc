module lang::bird::nescio::NescioPlugin

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::nescio::API;
import util::Reflective;

import ParseTree;
import Map;
import Set;
import Relation;
import String;

import IO;

loc(TypeName) buildBirdModuleMapper(loc baseLoc) = loc(TypeName tn){
	if (typeName(path, name) := tn) {
		return baseLoc + "<intercalate("/", path)>/<name>.bird";
	}
};

TypeName birdModuleIdToTypeName((ModuleId) `<{Id "::"}+ moduleName>`) = typeName(lst[0..-1], lst[-1])
	when lst := ["<id>" | Id id <- moduleName];

list[loc](TypeName) buildBirdModulesComputer(loc baseLoc) = list[loc](TypeName m) {
	loc file = (buildBirdModuleMapper(baseLoc))(m);
	if (exists(file)) {
		start[Program] p = parseBird(file);
		set[TypeName] types = { birdModuleIdToTypeName(mid) | (Program) `module <ModuleId mid> <Import* imports> <TopLevelDecl* decls>` := p.top } + { birdModuleIdToTypeName(mid) | (Import) `import <ModuleId mid>` <- p.top.imports };
		return [(buildBirdModuleMapper(baseLoc))(tn)  |TypeName tn <- types];
	}
	else
		return [];

};
 
StructuredGraph birdGraphCalculatorAux(TModel model) = calculateFields(model);

StructuredGraph birdGraphCalculatorAux(start[Program] pt) {
 	TModel model = birdTModelFromTree(pt);
 	map[loc, AType] types = getFacts(model);
    return birdGraphCalculator(model);
 }
 
StructuredGraph birdGraphCalculator(list[loc] birdFiles) {
	set[StructuredGraph] graphs = {};
	for (loc birdFile <- birdFiles) {
		start[Program] pt = parseBird(birdFile);
 		TModel model = birdTModelFromTree(pt);
 		map[loc, AType] types = getFacts(model);
 		StructuredGraph current = birdGraphCalculatorAux(model);
    	graphs = graphs + { current };
    }
    return union(graphs);
 } 
  
public start[Program] parseBird(loc file) = parse(#start[Program], file);

public start[Program] sampleBird(str name, PathConfig pcfg) = parse(#start[Program], |project://bird-core/bird-src/<newName>.bird|)
	when newName := replaceAll(name, "::", "/"),
		bprintln(|project://bird-core/bird-src/<newName>.bird|);

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
str toStr(funType(name, _, _, _)) = name;
str toStr(anonType(_)) = "$anon";
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
	
list[str] findModuleId(loc typeLoc , TModel model) {
	loc l = typeLoc in domain(model.useDef) ? getOneFrom(model.useDef[typeLoc]) : typeLoc;
	if (model.facts[l] is anonType)
		return [];
	if (l in model.scopes) {
		scLoc = model.scopes[l];
		if (<_, id, moduleId(), scLoc, _> <- model.defines)
			return split("::", id);
		else
			throw "Struct or choice need to be defined inside a module";
	}
	else
		return [];
}

StructuredGraph calculateFields(TModel model) {
	g = {};
	for (<structScope, id, fieldId(), defined, defType(ty)> <- model.defines) {
		println("{<<structScope, id, defined>>}");
		if (<_, sid, structId(), structScope, _> <- model.defines) {
			g+= <typeName(findModuleId(structScope, model), sid), id, typeName(findModuleId(getTypeIdLoc(ty), model), toStr(model.facts[ty@\loc]))>;
		}
	}
		
	for (loc structScope <- model.anonymousFields) {
		set[Type] types = model.anonymousFields[structScope];
		if (<_, sid, structId(), structScope, _> <- model.defines) {
			TypeName src = typeName(findModuleId(structScope, model), sid);
			int i = 0;
			for (ty <- types) {
				g+= <src, "__anonymous<i>", typeName(findModuleId(getTypeIdLoc(ty), model), toStr(model.facts[ty@\loc]))>;
				i = i + 1;
			}
		}
	}
	
	return g;
}	
