module lang::bird::Generator2Nest

import IO;

import lang::bird::Syntax;
import lang::bird::Checker;
import analysis::graphs::Graph;


import List;
import Set;
import String;

extend analysis::typepal::TypePal;

bool biprintln(value v){
	iprintln(v);
	return true;
}

tuple[str, str] compile(current: (Program) `module <{Id "::"}+ moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types, map[loc,str] scopes, map[loc,Define] defines)
	= <packageName,
	"package engineering.swat.examples.formats<packageName>;
    '
    'import engineering.swat.nest.core.ParseError;
	'import engineering.swat.nest.core.bytes.ByteStream;
	'import engineering.swat.nest.core.bytes.Context;
	'import engineering.swat.nest.core.bytes.Sign;
	'import engineering.swat.nest.core.nontokens.NestBigInteger;
	'import engineering.swat.nest.core.nontokens.NestValue;
	'import engineering.swat.nest.core.tokens.Token;
	'import engineering.swat.nest.core.tokens.UserDefinedToken;
	'import engineering.swat.nest.core.tokens.primitive.TokenList;
	'import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
	'import java.nio.ByteOrder;
	'import java.nio.charset.StandardCharsets;
	'
	'public class <className>$ {
	'	private <className>$(){}
	'	<for (d <- decls, !(d is funDecl)) {>
	'   <compile(d, useDefs, types)>
	'	<}>
	'}">
	when [dirs*, className] := [x | x <-moduleName],
		 str packageName := ((size(dirs) == 0)? "": ("."+ intercalate(".", dirs)))
		 ;

str generateNestType((Type) `int`)
	= "NestBigInteger";

str generateNestType((Type) `byte []`)
	= "UnsignedBytes";
	
str generateNestType((Type) `<Type t> []`)
	= "TokenList\<<generateNestType(t)>\>"
	when (Type) `byte` !:= t;
	
str generateNestType((Type) `byte`) {
	throw "Single byte cannot be used as a type";
}

str generateNestType((Type) `<UInt v>`)
	= "UnsignedBytes";

str generateNestType((Type) `<Id id> <TypeActuals? typeActuals>`)
	="<id>";
	
str makeId(Tree t) = ("<t>" =="_")?"$anon_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.begin.column>_<lo.end.line>_<lo.end.column>":"<t>"
	when lo := t@\loc;
	
str compileAnno("encoding", str val)
	= "ctx = ctx.setEncoding(StandardCharsets.<val>);";
	
str compileAnno("endianness", str val)
	= "ctx = ctx.setByteOrder(ByteOrder.<val>_ENDIAN);";

default str compileAnno(str prop, str val) {
	throw "Now implementation for annotation <prop>";
}

str compile(current:(TopLevelDecl) `choice <Id sid> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types) =
   "public static final class <sid> extends UserDefinedToken {
   '
   '	private final Token entry;
   '
   ' 	<for (<id, typ> <- allFieldsList){>public final <generateNestType(typ)> <id>;
   '	<}>
   '	private <sid>(<intercalate(", ", ["Token entry"] + ["<generateNestType(typ)> <id>" | <id, typ> <- allFieldsList])>){
   '		this.entry = entry;
   '	<for  (<id, _> <- allFieldsList){>	this.<id> = <id>;
   '	<}>	
   '	}
   '
   '	public static <sid> parse(ByteStream source, Context ctx) throws ParseError {
   '		<for (aannos <- annos, (Anno) `<Id prop> = <Id val>` <- aannos.annos){><compileAnno("<prop>", "<val>")>
   '		<}>
   '	<for (DeclInChoice d <- decls){>
   '		<compile(d, sid, useDefs, types)><}>
   '		return new <sid>(<intercalate(", ", ["entry"] + [id | <id, _> <- fieldsList])>);
   '	}
   '
   '	@Override
   '    protected Token[] parsedTokens() {
   '        return new Token[]{ entry };
   '    }
   '}"           	
	when lrel[str, Type] formalsList := [<"<id>", typ> | aformals <- formals,(Formal) `<Type typ> <Id id>` <- aformals.formals],
		 lrel[str, Type] fieldsList := [<"<id>", ty> | (DeclInChoice) `abstract <Type ty> <Id id>` <- decls],
		 lrel[str, Type] allFieldsList := formalsList + [<id, ty> | <id, ty> <- fieldsList]
		 ;

str compile(current:(TopLevelDecl) `struct <Id sid> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types) =
   "public static final class <sid> extends UserDefinedToken {
   ' 	<for (<id, typ> <- allFieldsList){>public final <generateNestType(typ)> <id>;
   '	<}>
   '	private <sid>(<intercalate(", ", ["<generateNestType(typ)> <id>" | <id, typ> <- allFieldsList])>){
   '	<for  (<id, _> <- allFieldsList){>	this.<id> = <id>;
   '	<}>	
   '	}
   '
   '	public static <sid> parse(ByteStream source, Context ctx) throws ParseError {
   '		<for (aannos <- annos, (Anno) `<Id prop> = <Id val>` <- aannos.annos){><compileAnno("<prop>", "<val>")>
   '		<}>
   '	<for (DeclInStruct d <- decls){>
   '		<compile(d, sid, useDefs, types)><}>
   '		return new <sid>(<intercalate(", ", [id | <id, _, _> <- fieldsList])>);
   '	}
   '
   '	@Override
   '    protected Token[] parsedTokens() {
   '        return new Token[]{<intercalate(", ", ["<id>" | <id, _, isToken> <- fieldsList, isToken])>};
   '    }
   '}"           	
	when lrel[str, Type] formalsList := [<"<id>", typ> | aformals <- formals,(Formal) `<Type typ> <Id id>` <- aformals.formals],
		 lrel[str, Type, bool] fieldsList := [<makeId(d.id), d.ty, d is token> | DeclInStruct d <- decls],
		 lrel[str, Type] allFieldsList := formalsList + [<id, ty> | <id, ty, _> <- fieldsList]
		 ;		 
		 
str generateParsingInstruction(current: (Type) `byte []`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "source.readUnsigned(<compile(expr, [DId] "dummy", useDefs, types)>, ctx)"
	when listType(byteType(), n = just(expr)) :=  types[current@\loc];
	
str generateParsingInstruction(current: (Type) `byte []`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "TODO;"
	when listType(byteType(), n = nothing()) :=  types[current@\loc];	
	
str generateParsingInstruction((Type) `<Type t> []`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "TokenList.untilParseFailure(source, ctx, (s, c) -\> <generateNestType(t)>.parse(s, c))"
	when (Type) `byte` !:= t;
	
str generateParsingInstruction((Type) `byte`, rel[loc,loc] useDefs, map[loc, AType] types) {
	throw "Single byte cannot be used as a type";
}

str generateParsingInstruction(current: (Type) `<UInt v>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "source.readUnsigned(<n>, ctx)"
	when listType(byteType(), n = just(n)) :=  types[current@\loc];

str generateParsingInstruction((Type) `<Id id> <TypeActuals? typeActuals>`, rel[loc,loc] useDefs, map[loc, AType] types)
	="<id>.parse(source, ctx)";
			 
str generateParsingInstruction(Type t, rel[loc,loc] useDefs, map[loc, AType] types) {
	throw "Not yet generateParsingInstruction for type <t>";
}

str compileCondition((SideCondition) `? (<EqualityOperator eo> <Expr e>)`, DId this, Type thisType, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<makeId(this)>.sameBytes(<compile(e, this, useDefs, types)>)"
	when listType(byteType()) := types[e@\loc],
		 listType(byteType()) := types[thisType@\loc];

str generateSideCondition(current: (SideCondition) `? (<EqualityOperator eo> <Expr e>)`, Id parentId, DId this, Type thisType, rel[loc,loc] useDefs, map[loc, AType] types) =
	"if (<op>(<compileCondition(current, this, thisType, useDefs, types)>)) {
    '	throw new ParseError(\"<parentId>.<makeId(this)>\", <makeId(this)>);
    '}"
	when op := (eo is equality?"!":"");

str generateSideCondition((SideCondition) `? (<Expr e>)`, Id parentId, DId this, Type _, rel[loc,loc] useDefs, map[loc, AType] types) 
	= "if (!(<compile(e, this, useDefs, types)>)) {
    '	throw new ParseError(\"<parentId>.<makeId(this)>\", <makeId(this)>);
    '}"
	;
	
// Declarations in choices	

// Token field

str compile(current:(DeclInChoice) `<Type ty> <Arguments? args> <Size? size>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"";
	
// Abstract field	

str compile(current:(DeclInChoice) `abstract <Type ty> <Id id>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"";	

// Declarations in structs
		 
// Token field

// TODO Check if `while` does make sense for something that is not u8[].
// 		If not, enforce constraint in type checker (together with type of it)
str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> while (<Expr e>)`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"TokenList\<<generateNestType(ty)>\> <makeId(id)> = TokenList.parseWhile(source, ctx,
    '	(s, c) -\> s.readUnsigned(1, c),
    '	it -\> !(<compile(e, id, useDefs, types)>)
    ');";
		 
default str compile(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? sideCondition>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"<generateNestType(ty)> <makeId(id)> = <generateParsingInstruction(ty, useDefs, types)>;
	'<for (sc <- sideCondition){ bprintln(sc);><generateSideCondition(sc, parentId, id, ty, useDefs, types)>
	'<}>";

// Computed field

str compile(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"";


// Expressions

str compile(current: (Expr) `<IntDecLiteral nat>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	 "NestBigInteger.of(<nat>)";
	 
str compile(current: (Expr) `<IntHexLiteral hex>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestBigInteger.of(<hex.number>)";

// TODO ask Davy about second argument
str compile(current: (Expr) `<BytesHexLiteral hex>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(<hex>, <len>)"
	when len := (size("<hex>") - 2)/2;
	
str compile(current: (Expr) `<BytesBitLiteral bits>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"NestValue.of(<bits>, 1)";
	
str compile(current: (Expr) `<BytesStringLiteral string>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(<string>, ctx)";
		
str compile(current: (Expr) `\< <{SingleHexIntegerLiteral ","}+ bytes> \>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(new byte[]{<intercalate(", ", ["(byte) <b>" | b <- bytes])>})";

str compile(current: (Expr) `it`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"it";

str compile(current: (Expr) `this`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<makeId(this)>";
	
str compile(current: (Expr) `<Id id>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<id>"
	when (Id) `this` !:= id && (Id) `it` !:= id ;
	
str compile(current: (Expr) `<Id id> <Arguments args>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<javaName>.<id>(<intercalate(", ", [compile(e, this, useDefs, types) | e <- args.args])>)"
	when funType(_, _, _, javaName) := types[id@\loc];
	
default str compile(current: (Expr) `<Id id> <Arguments args>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) {
	throw "Not yet implemented";
}

str compile(current: (Expr) `<Expr e>.as[int]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<compile(e, this, useDefs, types)>.asValue().asInteger()";

default str compile(current: (Expr) `<Expr e>.as[<Type t>]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines);

str compile(current: (Expr) `<StringLiteral lit>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = "con(<lit>)";

str compile(current: (Expr) `<HexIntegerLiteral nat>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = "con(<nat>)";

str compile(current: (Expr) `<BitLiteral nat>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = "con(<noUnderscoreNat>)"
	when noUnderscoreNat := replaceAll("<nat>","_","");


str compile(current: (Expr) `(<Expr e>)`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	compile(e, this, useDefs, types);
	
// TODO check semantics for >= and <= 
str compile(current: (Expr) `<Expr e1> <ComparatorOperator uo> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)>.compareTo(<compile(e2, this, useDefs, types)>) <uo> 0)";
		 
str compile(current: (Expr) `<Expr e1> <EqualityOperator uo> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<op>(<compile(e1, this, useDefs, types)>.equals(<compile(e2, this, useDefs, types)>))"
	when op := (uo is equality?"":"!"); 
	
str compileBinary(str op, Expr e1, Expr e2, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)>).<op>(<compile(e2, this, useDefs, types)>)";
		 
str compile(current: (Expr) `<Expr e1> || <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("logicalOr", e1, e2, this, useDefs, types);
		 
str compile(current: (Expr) `<Expr e1> & <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("and", e1, e2, this, useDefs, types);
	
str compile(current: (Expr) `<Expr e1> ++ <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
    "TokenList.of(ctx, <compile(e1, this, useDefs, types)>, <compile(e2, this, useDefs, types)>)";
    
str compile(current: (Expr) e, DId this, rel[loc,loc] useDefs, map[loc, AType] types){
    throw "Expression not yet implemented: <e>";
}        
	
/*str compile(current: (Expr) `parse (<Expr e>) with <Type t>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"tie(<compiledType>, <compiledExpr>)"
	when compiledType := compile(t, parentId, tokenExps, useDefs, types, index, scopes, defines),
		 compiledExpr := compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines);*/

str compile(current: (Expr) `<Id id> ( <{Expr ","}* exprs>)`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
    "new <javaId>().apply(<intercalate(", ", [compile(e, parentId, tokenExps, useDefs, types, index, scopes, defines) | Expr e <- exprs])>)"
    when loc funLoc := Set::getOneFrom((useDefs[id@\loc])),
    	 funType(_,_,_,javaId) := types[funLoc];

str compile(current: (Expr) `<Expr e1> - <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
    "<getInfixOperator("-")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";
    
str compile(current: (Expr) `<Expr e1> + <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
    "<getInfixOperator("+")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    

str compile(current: (Expr) `<Expr e1> * <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
    "<getInfixOperator("*")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    
    
str compile(current: (Expr) `<Expr e1> (+) <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
    "<getInfixOperator("(+)")>(<compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines)>, <compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)>)";    	 

str compile(current: (Expr) `[ <{Expr ","}* es>]`, rel[loc,loc] useDefs, map[loc, AType] types) = "con(<intercalate(", ",["<e>" | e <- es])>)"
	when listType(ty) := types[current@\loc]; 
		 
str compile(current: (Expr) `<Expr e1> && <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
	calculateOp("&&", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\> <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \>\>\> <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
	calculateOp("\>\>", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc];
		 
str compile(current: (Expr) `<Expr e1> \<\< <Expr e2>`, Id parentId, rel[loc,loc] useDefs, map[loc, AType] types) =
	calculateOp("\<\<", {t1,t2}, [compile(e1, parentId, tokenExps, useDefs, types, index, scopes, defines), compile(e2, parentId, tokenExps, useDefs, types, index, scopes, defines)])
	when t1 := types[e1@\loc],
		 t2 := types[e2@\loc]; 
		 
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
