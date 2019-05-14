module lang::bird::Checker

import lang::bird::Syntax;
import util::Math;
import ListRelation;
import Set;
import String;

import IO;
import util::Maybe;

extend analysis::typepal::TypePal;
extend analysis::typepal::TestFramework;

lexical ConsId =  "$" ([a-z A-Z 0-9 _] !<< [a-z A-Z _][a-z A-Z 0-9 _]* !>> [a-z A-Z 0-9 _])\Reserved;

data AType
	= voidType()
	| intType()
	| typeType(AType ty)
	| strType()
	| boolType()
	| listType(AType ty, Maybe[Expr] n = nothing())
	| consType(AType formals)
	| funType(str name, AType returnType, AType formals, str javaRef)
	| structDef(str name, list[str] typeFormals)
	| structType(str name, list[AType] typeActuals)
	| anonType(lrel[str, AType] fields)
	| byteType()
	| moduleType()
	| variableType(str s)
	;
	
data IdRole
    = structId()
    | fieldId()
    | paramId()
    | typeVariableId()
    | variableId()
    | consId()
    | moduleId()
    | funId()
    ;
    
data PathRole
    = importPath()
    ;
    	
//bool birdIsSubType(AType _, topTy()) = true;
//bool birdIsSubType(_), refType("Token"))) = true;
//bool birdIsSubType(AType t1, AType t2) = true
//	when t1 == t2;
//default bool birdIsSubType(AType _, AType _) = false;

bool isSubtype(atypeList(vs), atypeList(ws))
	= (true | isSubtype(v, w) && it | <v,w> <- zip(vs, ws))
	when (size(vs) == size(ws));
bool isSubtype(voidType(), AType t) = true;
bool isSubtype(listType(AType t1), listType(AType t2)) = isSubtype(t1, t2);
bool isSubtype(AType t1, AType t2) = true
	when t1 == t2;
default bool isSubtype(AType _, AType _) = false;


bool isConvertible(voidType(), AType t) = true;

bool isConvertible(atypeList(vs), atypeList(ws))
	= (true | isConvertible(v, w) && it | <v,w> <- zip(vs, ws))
	when (size(vs) == size(ws));

bool isConvertible(uType(_), intType()) = true;
	
bool isConvertible(uType(_), strType()) = true;
	
bool isConvertible(uType(n), uType(m)) = true;

bool isConvertible(listType(t1:uType(_)), t2) = isConvertible(t1, t2)
	when listType(_) !:= t2;

// TODO do we want covariant lists?
bool isConvertible(listType(t1), listType(t2)) = isConvertible(t1, t2);

bool isConvertible(AType t1, AType t2) = true
	when t1 == t2;
default bool isConvertible(AType _, AType _) = false;

str prettyPrintAType(voidType()) = "void";
str prettyPrintAType(byteType()) = "byte";
str prettyPrintAType(intType()) = "int";
str prettyPrintAType(typeType(t)) = "typeof(<prettyPrintAType(t)>)";
str prettyPrintAType(strType()) = "str";
str prettyPrintAType(boolType()) = "bool";
str prettyPrintAType(listType(t)) = "<prettyPrintAType(t)>[]";
str prettyPrintAType(structType(name, args)) = "structType(<name>, [<intercalate(",", [prettyPrintAType(a) | a <- args])>])";
str prettyPrintAType(anonType(_)) = "anonymous";
str prettyPrintAType(uType(n)) = "u<n>";
str prettyPrintAType(consType(formals)) = "constructor(<("" | it + "<prettyPrintAType(ty)>," | atypeList(fs) := formals, ty <- fs)>)";
str prettyPrintAType(funType(name,_,_,_)) = "fun <name>";
str prettyPrintAType(moduleType()) = "module";
str prettyPrintAType(variableType(s)) = "variableType(<s>)";
str prettyPrintAType(structDef(name, formals)) = "structDef(<name>, [<intercalate(", ", formals)>])";


AType birdInstantiateTypeParameters(Tree selector, structDef(str name1, list[str] formals), structType(str name2, list[AType] actuals), AType t, Solver s){
    if(size(formals) != size(actuals)) throw checkFailed([]);
    bindings = (formals[i] : actuals [i] | int i <- index(formals));
    return visit(t) { case variableType(str x) => bindings[x] };
}

default AType birdInstantiateTypeParameters(Tree selector, AType def, AType ins, AType act, Solver s) = act;


AType lub(AType t1, voidType()) = t1;
AType lub(voidType(), AType t1) = t1;
AType lub(AType t1, AType t2) = t1
	when t1 == t2;
AType lub(t1:uType(n), t2:uType(m)) = n>m?t1:t2;
AType lub(t1:uType(_), intType()) = intType();
AType lub(intType(), t1:uType(_)) = intType();
AType lub(t1:uType(_), strType()) = strType();
AType lub(strType(), t1:uType(_)) = strType();
AType lub(t1:listType(ta),t2:listType(tb)) = listType(lub(ta,tb));
default AType lub(AType t1, AType t2){ throw "Cannot find a lub for types <prettyPrintAType(t1)> and <prettyPrintAType(t2)>"; }

bool isTokenType(byteType()) = true;
bool isTokenType(structType(_,_)) = true;
bool isTokenType(variableType(_)) = true;
bool isTokenType(anonType(_)) = true;
bool isTokenType(listType(t)) = isTokenType(t);
bool isTokenType(consType(_)) = true;  
default bool isTokenType(AType t) = false;

AType infixComparator(intType(), intType()) = boolType();
default AType infixComparator(AType t1, AType t2){ throw "Wrong operands for a comparator"; }

AType infixLogical(t1, t2) = boolType()
	when isConvertible(t1, boolType()) && isConvertible(t2, boolType());
default AType infixLogical(AType t1, AType t2){ throw "Wrong operands for a logical operation"; }

AType infixBitwise(t1:listType(byteType()), t2:listType(byteType())) = listType(byteType());

default AType infixBitwise(AType t1, AType t2){ throw "Wrong operands for a bitwise operation: "+ prettyPrintAType(t1) +", " + prettyPrintAType(t2); }

AType infixShift(listType(byteType()), intType()) = listType(byteType());
default AType infixShift(AType t1, AType t2){ throw "Wrong operands for a shift operation"; }

// TODO Maybe more combinations? Also, there is redundancy between the two following definitions.
AType infixEquality(AType t1, AType t2) = boolType()
	when isSubtype(t1, t2) || isSubtype(t2, t1);
	
default AType infixEquality(AType t1, AType t2){ throw "Wrong operands for equality"; }

AType infixArithmetic(t1, t2) = intType()
	when isConvertible(t1, intType()) && isConvertible(t2, intType());
default AType infixArithmetic(AType t1, AType t2){ throw "Wrong operands for an arithmetic operation"; }

AType infixString(t1, t2) = strType()
	when isConvertible(t1, strType()) && isConvertible(t2, strType());
default AType infixString(AType t1, AType t2){ throw "Wrong operands for a string operation"; }


// TODO make it more flexible. Does this unify?
AType infixConcat(lt:listType(_), lt) = lt;

bool isUserDefined(structType(_,_)) = true;
bool isUserDefined(listType(t)) = isUserDefined(t);
default bool isUserDefined(AType t) = false;

str getUserDefinedName(structType(id, _)) = id;
str getUserDefinedName(listType(t)) = getUserDefinedName(t);
default str getUserDefinedName(AType t){ throw "Operation not defined on non-user defined types."; }

Type getNestedType((Type) `<Type t> []`) = getNestedType(t);
default Type getNestedType(Type t) = t;

// ---- Modules and imports

private loc project(loc file) {
   assert file.scheme == "project";
   return |project://<file.authority>|;
}

data PathConfig = pathConfig(list[loc] srcs = [], list[loc] libs = []);

PathConfig pathConfig(loc file) {
   assert file.scheme == "project";

   p = project(file);      
 
   return pathConfig(srcs = [ p + "bird-src"],  libs =[ p + "bird-lib"]);
}

private str __BIRD_IMPORT_QUEUE = "__birdImportQueue";

str getFileName((ModuleId) `<{Id "::"}+ moduleName>`) = replaceAll("<moduleName>.bird", "::", "/");

tuple[bool, loc] lookupModule(ModuleId name, PathConfig pcfg) {
    for (s <- pcfg.srcs + pcfg.libs) {
        result = (s + "/" + getFileName(name)); 
        println(result);
        if (exists(result)) {
            return <true, result>;
        }
    }
    return <false, |invalid:///|>;
}

void collect(current:(Import) `import <ModuleId name>`, Collector c) {
    c.addPathToDef(name, {moduleId()}, importPath());
    c.push(__BIRD_IMPORT_QUEUE, name);
}

void handleImports(Collector c, Tree root, PathConfig pcfg) {
    imported = {};
    while (list[ModuleId] modulesToImport := c.getStack(__BIRD_IMPORT_QUEUE) && modulesToImport != []) {
        c.clearStack(__BIRD_IMPORT_QUEUE);
        for (m <- modulesToImport, m notin imported) {
            if (<true, l> := lookupModule(m, pcfg)) {
                collect(parse(#start[Program], l).top, c);
            }
            else {
                c.report(error(root, "Cannot find module %v in %v or %v", m, pcfg.srcs, pcfg.libs));
            }
            imported += m; 
        }
    }
}

// ----  Collect definitions, uses and requirements -----------------------


void collect(current: (Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, Collector c){
 	c.define("<moduleName>", moduleId(), current, defType(moduleType()));
    c.enterScope(current);
    collect(imports, c);
    currentScope = c.getScope();
    	collect(decls, c);
    c.leaveScope(current);
}
 
Tree newConstructorId(Id id, loc root) {
    return visit(parse(#ConsId, "$<id>")) {
        case Tree t => t[@\loc = relocsingleLine(t@\loc, root)] 
            when t has \loc
    };
}

Tree newFieldNameId(DId id, loc root) {
    return visit(parse(#ConsId, "$<id>")) {
        case Tree t => t[@\loc = relocsingleLine(t@\loc, root)] 
            when t has \loc
    };
}

private loc relocsingleLine(loc osrc, loc base) 
    = (base.top)
        [offset = base.offset + osrc.offset]
        [length = osrc.length]
        [begin = <base.begin.line, base.begin.column + osrc.begin.column>]
        [end = <base.end.line, base.begin.column + osrc.end.column>]
        ;

 
/*void collect(current:(TopLevelDecl) `struct <Id id> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`,  Collector c) {
     c.define("<id>", structId(), current, defType(structDef("<id>", [])));
     //collect(id, formals, c);
     c.enterScope(current); {
     	collectFormals(id, formals, c);
     	collect(decls, c);
    }
    c.leaveScope(current);
}*/

void collect(current:(TopLevelDecl) `struct <Id id> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`,  Collector c) {
     list[Id] tformals = [tf |atypeFormals <- typeFormals, tf <- atypeFormals.typeFormals];
     if (_ <- tformals)
     	c.define("<id>", structId(), current, defType(structDef("<id>", ["<tf>" | tf <- tformals])));
     else
     	c.define("<id>", structId(), current, defType(structType("<id>", [])));
     //collect(id, formals, c);
     
     c.enterScope(current); {
     	for (Id tf <- tformals)
     		c.define("<tf>", typeVariableId(), tf, defType(variableType("<tf>")));
     	collectFormals(id, formals, c);
     	collect(decls, c);
    }
    c.leaveScope(current);
}

void collect(current:(TopLevelDecl) `@( <JavaId jid> ) <Type t> <Id id> <Formals? formals>`,  Collector c) {
     actualFormals = [af | fformals <- formals, af <- fformals.formals];
     collect(t, c);
     collect(actualFormals, c);
     c.define("<id>", funId(), current, defType([t] + actualFormals, AType(Solver s) {
     	return funType("<id>", s.getType(t), atypeList([s.getType(a) | a <- actualFormals]), "<jid>");
     	})); 
    
}

void collect(current:(Formal) `<Type ty> <Id id>`, Collector c){
	c.define("<id>", paramId(), current, defType(ty));
	collect(ty, c);
}

void collect(current:(DeclInStruct) `<Type ty> <Id id> = <Expr expr>`,  Collector c) {
	c.define("<id>", fieldId(), id, defType(expr));
	collect(ty, c);
	collect(expr, c);
	c.require("good assignment", current, [expr] + [ty],
        void (Solver s) { 
        	println("<expr> \>\>\> <s.getType(expr)>");
        	s.requireSubType(s.getType(expr), s.getType(ty), 
        		error(current, "Expression should be <prettyPrintAType(s.getType(ty))>, found <prettyPrintAType(s.getType(expr))>")); });
}    

void collect(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? cond>`,  Collector c) {
	c.require("declared type", ty, [ty], void(Solver s){
		s.requireTrue(isTokenType(s.getType(ty)), error(ty, "Non-initialized fields must be of a token type, but it was %t (AType: %t)", ty, s.getType(ty)));
	});
	if ("<id>" != "_"){
		c.define("<id>", fieldId(), id, defType(ty));
	}
	
	Maybe[Expr] siz = nothing();
	if (s <- size)
		siz = just(s. expr);
	
	collect(ty, c, size = siz);
	
	if (aargs <- args)
		collectArgs(ty, aargs, c);
	
	
	for (sz <-size){
		collectSize(ty, sz, c);
	}
	for (sc <- cond){
		collectSideCondition(ty, id, sc, c);
	}
}

void collectSideCondition(Type ty, DId id, current:(SideCondition) `? ( <Expr e>)`, Collector c){
	c.enterScope(current);
	// TODO Why did I get rid of this code but still works?
	//c.define("this", variableId(), newFieldNameId(id, id@\loc), defType(ty));
	c.define("this", variableId(), id, defType(ty));
	collect(e, c);
	c.require("side condition", current, [e], void (Solver s) {
		s.requireEqual(s.getType(e), boolType(), error(current, "Side condition must be boolean"));
	});
	c.leaveScope(current);
}

void collectSideCondition(Type ty, DId id, current:(SideCondition) `while ( <Expr e>)`, Collector c){
	c.enterScope(current);
	c.define("it", variableId(), newFieldNameId(id, id@\loc), defType([ty], AType (Solver s) {
	    if (listType(t) := s.getType(ty)) {
	       return t;
	    }
	    s.report(error(current, "while side condition can only guard list types"));
	}));
	collect(e, c);
	c.leaveScope(current);	
}

void collectSideCondition(Type ty, DId id, current:(SideCondition) `byparsing ( <Expr exp> )`, Collector c){
    c.enterScope(current);
    collect(exp, c);
    c.require("byparsing", current, [exp], void (Solver s){
		s.requireTrue(isTokenType(s.getType(exp)), error(exp, "The expression to parse should correspond to a token type"));
	});
    c.leaveScope(current);
}



void collectSideCondition(Type ty, DId id, current:(SideCondition) `? ( <ComparatorOperator uo> <Expr e>)`, Collector c){
	collect(e, c);
	c.require("side condition", current, [e], void (Solver s) {
		s.requireSubType(s.getType(e), intType(), error(current, "Expression in unary comparing side condition must have numeric type"));
	});
}

default void collectSideCondition(Type ty, DId id, current:(SideCondition) `? ( <EqualityOperator uo> <Expr e>)`, Collector c){
	collect(e, c);
	c.requireSubType(ty, e, error(current, "Type of unary expression in side condition must be compatible with the declared type"));
}

void collectSize(Type ty, sz:(Size) `[<Expr e>]`, Collector c){
	collect(e, c);
	c.require("size argument", sz, [ty] + [e], void (Solver s) {
		s.requireTrue(s.getType(ty) is listType, error(sz, "Setting size on a non-list element"));
		s.requireSubType(s.getType(e), intType(), error(sz, "Size must be an integer"));
	});
}

void collectArgs(Type ty, Arguments current, Collector c){
		currentScope = c.getScope();
		for (a <- current.args){
			collect(a, c);
		}
		c.require("constructor arguments", current, 
			  [ty] + [a |a <- current.args], void (Solver s) {
			if (!isUserDefined(s.getType(ty)))
				s.report(error(current, "Constructor arguments only apply to user-defined types but got %t", ty));
			if (isUserDefined(s.getType(ty))){
				idStr = getUserDefinedName(s.getType(ty));
				//ty_ = top-down-break visit (ty){
				//	case (Type)`<Type t> []` => t
				//	case Type t => t
				//};
				//tyLoc = ty@\loc;
				//conId = fixLocation(parse(#Type, "<ty_>"), tyLoc[offset=tyLoc.offset + tyLoc.length]);
				//conId = fixLocation(parse(#Type, "<ty_>"), tyLoc);
				ty_ = getNestedType(ty);
				AType t = s.getType(ty_);
				//println(t);
				//println(conId);
				//println(currentScope);
				if (structType(refName,actuals) := t){
					ct = s.getTypeInType(structType(refName, actuals), newConstructorId([Id] "<idStr>", ty@\loc), {consId()}, currentScope);
					argTypes = atypeList([ s.getType(a) | a <- current.args]);
					s.requireSubType(argTypes, ct.formals, error(current, "Wrong type of arguments"));
				}
				else throw "Operation not supported";
				
			}
		});
	
}

void collectFunctionArgs(Id id, Arguments current, Collector c){
		for (a <- current.args){
			collect(a, c);
		}
		c.require("constructor arguments", current, 
			  [id] + [a | a <- current.args], void (Solver s) {
			ty = s.getType(id);  
			if (funType(_, _, formals, _) := ty) {
				argTypes = atypeList([ s.getType(a) |  a <- current.args]);
				s.requireSubType(argTypes, formals, error(current, "Wrong type of arguments"));
			}
			else{
				s.report(error(current, "Function arguments only apply to function types but got %t", ty));
				
			}
		});
	
}

void collectFormals(Id id, Formals? current, Collector c){
	actualFormals = [af | fformals <- current, af <- fformals.formals];
	constructorFakeTree = newConstructorId(id, id@\loc);
	c.define("<constructorFakeTree>", consId(), constructorFakeTree, defType(actualFormals, AType(Solver s) {
     		return consType(atypeList([s.getType(a) | a <- actualFormals]));
    }));
    collect(actualFormals, c);
}

void collect(current:(TopLevelDecl) `choice <Id id> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`,  Collector c) {
	 // TODO  explore `Solver.getAllDefinedInType` for implementing the check of abstract fields
	 c.define("<id>", structId(), current, defType(structType("<id>",[])));
     c.enterScope(current); {
     	collectFormals(id, formals, c);
     	collect(decls, c);
     	ts = [ d.tp | d <- decls];
     	currentScope = c.getScope();
     	c.require("abstract fields", current, ts, void(Solver s){
     		abstractFields = [<"<id>", s.getType(id)> | (DeclInChoice) `abstract <Type _> <Id id>` <- decls];
     		for ((DeclInChoice) `<Type ty> <Arguments? args> <Size? size>` <- decls){
                map[str id, AType tp] definedFields;
                if (anonType(fields) := s.getType(ty)) {
                    definedFields = toMapUnique(fields);
                }
                else {
                   definedFields = toMapUnique(s.getAllDefinedInType(s.getType(ty), currentScope, {fieldId()}));
                }
                for (<aId, aTy> <- abstractFields) {
                    s.requireTrue(aId in definedFields, error(ty, "Field %v is missing from %v", aId, ty));
                    s.requireSubType(definedFields[aId], aTy, error(ty, "Field %v is not of the expected type %t", aId, aTy));
                }
     		};
     			
     	});
    }
    c.leaveScope(current);
    
}

void collect(current:(DeclInChoice) `abstract <Type ty> <Id id>`,  Collector c) {
	c.define("<id>", fieldId(), id, defType(ty));
	collect(ty, c);
}

void collect(current:(DeclInChoice) `<Type ty> <Arguments? args> <Size? size>`,  Collector c) {
	c.require("declared type", ty, [ty], void(Solver s){
		s.requireTrue(isTokenType(s.getType(ty)), error(ty, "Non-initialized fields must be of a token type but it was %t", ty));
	});
	collect(ty, c);
	
	if (aargs <- args)
		collectArgs(ty, aargs, c);
	
	for (sz <-size){
		collectSize(ty, sz, c);
	}
}

void collect(current:(UnaryExpr) `<UnaryOperator uo> <Expr e>`, Collector c){
	collect(e, c);
}


void collect(current:(Type)`<UInt v>`, Collector c, Maybe[Expr] size = nothing()) {
	c.calculate("actual type", current, [],
    	AType(Solver s) {
    		s.requireTrue(toInt("<v>"[1..]) % 8 == 0, error(current, "The number of bits in a u? type must be a multiple of 8")); 
            return listType(byteType(), n = just([Expr] "<toInt("<v>"[1..])/8>"));
        }); 
}


void collect(current:(Type)`byte`, Collector c, Maybe[Expr] size = nothing()) {
	c.fact(current, byteType());
}  

void collect(current:(Type)`str`, Collector c, Maybe[Expr] size = nothing()) {
	c.fact(current, strType());
}

void collect(current:(Type)`bool`, Collector c,  Maybe[Expr] size = nothing()) {
	c.fact(current, boolType());
}  

void collect(current:(Type)`int`, Collector c, Maybe[Expr] size = nothing()) {
	c.fact(current, intType());
}  


/*void collect(current:(Type)`<Id i>`, Collector c) {
	c.use(i, {structId(), typeVariableId()}); 
	c.calculate("variable type", current, [i] , AType(Solver s) { 
		println(s.getType(i));
    	if (structType(_):= s.getType(i))
    		return refType("<i>", []);
    	else
    		return s.getType(i);
  
     });
}*/

void collect(current:(Type)`<Type t> [ ]`, Collector c, Maybe[Expr] size = nothing()) {
	collect(t, c, size = size);
	c.calculate("list type", current, [t], AType(Solver s) {
		println("<t> &&& <s.getType(t)>");
		return listType(s.getType(t), n = size);
	});
}  

void collect(current: (Type) `<Id name> <TypeActuals? actuals>`, Collector c, Maybe[Expr] size = nothing()){
	println("checking <current>");
    c.use(name, {structId(), typeVariableId()});
    for (TypeActuals aactuals <- actuals, Type t <- aactuals.typeActuals)
    	collect(t, s, c);
    list[Type] tpActuals = [t | TypeActuals aactuals <- actuals, Type t <- aactuals.typeActuals];
    if (_ <- tpActuals){
    	c.calculate("actual type", current, [name] + tpActuals,
           AType(Solver s) {
           	if (structDef(_, fs) := s.getType(name))  
            	s.requireTrue(size(fs) == size(tpActuals), error(current, "Incorrect number of provided type arguments"));
            else if (sructType(_,_) := s.getType(name))
            	s.report(error(current, "User-defined type %v does not require parameters", name));
            else
            	s.report(error(current, "Type %v does not receive parameters", name));
            return structType("<name>", [s.getType(tp) | tp <- tpActuals]);});
    }
    else 
    	c.fact(current, name);
}

void collect(current:(Type)`struct { <DeclInStruct* decls>}`, Collector c, Maybe[Expr] size = nothing()) {
	c.enterScope(current);
		collect(decls, c);
	c.leaveScope(current);
	fields =for (d <-decls){
			switch(d){
				case (DeclInStruct) `<Type t> <Id id> = <Expr e>`: append(<"<id>", t>);
				case (DeclInStruct) `<Type t> <DId id> <Arguments? args> <Size? size> <SideCondition? sc>`: append(<"<id>", t>);
			};
		};
	//for (<id, ty> <- fields){
	//		c.define("<id>", fieldId(), current, defType(ty));
	//};
	c.calculate("anonymous struct type", current, [ty | <_, ty> <- fields], AType(Solver s){
		return anonType([<id, s.getType(ty)> | <id, ty> <- fields]);
	});
}

void collect(current: (Expr) `[<{Expr ","}*  exprs>]`, Collector c){
    collect([e | e <-exprs], c);
    c.calculate("list type", current, [e | e <-exprs], AType(Solver s) { 
    	return (listType(voidType()) | lub(it, listType(x)) | x <- [s.getType(e) | e <- exprs ]);
     });
}

void collect(current: (Expr) `<Expr e>.as[<Type t>]`, Collector c){
    collect(e, c);
    collect(t, c);
   	c.calculate("casting", current, [t], AType (Solver s){
		return s.getType(t);
	});
}

void collect(current: (Expr) `<StringLiteral lit>`, Collector c){
    c.fact(current, strType());
}

void collect(current: (Expr) `<IntHexLiteral nat>`, Collector c){
    c.fact(current, intType());
}

void collect(current: (Expr) `<IntDecLiteral nat>`, Collector c){
    c.fact(current, intType());
}

void collect(current: (Expr) `<IntBitLiteral nat>`, Collector c){
    c.fact(current, intType());
}

void collect(current: (Expr) `<BytesDecLiteral nat>`, Collector c){
    c.fact(current, listType(byteType()));
}

void collect(current: (Expr) `<BytesHexLiteral nat>`, Collector c){
	c.fact(current, listType(byteType()));
}

void collect(current: (Expr) `<BytesBitLiteral nat>`, Collector c){
    c.fact(current, listType(byteType()));
}

void collect(current: (Expr) `<BytesStringLiteral nat>`, Collector c){
    c.fact(current, listType(byteType()));
}

void collect(current: (Expr) `<BytesArrLiteral nat>`, Collector c){
    c.fact(current, listType(byteType()));
}

void collect(current: (Expr) `<Id id>`, Collector c){
    c.use(id, {variableId(), fieldId(), paramId()});
}

void collect(current: (Expr) `<Expr e>.offset`, Collector c){
	collect(e, c);
	c.require("offset", current, [e], void (Solver s) {
		s.requireTrue(isTokenType(s.getType(e)), error(current, "Only token types have offsets"));
	}); 
	c.fact(current, intType());
}

void collect(current: (Expr) `<Expr e>.length`, Collector c){
	collect(e, c);
	c.require("length", current, [e], void (Solver s) {
		s.requireTrue(listType(_) := s.getType(e), error(current, "Only list types have length"));
	}); 
	c.fact(current, intType());
}

void collect(current: (Expr) `typeOf[<Type t>]`, Collector c){
	collect(t, c);
	c.calculate("reified type", current, [t], AType (Solver s){
		return typeType(s.getType(t));
	});
}

void collect(current: (Expr) `<Expr e>.size`, Collector c){
	collect(e, c);
	c.require("size", current, [e], void (Solver s) {
		s.requireTrue(isTokenType(s.getType(e)), error(current, "Only token types have size"));
	}); 
	c.fact(current, intType());
}

void collect(current: (Expr) `<Expr e>.<Id field>`, Collector c){
	//currentScope = c.getScope();
	c.useViaType(e, field, {fieldId()});
	c.fact(current, field);
	collect(e, c);
	c.require("trivial", current, [field], void (Solver s) {
		println("<field> ||||| <s.getType(field)>");
	}); 
	//c.calculate("field type", current, [e], AType(Solver s) {
	//	return s.getTypeInType(s.getType(e), field, {fieldId()}, currentScope); });

}

void collect(current: (Expr) `<Id id> <Arguments args>`, Collector c){
	c.use(id, {funId()});
	collectFunctionArgs(id, args, c);
	c.calculate("function call", current, [id] + [a | a <- args.args], AType(Solver s){
		ty = s.getType(id);
		if (funType(_, retType, _, _) := ty)
			return retType;
		else{
			s.report(error(current, "Function arguments only apply to function types but got %t", ty));
			
		}
	});	
}

void collect(current: (Expr) `<Expr e>[<Range r>]`, Collector c){
	collect(e, c);
	c.require("list expression", current, [e], void(Solver s){
			s.requireTrue(listType(_) := s.getType(e), error(e, "Expression must be of list type"));
		});
	collectRange(current, e, r, c);
}

void collectRange(Expr access, Expr e, current:(Range) `: <Expr end>`, Collector c){
	collect(end, c);
	c.calculate("list access", access, [e, end], AType (Solver s){
		s.requireSubType(end, intType(), error(end, "Index must be integer"));
		return s.getType(e);
	});
}

void collectRange(Expr access, Expr e, current:(Range) `<Expr begin> : <Expr end>`, Collector c){
	collect(begin, end, c);
	c.calculate("list access", access, [e, begin, end], AType (Solver s){
		s.requireEqual(begin, intType(), error(begin, "Index must be integer"));
		s.requireEqual(end, intType(), error(end, "Index must be integer"));
		return s.getType(e);
	});
}

void collectRange(Expr access, Expr e, current: (Range) `<Expr begin> :`, Collector c){
	collect(begin, c);
	c.calculate("list access", access, [e, begin], AType (Solver s){
		s.requireEqual(begin, intType(), error(begin, "Index must be integer"));
		return s.getType(e);
	});
}

void collectRange(Expr access, Expr e, current: (Range) `<Expr idx>`, Collector c){
	collect(idx, c);
	c.calculate("list access", access, [e, idx], AType (Solver s){
		s.requireEqual(idx, intType(), error(idx, "Indexes must be integers"));
		s.requireTrue(listType(ty) := s.getType(e), error(e, "Expression is not of type list"));
		if (listType(ty) := s.getType(e))
			return ty;
	});	
}

void collect(current: (Expr) `<Expr e1> <EqualityOperator op> <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "<op>", infixEquality, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> || <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "||", infixLogical, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> && <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "&&", infixLogical, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> ? <Expr e2> : <Expr e3>`, Collector c){
    collect(e1, e2, e3, c);
    // TODO relax equality requirement
	c.calculate("ternary operator", current, [e1, e2, e3], AType(Solver s) {
		s.requireSubType(e1, boolType(), error(e1, "Condition must be boolean"));
		s.requireTrue(s.subtype(e2, e3) || s.subtype(e3, e2), error(e2, "The two branches of the ternary operation must have the same type"));
		return s.subtype(e2, e3)?s.getType(e3):s.getType(e2);
	});
}

void collect(current: (Expr) `<Expr e1> <ComparatorOperator u> <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "<u>", infixComparator, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> & <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "&", infixBitwise, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> ^ <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "^", infixBitwise, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> | <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "|", infixBitwise, e1, e2, c); 
}


void collect(current: (Expr) `<Expr e1> \>\> <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "\>\>", infixShift, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> \>\>\> <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "\>\>\>", infixShift, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> \<\< <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "\<\<", infixShift, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> (+) <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "+", infixString, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> + <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "+", infixArithmetic, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> % <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "%", infixArithmetic, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> / <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "/", infixArithmetic, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> - <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "-", infixArithmetic, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> * <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "*", infixArithmetic, e1, e2, c); 
}

void collect(current: (Expr) `<Expr e1> ++ <Expr e2>`, Collector c){
    collect(e1, e2, c);
    collectInfixOperation(current, "++", infixConcat, e1, e2, c); 
}

void collect(current: (Expr) `(<Expr e>)`, Collector c){
    collect(e, c); 
    c.fact(current, e);
}

void collect(current: (Expr)`! <Expr e>`, Collector c) {
    collect(e,c);
    c.calculate("not expression", current, [e], AType (Solver s) {
        et = s.getType(e);
        if (et != boolType()) {
            s.requireSubType(et, intType(),error(e, "Expected either a boolean type, or an int type, got: %t", e));
        }
        return et;
    });
}

void collect(current: (Expr)`- <Expr e>`, Collector c) {
    collect(e,c);
    c.fact(current, e);
    c.requireSubType(e, intType(), error(e, "Expected a int type, got: %t", e));
}


void collect(current: (Expr) `( <Type accuType> <Id accuId> = <Expr init> | <Expr update> | <Id loopVar> \<- <Expr source>)`, Collector c){
    collect(source, c);  // source should be outside the scope of the reducer
    c.fact(current, accuId);
    c.enterScope(current); {
        collect(accuType, init, update, c);
        collectGenerator(loopVar, source, c);

        c.define("<accuId>", variableId(), accuId, defType(accuType));
        c.requireSubType(update, accuId, error(update, "Expected type: %t got: %t", accuId, update));
        c.requireSubType(init, accuId, error(update, "Expected type: %t got: %t", accuId, init));
    } c.leaveScope(current);
}

void collect(current: (Expr) `[ <Expr mapper> | <Id loopVar> \<- <Expr source>]`, Collector c){
    collect(source, c);  // source should be outside the scope of the comprehension 
    c.calculate("list type", current, [mapper], AType (Solver s) { return listType(s.getType(mapper)); });
    c.enterScope(current); {
        collect(mapper, c);
        collectGenerator(loopVar, source, c);
    } c.leaveScope(current);
}

void collectGenerator(Id loopVar, Expr source, Collector c) {
    c.define("<loopVar>", variableId(), loopVar, defType([source], AType(Solver s) {
        if (listType(AType tp) := s.getType(source)) {
            return tp;
        }
        s.report(error(source, "Expected a list type, got: %t", source));
    }));
}



void collectInfixOperation(Tree current, str op, AType (AType,AType) infixFun, Tree lhs, Tree rhs, Collector c) {
	c.calculate("<op>",current, [lhs, rhs], AType(Solver s) {
		try{
			return infixFun(s.getType(lhs), s.getType(rhs));
		}	
		catch str msg:{
			s.report(error(current, msg));
		}
	});
}	

// ----  Examples & Tests --------------------------------
TModel birdTModelFromTree(Tree pt, bool debug = false){
    if (pt has top) pt = pt.top;
    c = newCollector("collectAndSolve", pt, config=getBirdConfig());    // TODO get more meaningfull name
    collect(pt, c);
    handleImports(c, pt, pathConfig(pt@\loc));
    return newSolver(pt, c.run()).run();
}

tuple[list[str] typeNames, set[IdRole] idRoles] birdGetTypeNameAndRole(structType(str name,_)) = <[name], {structId()}>;
tuple[list[str] typeNames, set[IdRole] idRoles] birdGetTypeNameAndRole(structDef(str name,_)) = <[name], {structId()}>; // TODO this *has to be* deprecated
tuple[list[str] typeNames, set[IdRole] idRoles] birdGetTypeNameAndRole(funType(str name, _, _, _)) = <[name], {funId()}>;
tuple[list[str] typeNames, set[IdRole] idRoles] birdGetTypeNameAndRole(AType t) = <[], {}>;

AType birdGetTypeInAnonymousStruct(AType containerType, Tree selector, loc scope, Solver s){
    if(anonType(fields) :=  containerType){
    	return Set::getOneFrom((ListRelation::index(fields))["<selector>"]);
    }
    else
    {	s.report(error(selector, "Undefined field <selector> on %t",containerType));
    }
}

private TypePalConfig getBirdConfig() = tconfig(
    isSubType = isSubtype,
    getTypeNamesAndRole = birdGetTypeNameAndRole,
    getTypeInNamelessType = birdGetTypeInAnonymousStruct,
    instantiateTypeParameters = birdInstantiateTypeParameters
);


public start[Program] sampleBird(str name) = parse(#start[Program], |project://bird-core/<name>.bird|);

list[Message] runBird(str name, bool debug = false) {
    Tree pt = sampleBird(name);
    TModel tm = birdTModelFromTree(pt, debug = debug);
    return tm.messages;
}
 
bool testBird(int n, bool debug = false, set[str] runOnly = {}) {
    return runTests([|project://bird-core/src/lang/bird/bird<"<n>">.ttl|], #start[Program], TModel (Tree t) {
        return birdTModelFromTree(t, debug=debug);
    }, runOnly = runOnly);
}

