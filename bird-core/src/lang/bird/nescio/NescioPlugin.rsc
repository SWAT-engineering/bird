module lang::bird::nescio::NescioPlugin

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::nescio::API;
import util::Reflective;

import ParseTree;

StructuredGraph birdGraphCalculator(str moduleName, PathConfig pcfg) {
 	start[Program] pt = sampleBird(moduleName, pcfg);
 	TModel model = birdTModelFromTree(pt);
 	map[loc, AType] types = getFacts(model);
    rel[loc, loc] useDefs = getUseDef(model);
    return calculateFields(pt.top, useDefs, types);
 }
  
public start[Program] sampleBird(str name) = parse(#start[Program], |project://bird-core/bird-src/<name>.bird|);  
  

//alias Fields = rel[str typeName, str field, str fieldType];
StructuredGraph calculateFields((Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types)  
	= ({} | it + calculateFields(moduleName, decls) | d <- decls);
	
StructuredGraph calculateFields(current:(TopLevelDecl) `struct <Id sid> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, ModuleId currentModule, rel[loc,loc] useDefs, map[loc, AType] types) =
	{};
	
