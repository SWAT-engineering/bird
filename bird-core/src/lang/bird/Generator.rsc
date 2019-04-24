module lang::bird::Generator

import IO;

import lang::bird::Syntax;
import lang::bird::Checker;
import analysis::graphs::Graph;


import List;
import Set;
import String;

extend analysis::typepal::TypePal;

syntax Aux = "{" SideCondition? sc "}";

int BYTE_SIZE = 8;

bool isSimpleByteType(uType(_)) = true;
bool isSimpleByteType(sType(_)) = true;
bool isSimpleByteType(AType _) = false;

int sizeSimpleByteType(uType(n)) = n;
int sizeSimpleByteType(sType(n)) = n;  
int sizeSimpleByteType(AType ty){ throw "Incorrect operation on type <prettyPrintAType(ty)>"; }

str calculateEq({intType()}) = "eqNum";
str calculateEq({strType()}) = "eqStr";
str calculateEq({strType(), uType(_)}) =  "eq";
str calculateEq({strType(), sType(_)}) = "eq";
str calculateEq({intType(), uType(_)}) = "eq";
str calculateEq({intType(), sType(_)}) = "eq";	
str calculateEq({sType(_)}) = "eq";
str calculateEq({uType(_)}) = "eq";
str calculateEq({uType(_), listType(intType())}) = "eq";

str calculateOp("==", set[AType] ts, list[str] es) = "<calculateEq(ts)>(<intercalate(",", es)>)";
str calculateOp("!=", set[AType] ts, list[str] es) = "not(<calculateEq(ts)>(<intercalate(",", es)>))";
str calculateOp("&&", set[AType] ts, list[str] es) = "and(<intercalate(",", es)>)";
str calculateOp("||", set[AType] ts, list[str] es)= "or(<intercalate(",", es)>)"; 
str calculateOp("&", set[AType] ts, list[str] es) = "and(<intercalate(",", es)>)";
str calculateOp("|", set[AType] ts, list[str] es) = "or(<intercalate(",", es)>)";
str calculateOp("\>\>", set[AType] ts, list[str] es) = "shr(<intercalate(",", es)>)";
str calculateOp("\<\<", set[AType] ts, list[str] es) = "shl(<intercalate(",", es)>)";
str calculateOp("\>", set[AType] ts, list[str] es) = "gtNum(<intercalate(",", es)>)";
str calculateOp("\<", set[AType] ts, list[str] es) = "ltNum(<intercalate(",", es)>)";
str calculateOp("\>=", set[AType] ts, list[str] es) = "gtEqNum(<intercalate(",", es)>)";
str calculateOp("\<=", set[AType] ts, list[str] es) = "ltEqNum(<intercalate(",", es)>)";
str calculateOp("&", set[AType] ts, list[str] es) = "and(<intercalate(",", es)>)";
	

default str calculateOp(str other, set[AType] ts){ throw "generation for operator <other> not yet implemented"; }

list[str] getActualFormals(Formals current, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	[compile(af, parentId, (), useDefs, types, index, scopes, defines) | af <- current.formals];		
	
list[str] getActualTypeFormals(TypeFormals current, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= actualTypePars
	when typeFormalList := (current is noTypeFormals ? [] : [f | f <- current.formals]),
		 actualTypePars := ["Token <tp>" | Id tp <- typeFormalList];

str getInfixOperator("-") = "sub";
str getInfixOperator("(+)") = "cat";
str getInfixOperator("+") = "add";
str getInfixOperator("*") = "mul";
str getInfixOperator("++") = "cat";

//default str calculateEq(set[AType] ts) { throw "Incorrect arguments to calculateEq: <ts>"; }

bool biprintln(value v){
	iprintln(v);
	return true;
} 

str makeSafeId(str id, loc lo) =
	//"<newId>_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.end.line>_<lo.begin.column>_<lo.end.column>"
	"<newId>"
	when newId := (("<id>"=="_")?"dummy":"<id>");

tuple[str, str] compile(current: (Program) `module <{Id "::"}+ moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types, map[loc,str] scopes, map[loc,Define] defines)
	= <packageName,
	  "package engineering.swat.formats<packageName>;
      '
      'import static engineering.swat.metal.Let.*;
      'import engineering.swat.metal.StructWrapperExpression;
      '
      'import io.parsingdata.metal.token.Token;
	  'import io.parsingdata.metal.expression.value.ValueExpression;
	  'import io.parsingdata.metal.Shorthand;
	  '
	  'import static io.parsingdata.metal.token.Token.EMPTY_NAME;
	  'import static io.parsingdata.metal.Shorthand.*;
	  '
	  'public class <className> {
	  '\tprivate <className>(){}
	  '\t<intercalate("\n", [compile(d, useDefs, types, index, scopes, defines) | d <- decls])>
	  '}">
	when [dirs*, className] := [x | x <-moduleName],
		 str packageName := ((size(dirs) == 0)? "": ("."+ intercalate(".", dirs))),
		 map[loc, TopLevelDecl] declsMap := (d@\loc: d | d <- decls),
		 list[loc] tmpLos := [lo | lo <- order(useDefs), lo in declsMap],
		 set[loc] los :=  domain(declsMap) - toSet(tmpLos),
		 bprintln(intercalate("\n", tmpLos)),
		 Tree(loc) index := Tree(loc l){
		 	visit (current){
		 		case Tree t: {
		 			if (t has \loc){
		 				if (l == t@\loc){
		 				 	return t;
		 				}
		 			}
		 		}
		 	};
		 	println(l);
		 	throw "no corresponding tree has been found for location <l>";
		 };
		 
str compile(current:(TopLevelDecl) `choice <Id id> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
   "<wrapper>
   '
   'public static final Token <id>(<intercalate(", ", compiledFormals)>) {
   '	return <compiledDecls>;
   '}"
   when  nameAndTypesFormals := [<"<name>", ty, isUserDefined(types[ty@\loc])> | fls <- formals, (Formal) `<Type ty> <Id name>` <- fls.formals],
		 list[str] compiledFormals := ["String base"] + ["<isUserDefined?"StructWrapperExpression":"ValueExpression"> <name>"| <name, _, isUserDefined> <- nameAndTypesFormals],
		 //Tree constructorFakeTree := newConstructorId(id, id@\loc),
		 //consType(atypeList(atypes)) := types[constructorFakeTree@\loc],
		 str wrapper := compileWrapper("<id>", decls, types),
		 declsNumber := size([d| d <-decls]),
		 abstractIds := ["<name>" | (DeclInChoice) `abstract <Type _> <Id name>` <- decls],
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? (([compile(d, useDefs,types, index, scopes, defines) | d <-decls])[0]) : "cho(<intercalate(", ", ["\"<id>\""] + [compileDeclInChoice(d, id, abstractIds, useDefs, types, index, scopes, defines) | d <-decls, (DeclInChoice) `abstract <Type _> <Id name>` !:= d])>)"))
		 ; 		 
		 
Type extractType((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`)	= ty;
Type extractType((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = ty;

str extractId((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`) = "<id>";
str extractId((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = "<id>";

loc extractIdLoc((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`) = id@\loc;
loc extractIdLoc((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = id@\loc;

str compileWrapper(str id, DeclInStruct* decls, map[loc, AType] types) =
	"public static final StructWrapperExpression __<id>(String base){
	'	return new StructWrapperExpression(new String[] { <ids> }, new ValueExpression[] { <exprs> });
	'}"
	when nameAndTypes := [<"<d.id>", d.ty, consType(_) := types[d.ty@\loc]> | DeclInStruct d <- decls],
		 bprintln([<name, "<ty>", types[ty@\loc], isUD> | <name, ty, isUD> <- nameAndTypes]),
		 //computedFormals := intercalate(", ", ["String base"] + 
		 //								["<isUserDefined?"StructWrapperExpression":"ValueExpression"> <name>"| <name, _, isUserDefined> <- nameAndTypes]),
		 ids := intercalate(", ", ["\"<name>\""| <name, _, _> <- nameAndTypes]),
		 // TODO: here we assume user-defined equals structs
		 exprs := intercalate(", ", 
		 	[] //["<name>" | <name, ty, isUserDefined> <- nameAndTypes]
		 	+ [isUserDefined?"__<ty>(base+\"<id>.<name>.\")":"ref(base + \"<id>.<name>\")" | <name, ty, isUserDefined> <- nameAndTypes])
		 ;
		 
str compileWrapper(str id, DeclInChoice* decls, map[loc, AType] types) =
	"public static final StructWrapperExpression __<id>(String base){
	'	return new StructWrapperExpression(new String[] { <ids> }, new ValueExpression[] { <exprs> });
	'}"
	when nameAndTypes := [<"<id>", tp, consType(_) := types[tp@\loc]> | (DeclInChoice) `abstract <Type tp> <Id id>` <- decls],
		 ids := intercalate(", ", ["\"<name>\""| <name, _, _> <- nameAndTypes]),
		 // TODO: here we assume user-defined equals structs
		 exprs := intercalate(", ", 
		 	[] //["<name>" | <name, ty, isUserDefined> <- nameAndTypes]
		 	+ [isUserDefined?"__<ty>(base+\"<id>.<name>.\")":"ref(base + \"<id>.<name>\")" | <name, ty, isUserDefined> <- nameAndTypes])
		 ;
		 
		 
		 
str compile(current:(TopLevelDecl) `struct <Id id> <TypeFormals typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc,Define] defines) =
   "<wrapper>
   '
   'public static final Token <id>(<intercalate(", ", formalsAndTypePars)>) {
   '	return <compiledDecls>;
   '}"           	
	when map[str, str] tokenExps := (()|it + (extractId(d):
		 										(((DeclInStruct) `<Type t> <Id i> = <Expr e>` := d)?compile(e, id, it, useDefs, types, index, scopes, defines) :extractId(d))
		 									  )|d <- decls, 
		 							"_" !:= extractId(d),
		 							bprintln("type of <d> is <types[extractIdLoc(d)]>"),
		 							(isUserDefined(types[extractIdLoc(d)]) || structDef(_,_) := types[extractIdLoc(d)])),
		 nameAndTypesFormals := [<"<name>", ty, isUserDefined(types[ty@\loc])> | fls <- formals, (Formal) `<Type ty> <Id name>` <- fls.formals],
		 list[str] compiledTypeParsList := getActualTypeFormals(typeFormals, useDefs, types, index, scopes, defines),
		 list[str] compiledFormals := ["String base"] + ["<isUserDefined?"StructWrapperExpression":"ValueExpression"> <name>"| <name, _, isUserDefined> <- nameAndTypesFormals],
		 list[str] formalsAndTypePars := compiledFormals + compiledTypeParsList,
		 //Tree constructorFakeTree := newConstructorId(id, id@\loc),
		 //consType(atypeList(atypes)) := types[constructorFakeTree@\loc],
		 str wrapper := compileWrapper("<id>", decls, types),
		 declsNumber :=  size([d| d <- decls]),
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? "seq(\"<id>\",  <([compile(d, id, tokenExps, useDefs , types, index, scopes, defines) | d <-decls])[0]>, EMPTY)": "seq(<intercalate(", ", ["\"<id>\""] + [compile(d, id, tokenExps, useDefs, types, index, scopes, defines) | d <-decls])>)"))
		 ;		 

// TODO Fix ugly workaround to pass an empty condition
str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> byparsing (<Expr e>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"tie(\"<safeId>\", <compileDeclInStruct(current, ty, id, args, emptyCondition, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)>)"    
	when safeId := makeSafeId("<id>", id@\loc),
		 emptyCondition := ([DeclInStruct] "<ty> <id> <args>").sideCondition,
		 bprintln("current in tie: <current>")
		;

str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> <SideCondition? cond>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"rep(\"<safeId>\", <compileDeclInStruct(current, ty, id, args, cond, parentId, tokenExps, useDefs, types, index, scopes, defines)>)"
	when safeId := makeSafeId("<id>", id@\loc)
		;
		
str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> [<Expr n>] <SideCondition? cond>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"repn(\"<safeId>\", def(\"<safeId>\", con(<size/8>)), first(<compile(n, parentId, tokenExps, useDefs, types, index, scopes, defines)>))"
	 //"def(\"<safeId>\", last(mul(con(<size/8>), <compile(n, tokenExps, useDefs, types, index, scopes, defines)>))<condStr>)"
	when AType aty := types[ty@\loc],
		 isSimpleByteType(aty),
		 int size := sizeSimpleByteType(aty),
		 safeId := makeSafeId("<id>", id@\loc),
		 condStr := ("" | it + ", <compileSideCondition(aCond, aty, tokenExps, useDefs, types, index, scopes, defines)>" |SideCondition aCond <- cond)
		 ;
		 
str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> [<Expr n>] <SideCondition? cond>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = 
	//"def(\"<safeId>\", last(mul(<compileDeclInStruct(current, ty, id, args, cond, tokenExps, useDefs, types, index, scopes, defines)>, <compile(n, tokenExps, useDefs, types, index, scopes, defines)>))<condStr>)"
	"repn(\"<safeId>\", <compileDeclInStruct(current, ty, id, args, cond, parentId, tokenExps, useDefs, types, index, scopes, defines)>, first(<compile(n, parentId, tokenExps, useDefs, types, index, scopes, defines)>))"
	when AType aty := types[ty@\loc], 
		 !isSimpleByteType(aty),
		 safeId := makeSafeId("<id>", id@\loc),
		 condStr := ("" | it + ", <compileSideCondition(aCond, aty, tokenExps, useDefs, types, index, scopes, defines)>" |SideCondition aCond <- cond)
		 ;
		 
		 
str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	compileDeclInStruct(current, ty, id, args, cond, parentId, tokenExps, useDefs, types, index, scopes, defines);
		
str compile(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	//println("[WARNING] Declaration of computed field case not handled in the generator");
	//throw "Declaration of computed field case not handled in the generator";
	// TODO is it true that this is always a ValueExpression, and therefore a dynamic type?
	= "let(\"<safeId>\", <compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)>)"
	when safeId := makeSafeId("<id>", id@\loc),
		 bprintln("TY: <types[ty@\loc]> FOR DECL: <current>"),
		 !(isUserDefined(types[ty@\loc]) ||
		   structDef(_,_) := types[ty@\loc]); // TODO this is workaround due to structDef/structType ambiguity 

str compile(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= "EMPTY"
	when safeId := makeSafeId("<id>", id@\loc),
		 bprintln("TY: <types[ty@\loc]> FOR DECL: <current>"),
		 (isUserDefined(types[ty@\loc]) ||
		   structDef(_,_) := types[ty@\loc]); // TODO this is workaround due to structDef/structType ambiguity 

		 
str compileDeclInStruct(DeclInStruct current, Type ty, DId id, Arguments? args, SideCondition? cond, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	=  compileType(ty, safeId, args, compiledCond, parentId, tokenExps, useDefs, types, index, scopes, defines)
	when safeId := makeSafeId("<id>", id@\loc),
		 AType aty := types[ty@\loc],
		 bprintln(ty),
		 compiledCond := ("" | it + ", <compileSideCondition(c, aty, parentId, tokenExps, useDefs, types, index, scopes, defines)>" | c <- cond);   
	
str compileDeclInChoice((DeclInChoice) `struct { <DeclInStruct* decls>}`, Id parentId, list[str] abstractIds, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	compiledDecls           	
	when declsNumber := size([d | d <- decls]),
		 map[str, str] tokenExps := (()|it + (extractId(d):
		 										(((DeclInStruct) `<Type t> <Id i> = <Expr e>` := d)?compile(e, it, useDefs, types, index, scopes, defines) :extractId(d))
		 									  )|d <- decls, (isUserDefined(types[extractIdLoc(d)]) || structDef(_,_) := types[extractIdLoc(d)])),
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? (([compile(d, parentId, tokenExps, useDefs, types, index, scopes, defines) | d <-decls])[0]) : "seq(<intercalate(", ", ["\"<safeId>\""] + [compile(d, parentId, tokenExps, useDefs, types, index, scopes, defines) | d <-decls])>)"))
		 ;

str compileDeclInChoice(current:(DeclInChoice) `<Id typeId>`, Id parentId, list[str] abstractIds, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"seq(<typeId>(base + \"<parentId>.\"), <abstracts>)"
	when abstracts := ((size(abstractIds) == 0)?"EMPTY":intercalate(", ", 
		["let(\"<aid>\",ref(base + \"<parentId>.<typeId>.<aid>\"))" | aid <- abstractIds]));
	
str compileDeclInChoice(current:(DeclInChoice) `<Id typeId> (<{Expr ","}* args>)`, Id parentId, list[str] abstractIds, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"seq(<typeId><exprs>, <abstracts>)"
	when abstracts := ((size(abstractIds) == 0)?"EMPTY":intercalate(", ", ["let(\"<aid>\",ref(base + \"<parentId>.<typeId>.<aid>\"))" | aid <- abstractIds])),
		 exprs := ((_ <- args)?"(<intercalate(", ", ["base + \"<parentId>.\""] +[compile(a, parentId, tokenExps, useDefs, types, index, scopes, defines) | a <- args])>)":"");
	
	

		 
str compileSideCondition((SideCondition) `?(== <Expr e>)`, AType t1, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("==", {t1,t2}, [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t2 := types[e@\loc];

str compileSideCondition((SideCondition) `?(!= <Expr e>)`, AType t1,  map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("!=", {t1,t2}, [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t2 := types[e@\loc];

default str compileSideCondition(current:(SideCondition) `? ( <Expr e>)`, AType ty, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) 
	= compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines);
	
default str compileSideCondition(SideCondition sc, AType ty, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines){ throw "Not yet implemented: <sc>"; } 

str compile(current:(Formal) `<Type ty> <Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= "<safeId>" 
	when safeId := makeSafeId("<id>", current@\loc);

		 
str compile((Arguments)  `( <{Expr ","}* args>  )`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= "<intercalate(", ", actualArgs)>"
	when actualArgs := ["<compile(arg, parentId, tokenExps, useDefs, types, index, scopes, defines)>" | arg <- args];	 

str compileType(current:(Type)`<UInt v>`, str containerId, Arguments? args, str cond, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	(cond == "")? "def(\"<containerId>\", con(<toInt("<v>"[1..])/BYTE_SIZE>))" : "def(\"<containerId>\", con(<toInt("<v>"[1..])/BYTE_SIZE>) <cond>)";	

str compileType(current:(Type)`<Id id>`, str containerId, Arguments? args, str cond, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"seq(\"<containerId>\", <id>(<compiledArgs>), EMPTY)" 
	when compiledArgsLst := [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines) | aargs <- args, Expr e <- aargs.args],
		 compiledArgs := intercalate(", ", ["base + \"<parentId>.<containerId>.\""] + compiledArgsLst);
	
str compileType(current:(Type)`<Id id> \< <{ Type ","}* ts> \>`, str containerId, Arguments? args, str cond, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"seq(\"<containerId>\", <id>(<compiledArgs>), EMPTY)"
	when 
		 compiledTypeArgsLst := [compileType(ta, containerId, args, cond, parentId, tokenExps, useDefs, types, index, scopes, defines) | ta <- ts],
		 compiledArgsLst := [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines) | aargs <- args, Expr e <- aargs.args],
		 compiledArgs := intercalate(", ", ["base + \"<parentId>.<containerId>.\""] + compiledTypeArgsLst + compiledArgsLst);
	
str compile(current:(Type)`<UInt v>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"nod(<toInt("<v>"[1..])/BYTE_SIZE>)";
	
str compile(current:(Type)`<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"<id>";
		  
str compile(current:(Type)`<Id id> \< <{Type ","}* targs> \>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"<id><args>"
	when //structType(_,ts) := types[current@\loc],
		 size(ts) == size(targs),
		 args := ((t <- targs)?"(<intercalate(", ", [t | t <- targs])>":"");
		 
str compile(current:(Type)`<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"<id>"
	when variableType(_) := types[current@\loc];
	
str compile(current:(Type)`<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	{ throw "BUG!"; }
	when structType(name,_) := types[current@\loc];		 	
	
str compile(current:(SideCondition) `while ( <Expr e>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines){
	throw "Missing implementation for compiling while side condition.";
}

str compile(current:(SideCondition) `? ( <ComparatorOperator uo> <Expr e>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= compile(uo, compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines), parentId, tokenExps, useDefs, types, index, scopes, defines);

default str compile(current:(SideCondition) `? ( <UnaryOperator uo> <Expr e>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines)
	= compile(uo, compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines), parentId, tokenExps, useDefs, types, index, scopes, defines)
	;

str compile(current:(ComparatorOperator) `\>=`, str s, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "gtEqNum(<s>)";

str compile(current:(UnaryOperator) `==`, str s, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "eq(<s>)";

str compile(current:(UnaryOperator) `!=`, str s, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "not(eq(<s>))";

str compile(current: (Expr) `<Expr e>.as[int]`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = 
	"sub(<compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)>, 0x30)";

default str compile(current: (Expr) `<Expr e>.as[<Type t>]`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines);

str compile(current: (Expr) `<StringLiteral lit>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "con(<lit>)";

str compile(current: (Expr) `<HexIntegerLiteral nat>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "con(<nat>)";

str compile(current: (Expr) `<BitLiteral nat>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "con(<noUnderscoreNat>)"
	when noUnderscoreNat := replaceAll("<nat>","_","");

str compile(current: (Expr) `<NatLiteral nat>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "con(<nat>)";

str compile(current: (Expr) `(<Expr e>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)
	when bprintln(e);
	
/*str compile(current: (Expr) `parse (<Expr e>) with <Type t>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = 
	"tie(<compiledType>, <compiledExpr>)"
	when compiledType := compile(t, parentId, tokenExps, useDefs, types, index, scopes, defines),
		 compiledExpr := compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines);*/

str compile(current: (Expr) `<Id id> ( <{Expr ","}* exprs>)`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "new <javaId>().apply(<intercalate(", ", [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines) | Expr e <- exprs])>)"
    when loc funLoc := Set::getOneFrom((useDefs[id@\loc])),
    	 funType(_,_,_,javaId) := types[funLoc];

str compile(current: (Expr) `<Expr e1> - <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<getInfixOperator("-")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";
    
str compile(current: (Expr) `<Expr e1> + <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<getInfixOperator("+")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    

str compile(current: (Expr) `<Expr e1> * <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<getInfixOperator("*")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    


str compile(current: (Expr) `<Expr e1> ++ <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<getInfixOperator("++")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";
    
str compile(current: (Expr) `<Expr e1> (+) <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<getInfixOperator("(+)")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    
        

/*str compile(current:(Expr)`<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	"fold(ref(\"<id>.<id>\"), Shorthand::cat)"
	when t:listType(_) := types[current@\loc],
	     !t.bounded;
*/		 

str compile(current: (Expr) `<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	compileRef("<id>", srcLoc, parentId, tokenExps, useDefs, types, index, scopes, defines)
	when "<id>" notin {"this", "it"},
		 srcLoc := getOneFrom(useDefs[current@\loc]);
		 
str compile(current: (Expr) `this`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	compileRef(srcId, srcLoc, parentId, tokenExps, useDefs, types, index, scopes, defines)
	when srcLoc := getOneFrom(useDefs[current@\loc]),
		 srcId := "<index(srcLoc)>";		 
		 
str compileRef(str id, loc srcLoc, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) {
	println("reference for <id>");
	if (srcLoc in defines && <_, _, paramId(), _, _> := defines[srcLoc]) {
		return id;
	}
	else if (structType(ty, _) := types[srcLoc]) {
		return compileUserDefinedRef(ty, id, parentId);
	}
	else if (t:listType(_) := types[srcLoc]) {
		if (t.bounded)
			return compileArrayRef(id, parentId);
		else
			throw "Referencing unbounded arrays is not yet implemented";	
	}
	else {
		return compileSimpleRef(id, parentId);
	}
		
}
		 
str compileArrayRef(str id, Id parentId) = "fold(ref(base + \"<parentId>.<id>.<id>\"), Shorthand::cat)";
		  
str compileSimpleRef(str id, Id parentId) = "ref(base + \"<parentId>.<id>\")";

str compileUserDefinedRef(str ty, str id, Id parentId) = "__<ty>(base + \"<parentId>.<id>.\")";

str compile(current: (Expr) `<Expr e>.<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
    "<compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines)>.getField(\"<id>\")"
	when structType(ty, _) := types[e@\loc];
	
str compile(current: (Expr) `<Expr e>.<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = {
		throw "Not yet implemented";
	}
	when structType(ty, _) !:= types[e@\loc];	
     
        
         
//str compile(current: (Expr) `<Id id1>.<Id id>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
//    "seq(let(\"<newId>\", <initialExp>),last(ref(\"<newId>.<tid>.<id>\")))" 
//    when lo := ([l | l <- useDefs[id1@\loc]])[0],
//         fixedLo := (("<id1>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
//         bprintln("attempting to generate for <current>"),
//         srcId := "<index(fixedLo)>",33
//         structType(tid, _) := types[fixedLo],
//         Expr id1AsExp := ((Expr) `<Id id1>`)[@\loc = id1@\loc],
//         initialExp := compile(id1AsExp,  tokenExps, useDefs, types, index, scopes, defines),
//         newId := "__tmp"
    	 
str compile(current: (Expr) e, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines){
    throw "Operation not yet implemented: <e>";
}    	 

str compile(current: (Expr) `[ <{Expr ","}* es>]`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) = "con(<intercalate(", ",["<e>" | e <- es])>)"
	when listType(ty) := types[current@\loc]; 
	
		 
str compile(current: (Expr) `<Expr e1> <ComparatorOperator uo> <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("<uo>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> <EqualityOperator uo> <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("<uo>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when bprintln(e1),
		 t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
		 
str compile(current: (Expr) `<Expr e1> && <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("&&", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\> <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\>\> <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \<\< <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("\<\<", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
		 
str compile(current: (Expr) `<Expr e1> || <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("||", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> & <Expr e2>`, Id parentId, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index, map[loc,str] scopes, map[loc, Define] defines) =
	calculateOp("&", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];		 
		 
str type2Java(AType t) = "ValueExpression"
	when isTokenType(t);	  
str type2Java(intType()) = "int";
str type2Java(strType()) = "String";
str type2Java(boolType()) = "boolean";
str type2Java(listType(t)) = "List\<<type2Java(t)>\>"
	when !isTokenType(t);	  
            	

public start[Program] sampleBird(str name) = parse(#start[Program], |project://bird-core/bird-src/<name>.bird|);

tuple[str,str] compileBird(str name) {
	start[Program] pt = sampleBird(name);
	return compileBird(pt);
}

tuple[str,str] compileBird(start[Program] pt) {
	TModel model = birdTModelFromTree(pt);
    map[loc, AType] types = getFacts(model);
    rel[loc, loc] useDefs = getUseDef(model);
    Defines defines = model.defines;
	//alias Define  = tuple[loc scope, str id, IdRole idRole, loc defined, DefInfo defInfo];
	//alias Defines = set[Define];                                 // All defines

    map[loc, str] scopes = ();
    map[loc, Define] definesMap = ();
    for (Define d <- defines) {
    	if (d.scope in types, structDef(name, _) := types[d.scope]) {
    		scopes += (d.defined : name);
    	}
    	definesMap += (d.defined : d);
    }
    
    return compile(pt.top, useDefs, types, scopes, definesMap);
}

void compileBirdTo(str name, loc file) {
    <_,text> = compileBird(name);
    //println(text);
    writeFile(file, text);
}
