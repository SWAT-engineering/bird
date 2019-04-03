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

list[str] getActualFormals(Formals current, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	[compile(af, (), useDefs, types, index) | af <- current.formals];		
	
list[str] getActualTypeFormals(TypeFormals current, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
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

tuple[str, str] compile(current: (Program) `module <{Id "::"}+ moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= <packageName,
	  "package engineering.swat.formats<packageName>;
      '
      'import static engineering.swat.metal.Let.*;
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
	  '\t<intercalate("\n", [compile(d, useDefs, types, index) | d <- decls])>
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
		 	throw "no corresponding tree has been found for location <l>";
		 };
		 
str compile(current:(TopLevelDecl) `choice <Id id> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
   "public static final Token <id><compiledFormals> <startBlock> <compiledDecls>; <endBlock>"
   when  areThereFormals := (fls <- formals),
		 startBlock := (areThereFormals?"{ return ":"="),
		 endBlock := (areThereFormals?"}":""),
		 list[str] compiledFormalsList := {if (fs  <- formals) getActualFormals(fs, useDefs, types, index); else [];},
		 compiledFormals := {if (fs  <- formals) "(<intercalate(", ", compiledFormalsList)>)"; else "";},
		 declsNumber := size([d| d <-decls]),
		 abstractIds := ["<name>" | (DeclInChoice) `abstract <Type _> <Id name>` <- decls],
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? (([compile(d, useDefs,types, index) | d <-decls])[0]) : "cho(<intercalate(", ", ["\"<id>\""] + [compileDeclInChoice(d, abstractIds, useDefs, types, index) | d <-decls, !((DeclInChoice) `abstract <Type _> <Id name>` := d)])>)"))
		 ; 		 
		 
Type extractType((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`)	= ty;
Type extractType((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = ty;

str extractId((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`) = "<id>";
str extractId((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = "<id>";

loc extractIdLoc((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`) = id@\loc;
loc extractIdLoc((DeclInStruct) `<Type ty> <Id id> = <Expr e>`) = id@\loc;
		 
str compile(current:(TopLevelDecl) `struct <Id id> <TypeFormals typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
   "public static final Token <id><compiledFormalsAndTypePars> <startBlock> <compiledDecls>; <endBlock>"           	
	when areThereFormals := ((fls <- formals) || (typeFormals is withTypeFormals)),
	     startBlock := (areThereFormals?"{ return ":"="),
		 endBlock := (areThereFormals?"}":""),
		 map[str, str] tokenExps := (()|it + (extractId(d):
		 										(((DeclInStruct) `<Type t> <Id i> = <Expr e>` := d)?compile(e, it, useDefs, types, index) :extractId(d))
		 									  )|d <- decls, 
		 							"_" !:= extractId(d),
		 							bprintln("type of <d> is <types[extractIdLoc(d)]>"),
		 							(isUserDefined(types[extractIdLoc(d)]) || structDef(_,_) := types[extractIdLoc(d)])),
		 list[str] compiledFormalsList := {if (fs  <- formals) getActualFormals(fs, useDefs, types, index); else [];},
		 list[str] compiledTypeParsList := getActualTypeFormals(typeFormals, useDefs, types, index),
		 list[str] formalsAndTypePars := compiledTypeParsList + compiledFormalsList,
		 compiledFormalsAndTypePars := {if (size(formalsAndTypePars)!=0) "(<intercalate(", ", formalsAndTypePars)>)"; else "";},
		 declsNumber :=  size([d| d <- decls]),
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? "seq(\"<id>\",  <([compile(d, tokenExps, useDefs,types, index) | d <-decls])[0]>, EMPTY)": "seq(<intercalate(", ", ["\"<id>\""] + [compile(d, tokenExps, useDefs, types, index) | d <-decls])>)"))
		 ;		 

str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> byparsing (<Expr e>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"tie(\"<safeId>\", <compile(ty, tokenExps, useDefs, types, index)>, <compile(e, tokenExps, useDefs, types, index)>)"    
	when safeId := makeSafeId("<id>", id@\loc),
		 bprintln("current in tie: <current>")
		;

str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> <SideCondition? cond>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"rep(\"<safeId>\", <compileDeclInStruct(current, ty, id, args, cond, tokenExps, useDefs, types, index)>)"
	when safeId := makeSafeId("<id>", id@\loc)
		;
		
str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> [<Expr n>] <SideCondition? cond>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"repn(\"<safeId>\", def(\"<safeId>\", con(<size/8>)), <compile(n, tokenExps, useDefs, types, index)>)"
	 //"def(\"<safeId>\", last(mul(con(<size/8>), <compile(n, tokenExps, useDefs, types, index)>))<condStr>)"
	when AType aty := types[ty@\loc],
		 isSimpleByteType(aty),
		 int size := sizeSimpleByteType(aty),
		 safeId := makeSafeId("<id>", id@\loc),
		 condStr := ("" | it + ", <compileSideCondition(aCond, aty, tokenExps, useDefs, types, index)>" |SideCondition aCond <- cond)
		 ;
		 
str compile(current:(DeclInStruct) `<Type ty>[] <DId id> <Arguments? args> [<Expr n>] <SideCondition? cond>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = 
	//"def(\"<safeId>\", last(mul(<compileDeclInStruct(current, ty, id, args, cond, tokenExps, useDefs, types, index)>, <compile(n, tokenExps, useDefs, types, index)>))<condStr>)"
	"repn(\"<safeId>\", <compile(ty, tokenExps, useDefs, types, index)>, <compile(n, tokenExps, useDefs, types, index)>)"
	when AType aty := types[ty@\loc], 
		 !isSimpleByteType(aty),
		 safeId := makeSafeId("<id>", id@\loc),
		 condStr := ("" | it + ", <compileSideCondition(aCond, aty, tokenExps, useDefs, types, index)>" |SideCondition aCond <- cond)
		 ;
		 
		 
str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	compileDeclInStruct(current, ty, id, args, cond, tokenExps, useDefs, types, index);
		
str compile(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	//println("[WARNING] Declaration of computed field case not handled in the generator");
	//throw "Declaration of computed field case not handled in the generator";
	// TODO is it true that this is always a ValueExpression, and therefore a dynamic type?
	= "let(\"<safeId>\", <compile(e, tokenExps, useDefs, types, index)>)"
	when safeId := makeSafeId("<id>", id@\loc),
		 bprintln("TY: <types[ty@\loc]> FOR DECL: <current>"),
		 !(isUserDefined(types[ty@\loc]) ||
		   structDef(_,_) := types[ty@\loc]); // TODO this is workaround due to structDef/structType ambiguity 

str compile(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	= "EMPTY"
	when safeId := makeSafeId("<id>", id@\loc),
		 bprintln("TY: <types[ty@\loc]> FOR DECL: <current>"),
		 (isUserDefined(types[ty@\loc]) ||
		   structDef(_,_) := types[ty@\loc]); // TODO this is workaround due to structDef/structType ambiguity 

		 
str compileDeclInStruct(DeclInStruct current, Type ty, DId id, Arguments? args, SideCondition? cond, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	=  compileType(ty, safeId, args, compiledCond, tokenExps, useDefs, types, index)
	when safeId := makeSafeId("<id>", id@\loc),
		 AType aty := types[ty@\loc],
		 bprintln(ty),
		 compiledCond := ("" | it + ", <compileSideCondition(c, aty, tokenExps, useDefs, types, index)>" | c <- cond);   
	
str compileDeclInChoice((DeclInChoice) `struct { <DeclInStruct* decls>}`, list[str] abstractIds, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	compiledDecls           	
	when declsNumber := size([d | d <- decls]),
		 map[str, str] tokenExps := (()|it + (extractId(d):
		 										(((DeclInStruct) `<Type t> <Id i> = <Expr e>` := d)?compile(e, it, useDefs, types, index) :extractId(d))
		 									  )|d <- decls, (isUserDefined(types[extractIdLoc(d)]) || structDef(_,_) := types[extractIdLoc(d)])),
		 compiledDecls := ((declsNumber == 0)?"EMPTY":
		 	((declsNumber ==  1)? (([compile(d, tokenExps,  useDefs,types,index) | d <-decls])[0]) : "seq(<intercalate(", ", ["\"<safeId>\""] + [compile(d, tokenExps, useDefs, types, index) | d <-decls])>)"))
		 ;

str compileDeclInChoice(current:(DeclInChoice) `<Id typeId>`, list[str] abstractIds, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"seq(<typeId>, <abstracts>)"
	when abstracts := ((size(abstractIds) == 0)?"EMPTY":intercalate(", ", ["let(\"<aid>\",last(ref(\"<typeId>.<aid>\")))" | aid <- abstractIds]));
	
str compileDeclInChoice(current:(DeclInChoice) `<Id typeId> (<{Expr ","}* args>)`, list[str] abstractIds, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"seq(<typeId><exprs>, <abstracts>)"
	when abstracts := ((size(abstractIds) == 0)?"EMPTY":intercalate(", ", ["let(\"<aid>\",last(ref(\"<typeId>.<aid>\")))" | aid <- abstractIds])),
		 exprs := ((_ <- args)?"(<intercalate(", ", [compile(a, tokenExps, useDefs, types, index) | a <- args])>)":"");
	
	

		 
str compileSideCondition((SideCondition) `?(== <Expr e>)`, AType t1, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("==", {t1,t2}, [compile(e, tokenExps, useDefs, types, index)])
	when t2 := types[e@\loc];

str compileSideCondition((SideCondition) `?(!= <Expr e>)`, AType t1,  map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("!=", {t1,t2}, [compile(e, tokenExps, useDefs, types, index)])
	when t2 := types[e@\loc];

default str compileSideCondition(current:(SideCondition) `? ( <Expr e>)`, AType ty, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) 
	= compile(e, tokenExps, useDefs, types, index);
	
default str compileSideCondition(SideCondition sc, AType ty, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index){ throw "Not yet implemented: <sc>"; } 

str compile(current:(Formal) `<Type ty> <Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	= "ValueExpression <safeId>" 
	when safeId := makeSafeId("<id>", current@\loc);

		 
str compile((Arguments)  `( <{Expr ","}* args>  )`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	= "<intercalate(", ", actualArgs)>"
	when actualArgs := [compile(arg, tokenExps, useDefs, types, index) | arg <- args];	 

str compileType(current:(Type)`<UInt v>`, str containerId, Arguments? args, str cond, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	(cond == "")? "def(\"<containerId>\", con(<toInt("<v>"[1..])/BYTE_SIZE>))" : "def(\"<containerId>\", con(<toInt("<v>"[1..])/BYTE_SIZE>) <cond>)";	

str compileType(current:(Type)`<Id id>`, str containerId, Arguments? args, str cond, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"seq(\"<containerId>\", <id><compiledArgs>, EMPTY)"
	when compiledArgs := ((aargs <- args) ? "(<("" | it + compile(aargs, tokenExps, useDefs, types, index) | aargs <- args)>)" : "");
		 
	
str compileType(current:(Type)`<Id id> \< <{ Type ","}* ts> \>`, str containerId, Arguments? args, str cond, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"seq(\"<containerId>\", <id><compiledArgs>, EMPTY)"
	when compiledArgsLst := [compile(aargs, tokenExps, useDefs, types, index) | aargs <- args],
		 bprintln(ts),
		 compiledTypeArgsLst := [compileType(ta, containerId, args, cond, tokenExps, useDefs, types, index) | ta <- ts],
		 compiledArgs := ((_ <- compiledArgsLst + compiledTypeArgsLst)?"(<intercalate(",", compiledArgsLst + compiledTypeArgsLst)>)":"");
	
str compile(current:(Type)`<UInt v>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"nod(<toInt("<v>"[1..])/BYTE_SIZE>)";
	
str compile(current:(Type)`<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"<id>";
		  
str compile(current:(Type)`<Id id> \< <{Type ","}* targs> \>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"<id><args>"
	when //structType(_,ts) := types[current@\loc],
		 size(ts) == size(targs),
		 args := ((t <- targs)?"(<intercalate(", ", [t | t <- targs])>":"");
		 
str compile(current:(Type)`<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"<id>"
	when variableType(_) := types[current@\loc];
	
str compile(current:(Type)`<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	{ throw "BUG!"; }
	when structType(name,_) := types[current@\loc];		 	
	
str compile(current:(SideCondition) `while ( <Expr e>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index){
	throw "Missing implementation for compiling while side condition.";
}

str compile(current:(SideCondition) `? ( <ComparatorOperator uo> <Expr e>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	= compile(uo, compile(e, tokenExps, useDefs, types, index), tokenExps, useDefs, types, index);

default str compile(current:(SideCondition) `? ( <UnaryOperator uo> <Expr e>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index)
	= compile(uo, compile(e, tokenExps, useDefs, types, index), tokenExps, useDefs, types, index)
	;

str compile(current:(ComparatorOperator) `\>=`, str s, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "gtEqNum(<s>)";

str compile(current:(UnaryOperator) `==`, str s, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "eq(<s>)";

str compile(current:(UnaryOperator) `!=`, str s, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "not(eq(<s>))";

str compile(current: (Expr) `<Expr e>.as[<Type t>]`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = compile(e, tokenExps, useDefs, types, index);

str compile(current: (Expr) `<StringLiteral lit>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "con(<lit>)";

str compile(current: (Expr) `<HexIntegerLiteral nat>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "con(<nat>)";

str compile(current: (Expr) `<BitLiteral nat>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "con(<noUnderscoreNat>)"
	when noUnderscoreNat := replaceAll("<nat>","_","");

str compile(current: (Expr) `<NatLiteral nat>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "con(<nat>)";

str compile(current: (Expr) `(<Expr e>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = compile(e, tokenExps, useDefs, types, index)
	when bprintln(e);
	
/*str compile(current: (Expr) `parse (<Expr e>) with <Type t>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = 
	"tie(<compiledType>, <compiledExpr>)"
	when compiledType := compile(t, tokenExps, useDefs, types, index),
		 compiledExpr := compile(e, tokenExps, useDefs, types, index);*/

str compile(current: (Expr) `<Id id> ( <{Expr ","}* exprs>)`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "new <javaId>().apply(<intercalate(", ", [compile(e, tokenExps, useDefs, types, index) | Expr e <- exprs])>)"
    when loc funLoc := Set::getOneFrom((useDefs[id@\loc])),
    	 funType(_,_,_,javaId) := types[funLoc];

str compile(current: (Expr) `<Expr e1> - <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "<getInfixOperator("-")>(<compile(e1, tokenExps, useDefs, types, index)>, <compile(e2, tokenExps, useDefs, types, index)>)";
    
str compile(current: (Expr) `<Expr e1> + <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "<getInfixOperator("+")>(<compile(e1, tokenExps, useDefs, types, index)>, <compile(e2, tokenExps, useDefs, types, index)>)";    

str compile(current: (Expr) `<Expr e1> * <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "<getInfixOperator("*")>(<compile(e1, tokenExps, useDefs, types, index)>, <compile(e2, tokenExps, useDefs, types, index)>)";    


str compile(current: (Expr) `<Expr e1> ++ <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "<getInfixOperator("++")>(<compile(e1, tokenExps, useDefs, types, index)>, <compile(e2, tokenExps, useDefs, types, index)>)";
    
str compile(current: (Expr) `<Expr e1> (+) <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "<getInfixOperator("(+)")>(<compile(e1, tokenExps, useDefs, types, index)>, <compile(e2, tokenExps, useDefs, types, index)>)";    
        

str compile(current: (Expr) `<Id id1>.<Id id2>.<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "last(ref(\"<initialId>.<id2>.<id>\"))"
    when lo := ([l | l <- useDefs[id1@\loc]])[0],
	 	 fixedLo := (("<id1>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
		 srcId := "<index(fixedLo)>",
		 initialId := (("<id1>" in tokenExps)?tokenExps["<id1>"]:makeSafeId("<srcId>", fixedLo));

str compile(current: (Expr) `<Id id1>.<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
   	"fold(ref(\"<initialId>.<tid>.<id>.<id>\"), Shorthand::cat)"
    when t:listType(_) := types[current@\loc],
    	 !t.bounded,
    	 lo := ([l | l <- useDefs[id1@\loc]])[0],
         fixedLo := (("<id1>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
         srcId := "<index(fixedLo)>",
         structType(tid, _) := types[fixedLo],
         initialId := ("<id1>" in tokenExps?tokenExps["<id1>"]:makeSafeId("<srcId>", fixedLo));    
         
str compile(current: (Expr) `<Id id1>.<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
    "last(ref(\"<initialId>.<tid>.<id>\"))" 
    when //listType(_) !:= types[current@\loc],
    	 lo := ([l | l <- useDefs[id1@\loc]])[0],
         fixedLo := (("<id1>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
         srcId := "<index(fixedLo)>",
         structType(tid, _) := types[fixedLo],
         initialId := ("<id1>" in tokenExps?tokenExps["<id1>"]:makeSafeId("<srcId>", fixedLo));              
         
//str compile(current: (Expr) `<Id id1>.<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
//    "seq(let(\"<newId>\", <initialExp>),last(ref(\"<newId>.<tid>.<id>\")))" 
//    when lo := ([l | l <- useDefs[id1@\loc]])[0],
//         fixedLo := (("<id1>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
//         bprintln("attempting to generate for <current>"),
//         srcId := "<index(fixedLo)>",33
//         structType(tid, _) := types[fixedLo],
//         Expr id1AsExp := ((Expr) `<Id id1>`)[@\loc = id1@\loc],
//         initialExp := compile(id1AsExp,  tokenExps, useDefs, types, index),
//         newId := "__tmp"
    	 
str compile(current: (Expr) e, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index){
    throw "Operation not yet implemented: <e>";
}    	 

str compile(current: (Expr) `[ <{Expr ","}* es>]`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = "con(<intercalate(", ",["<e>" | e <- es])>)"
	when listType(ty) := types[current@\loc]; 
	
str compile(current:(Expr)`<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	"fold(ref(\"<id>.<id>\"), Shorthand::cat)"
	when t:listType(_) := types[current@\loc],
	     !t.bounded;

str compile(current: (Expr) `<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = 
	"last(<id>)" 
	when //listType(_) !:= types[current@\loc],
		 
		 lo := ([l | l <- useDefs[id@\loc]])[0],
	 	 fixedLo := (("<id>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
	 	 Formal f := index(fixedLo);
		

str compile(current: (Expr) `<Id id>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) = 
	"last(ref(\"<makeSafeId("<srcId>", fixedLo)>\"))"
	when //listType(_) !:= types[current@\loc],
		 lo := ([l | l <- useDefs[id@\loc]])[0],
	 	 fixedLo := (("<id>" in {"this", "it"}) ? (lo[length=lo.length-1][end=<lo.end.line, lo.end.column-1>]) : lo),
		 srcId := index(fixedLo),
		 Formal f !:= srcId;
		 
str compile(current: (Expr) `<Expr e1> <ComparatorOperator uo> <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("<uo>", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> <EqualityOperator uo> <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("<uo>", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when bprintln(e1),
		 t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
		 
str compile(current: (Expr) `<Expr e1> && <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("&&", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\> <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\>\> <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \<\< <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("\<\<", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
		 
str compile(current: (Expr) `<Expr e1> || <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("||", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> & <Expr e2>`, map[str, str] tokenExps, rel[loc,loc] useDefs, map[loc, AType] types, Tree(loc) index) =
	calculateOp("&", {t1,t2}, [compile(e1, tokenExps, useDefs, types, index), compile(e2, tokenExps, useDefs, types, index)])
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
    return compile(pt.top, useDefs, types);
}

void compileBirdTo(str name, loc file) {
    <_,text> = compileBird(name);
    //println(text);
    writeFile(file, text);
}
