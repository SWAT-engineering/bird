module lang::bird::Generator2Nest

import IO;

import lang::bird::Syntax;
import lang::bird::Checker;
import analysis::graphs::Graph;


import List;
import Set;
import String;

extend analysis::typepal::TypePal;

data PathConfig(loc target = |cwd:///|);	

DId DUMMY_DID = [DId] "dummy";

bool biprintln(value v){
	iprintln(v);
	return true;
}

tuple[str package, str class] toJavaName(ModuleId moduleName) =
	<packageName, "<className>$"> 
	when [dirs*, className] := [x | x <- moduleName.moduleName],
		 str packageName := ((size(dirs) == 0)? "": (intercalate(".", dirs)));
		 

str toJavaFQName(ModuleId moduleName) = 
	"__$<tokenName>"
	when [tokenName] := [x | x <- moduleName.moduleName];			 
		 
str toJavaFQName(ModuleId moduleName) = 
	dirs == "<className>$.__$<tokenName>"
	when [className, tokenName] := [x | x <- moduleName.moduleName];		 
		 
str toJavaFQName(ModuleId moduleName) = 
	dirs == []? "__$<className>": "<intercalate(".", ["<x>" | x <- dirs]  + ["<className>$"] + ["__$<tokenName>"])>"
	when [dirs*, className, tokenName] := [x | x <- moduleName.moduleName],
		 dirs != [];

tuple[str, str] compile(current: (Program) `module <ModuleId moduleName> <Import* imports> <TopLevelDecl* decls>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= <fqn,
	"package <packageName>;
    '
    'import engineering.swat.nest.core.ParseError;
	'import engineering.swat.nest.core.bytes.ByteStream;
	'import engineering.swat.nest.core.bytes.Context;
	'import engineering.swat.nest.core.bytes.Sign;
	'import engineering.swat.nest.core.nontokens.NestBigInteger;
	'import engineering.swat.nest.core.nontokens.NestValue;
	'import engineering.swat.nest.core.tokens.Token;
	'import engineering.swat.nest.core.tokens.operations.Choice;
	'import engineering.swat.nest.core.tokens.UserDefinedToken;
	'import engineering.swat.nest.core.tokens.primitive.TokenList;
	'import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
	'import java.nio.ByteOrder;
	'import java.nio.charset.StandardCharsets;
	'import java.util.Collections;
	'import java.util.function.BiFunction;
	'import java.util.concurrent.atomic.AtomicReference;
	'import java.util.stream.IntStream;
	'<for ((Import) `import <ModuleId i>` <- imports) { <pn, cn> = toJavaName(i); >
	'import <pn>.<cn>.*;
	'<}>
	'public class <className> {
	'	private <className>(){}
	'	<for (d <- decls, !(d is funDecl)) {>
	'   <compile(d, useDefs, types)>
	'	<}>
	'}">
	when <packageName, className> := toJavaName(moduleName),
		 fqn := (packageName == "" ? className : "<packageName>.<className>");
		 
str generateNestType(current: (Type) `struct { <DeclInStruct* decls>}`, map[loc, AType] types)
	= "$anon_type_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.begin.column>_<lo.end.line>_<lo.end.column>"
	when lo := current@\loc;

str generateNestType((Type) `int`, map[loc, AType] types)
	= "NestBigInteger";
	
str generateNestType((Type) `str`, map[loc, AType] types)
	= "String";	

str generateNestType(current: (Type) `byte []`, map[loc, AType] types)
	= "UnsignedBytes"
	when listType(byteType(), n = nothing()) := types[current@\loc];
	
str generateNestType(current: (Type) `byte []`, map[loc, AType] types)
	= "UnsignedBytes"
	when listType(byteType(), n = just(_)) := types[current@\loc];	
	
str generateNestType((Type) `<Type t> []`, map[loc, AType] types)
	= "TokenList\<<generateNestType(t, types)>\>"
	when (Type) `byte` !:= t;
	
str generateNestType((Type) `byte`, map[loc, AType] types) {
	throw "Single byte cannot be used as a type";
}

str generateNestType((Type) `<UInt v>`, map[loc, AType] types)
	= "UnsignedBytes";

str generateNestType(current: (Type) `<ModuleId id> <TypeActuals? typeActuals>`, map[loc, AType] types)
	="<toJavaFQName(id)><nestTypeActuals>"
	when variableType(_) !:= types[current@\loc],
		 list[Type] typeActualsList := [ta | atypeActuals <- typeActuals, Type ta <- atypeActuals.typeActuals],
		 str nestTypeActuals := ((size(typeActualsList) > 0)?"\<<intercalate(",", [generateNestType(ta, types) | Type ta <- typeActualsList])>\>":"");

str generateNestType(current: (Type) `<ModuleId id> <TypeActuals? typeActuals>`, map[loc, AType] types)
	= "<id>"
	when variableType(_) := types[current@\loc];


str makeUnique(Tree t, str prefix) = "$<prefix>_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.begin.column>_<lo.end.line>_<lo.end.column>"
	when lo := t@\loc;
	
str makeId(Tree t) = ("<t>" =="_")?"$anon_<lo.offset>_<lo.length>_<lo.begin.line>_<lo.begin.column>_<lo.end.line>_<lo.end.column>":"<t>"
	when lo := t@\loc;
	
str compileAnno("encoding", (Expr) `<Id val>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "__$ctx = __$ctx.setEncoding(StandardCharsets.<val>);";
	
str compileAnno("endianness", (Expr) `<Id val>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "__$ctx = __$ctx.setByteOrder(ByteOrder.<val>_ENDIAN);";
	
str compileAnno("endianness", (Expr) `<Id val>`, rel[loc,loc] useDefs, map[loc, AType] types)
	= "__$ctx = __$ctx.setByteOrder(ByteOrder.<val>_ENDIAN);";
	
str compileAnno("offset", Expr e, rel[loc,loc] useDefs, map[loc, AType] types)
	= "__$src = __$src.fork(<compile(e, DUMMY_DID, useDefs, types)>);";	
	
default str compileAnno(str prop, Expr e, rel[loc,loc] useDefs, map[loc, AType] types) {
	throw "No implementation for annotation <prop>";
}

str compile(current:(TopLevelDecl) `choice <Id sid> <Formals? formals> <Annos? annos> { <DeclInChoice* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types) =
   "public static final class __$<sid> extends UserDefinedToken {
   '	<for ((DeclInChoice) `<Type ty> <Arguments? args> <Size? sz>` <- decls, ty is anonymousType){><generateAnonymousType(ty, formalsList, useDefs, types)>
   '	<}>
   '	private final Token __$$entry;
   ' 	<for (<id, typ> <- fieldsList){>public final <generateNestType(typ, types)> <id>;
   '	<}>
   '	private __$<sid>(<intercalate(", ", ["Token __$$entry"] + ["<generateNestType(typ, types)> <id>" | <id, typ> <- fieldsList])>){
   '		this.__$$entry = __$$entry;
   '	<for  (<id, _> <- fieldsList){>	this.<id> = <id>;
   '	<}>	
   '	}
   '
   '	public static __$<sid> parse(ByteStream __$src, Context __$ctx<intercalate(", ", [""] +["<generateNestType(typ, types)> <id>" | <id, typ> <- formalsList])>) throws ParseError {
   '		<for (aannos <- annos, (Anno) `<Id prop> = <Expr e>` <- aannos.annos){><compileAnno("<prop>", e, useDefs, types)>
   '		<}>
   '	<for (<id, ty> <- fieldsList){>
   '		AtomicReference\<<generateNestType(ty, types)>\> <id> = new AtomicReference\<\>();
   '	<}>
   '		Token __$$entry = Choice.between(__$src, __$ctx
   '	<for ((DeclInChoice) `<Type ty> <Arguments? args> <Size? sz>` <- decls){>,
   '		(s, c) -\> {
   '			<generateNestType(ty, types)> __$result = <generateParsingInstruction(ty, [a | aargs <- args, Expr a <- aargs.args], [id | <id, _> <- formalsList], useDefs, types)>;
   '			<for (<id, _> <- fieldsList){><id>.set(__$result.<id>);
   '			<}>
   '			return __$result;
   '		}
   '		<}>);
   '		return new __$<sid>(<intercalate(", ", ["__$$entry"] + ["<id>.get()" | <id, _> <- fieldsList])>);
   '	}
   '
   '	@Override
   '    protected Token[] parsedTokens() {
   '        return new Token[]{ __$$entry };
   '    }
   '
   '    @Override
   '    protected Token[] allTokens() {
   '        return new Token[]{ __$$entry };
   '    }
   '}"           	
	when lrel[str, Type] formalsList := [<"<id>", typ> | aformals <- formals,(Formal) `<Type typ> <Id id>` <- aformals.formals],
		 lrel[str, Type] fieldsList := [<"<id>", ty> | (DeclInChoice) `abstract <Type ty> <Id id>` <- decls],
		 lrel[str, Type] allFieldsList := formalsList + [<id, ty> | <id, ty> <- fieldsList]
		 //,<_, map[loc, str] alternatives> := (<1,()> | <it.first + 1, it.second + (d@\loc:"$alternative<it.first>")> | (DeclInChoice) `<Type tp> <Arguments? args> <Size? sz>` <- decls)
		 ;

str compile(current:(TopLevelDecl) `struct <Id sid> <TypeFormals? typeFormals> <Formals? formals> <Annos? annos> { <DeclInStruct* decls> }`, rel[loc,loc] useDefs, map[loc, AType] types) =
   "public static final class __$<sid><javaTypeFormals> extends UserDefinedToken {
   '	<for ((DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? sideCondition>` <- decls, ty is anonymousType){><generateAnonymousType(ty, useDefs, types)>
   '	<}>
   '	<for (<id, typ> <- allFieldsList){>
   '		public final <generateNestType(typ, types)> <id>;
   '	<}>
   '	private __$<sid>(<intercalate(", ", ["<generateNestType(typ, types)> <id>" | <id, typ> <- allFieldsList])>){
   '	<for  (<id, _> <- allFieldsList){>	this.<id> = <id>;
   '	<}>	
   '	}
   '
   '	public static <javaTypeFormals> __$<sid><javaTypeFormalsNames> parse(ByteStream __$src, Context __$ctx<intercalate(", ", [""] +["<generateNestType(typ, types)> <id>" | <id, typ> <- formalsList] + ["BiFunction\<ByteStream, Context, <tf>\> parserFor<tf>" |Id tf <- typeFormalsList])>) throws ParseError {
   '		<for (aannos <- annos, (Anno) `<Id prop> = <Expr e>` <- aannos.annos){><compileAnno("<prop>", e, useDefs, types)>
   '		<}>
   '	<for (DeclInStruct d <- decls){>
   '		<compileDeclInStruct(d, "<sid>", [id | <id ,_>  <- formalsList], useDefs, types)><}>
   '		return new __$<sid>(<intercalate(", ", [id | <id, _> <- allFieldsList])>);
   '	}
   '
   '	@Override
   '    protected Token[] parsedTokens() {
   '        return new Token[]{<intercalate(", ", ["<id>" | <id, _, isToken, isTie> <- fieldsList, isToken, !isTie])>};
   '    }
   '
   '    @Override
   '    protected Token[] allTokens() {
   '        return new Token[]{<intercalate(", ", ["<id>" | <id, _, isToken, _> <- fieldsList, isToken])>};
   '    }
   '}"           	
	when lrel[str, Type] formalsList := [<"<id>", typ> | aformals <- formals,(Formal) `<Type typ> <Id id>` <- aformals.formals],
		 lrel[str, Type, bool, bool] fieldsList := 
		 	[<makeId(d.id), d.ty, d is token, (DeclInStruct) `<Type _> <DId _> <Arguments? _> <Size? _> byparsing (<Expr _>)`:= d>| DeclInStruct d <- decls],
		 lrel[str, Type] allFieldsList := formalsList + [<id, ty> | <id, ty, _, _> <- fieldsList],
		 list[Id] typeFormalsList := [t | atypeFormals <- typeFormals, t <- atypeFormals.typeFormals],
		 str javaTypeFormals := ((size(typeFormalsList) > 0)?"\<<intercalate(",", ["<tf> extends Token" | tf <- typeFormalsList])>\>":""),
		 str javaTypeFormalsNames := ((size(typeFormalsList) > 0)?"\<<intercalate(",", ["<tf>" | tf <- typeFormalsList])>\>":"")
		 ;		 
		 
str generateAnonymousType(current: (Type) `struct { <DeclInStruct* decls>}`, lrel[str, Type] formals, rel[loc,loc] useDefs, map[loc, AType] types) =
	"private static class <sid> extends UserDefinedToken {
	' 	<for (<id, typ> <- fieldsList){>public final <generateNestType(typ, types)> <id>;
    '	<}>
    '	private <sid>(<intercalate(", ", ["<generateNestType(typ, types)> <id>" | <id, typ> <- fieldsList])>){
    '	<for  (<id, _> <- fieldsList){>	this.<id> = <id>;
    '	<}>	
    '	}
    '
    '	public static <sid> parse(ByteStream __$src, Context __$ctx<for (<id, ty> <- formals){>, <generateNestType(ty, types)> <id><}>) throws ParseError {
    '	<for (DeclInStruct d <- decls){>
    '		<compileDeclInStruct(d, "<sid>", [], useDefs, types)><}>
    '		return new <sid>(<intercalate(", ", [id | <id, _> <- fieldsList])>);
    '	}
    '
    '	@Override
    '    protected Token[] parsedTokens() {
    '        return new Token[]{<intercalate(", ", ["<id>" | <id, _> <- tokensList])>};
    '    }
    '}"
	when str sid := generateNestType(current, types),
		 lrel[str, Type] fieldsList := [<makeId(d.id), d.ty> | DeclInStruct d <- decls],
		 lrel[str, Type] tokensList := [<makeId(d.id), d.ty> | DeclInStruct d <- decls, d is token]
		 ;

		 
str generateParsingInstruction(current: (Type) `byte []`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	= "<src>.readUnsigned(<compile(expr, DUMMY_DID, useDefs, types)>, <ctx>)"
	when listType(byteType(), n = just(expr)) :=  types[current@\loc];
	
str generateParsingInstruction(current: (Type) `byte []`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	= "TokenList.untilParseFailure(<src>, <ctx>, (<src1>, <ctx1>) -\> <src1>.readUnsigned(1, <ctx1>))"
	when listType(byteType(), n = nothing()) :=  types[current@\loc],
		 src1 := makeUnique(current, "__$src"),
		 ctx1 := makeUnique(current, "__$ctx");
	
str generateParsingInstruction(current: (Type) `<Type t> []`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	= "TokenList.times(<src>, <ctx>, (<src1>, <ctx1>) -\> <generateParsingInstruction(t, [], [], useDefs, types, src = "<src1>", ctx = "<ctx1>")>, <compile(expr, DUMMY_DID, useDefs, types)>.intValueExact())"
	when (Type) `byte` !:= t,
	     listType(_, n = just(expr)) :=  types[current@\loc],
		 src1 := makeUnique(t, "__$src"),
		 ctx1 := makeUnique(t, "__$ctx");
		 
str generateParsingInstruction(current: (Type) `<Type t> []`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	= "TokenList.untilParseFailure(<src>, <ctx>, (<src1>, <ctx1>) -\> <generateParsingInstruction(t, [], [], useDefs, types, src = "<src1>", ctx = "<ctx1>")>)"
	when (Type) `byte` !:= t,
		 listType(_, n = nothing()) :=  types[current@\loc],
		 src1 := makeUnique(t, "__$src"),
		 ctx1 := makeUnique(t, "__$ctx");		 
	
// TODO This should be forbidden by the type checker	
str generateParsingInstruction((Type) `byte`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx") {
	throw "Single byte cannot be used as a type";
}

str generateParsingInstruction(current: (Type) `<UInt v>`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	= "<src>.readUnsigned(<n>, <ctx>)"
	when listType(byteType(), n = just(n)) :=  types[current@\loc];

str generateParsingInstruction(current: (Type) `<ModuleId id> <TypeActuals? typeActuals>`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
 	= "<toJavaFQName(id)>.parse(<src>, <ctx><for (e <-args){>, <compile(e, DUMMY_DID, useDefs, types)><}><for (atypeActuals <- typeActuals, Type ta <- atypeActuals.typeActuals){>, (<src1>, <ctx1>) -\> <generateParsingInstruction(ta, [], [], useDefs, types, src = src1, ctx = ctx1)><}>)"
 	
 	when variableType(_) !:= types[current@\loc],
 		 src1 := makeUnique(id, "__$src"),
 		 ctx1 := makeUnique(id, "__$ctx");
 	
str generateParsingInstruction((Type) `<Id id>`, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
 	= "parserFor<id>.apply(<src>, <ctx>)"
 	when variableType(_) := types[id@\loc];
 	
 	
str generateParsingInstruction(current: (Type) `struct { <DeclInStruct* decls>}`, list[Expr] args, list[str] formalIds, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx")
	="<generateNestType(current, types)>.parse(<src>, <ctx><for (id <- formalIds){>, <id><}>)";
			 
default str generateParsingInstruction(Type t, list[Expr] args, list[str] _, rel[loc,loc] useDefs, map[loc, AType] types, str src = "__$src", str ctx = "__$ctx") {
	throw "Not yet generateParsingInstruction for type <t>";
}

str compileCondition((SideCondition) `? (<EqualityOperator eo> <Expr e>)`, DId this, Type thisType, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<makeId(this)>.sameBytes(<compile(e, this, useDefs, types)>)"
	when listType(byteType()) := types[e@\loc],
		 listType(byteType()) := types[thisType@\loc];

str generateSideCondition(current: (SideCondition) `? (<EqualityOperator eo> <Expr e>)`, str parentId, DId this, Type thisType, rel[loc,loc] useDefs, map[loc, AType] types) =
	"if (<op>(<compileCondition(current, this, thisType, useDefs, types)>)) {
    '	throw new ParseError(\"<parentId>.<makeId(this)>\", <makeId(this)>);
    '}"
	when op := (eo is equality?"!":"");

str generateSideCondition((SideCondition) `? (<Expr e>)`, str parentId, DId this, Type _, rel[loc,loc] useDefs, map[loc, AType] types) 
	= "if (!(<compile(e, this, useDefs, types)>)) {
    '	throw new ParseError(\"<parentId>.<makeId(this)>\", <makeId(this)>);
    '}"
	;

// Declarations in structs
		 
// Token field

// TODO Check if `while` does make sense for something that is not u8[].
// 		If not, enforce constraint in type checker (together with type of it)
str compileDeclInStruct(current:(DeclInStruct) `<Type ty>[]  <DId id> <Arguments? args> <Size? size> while (<Expr e>)`, str parentId, list[str] formalIds, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"<generateNestType(current.ty, types)> <makeId(id)> = TokenList.parseWhile(__$src, __$ctx,
    '	(s, c) -\> <generateParsingInstruction(ty, [e | aargs <- args, e <- aargs.args], [], useDefs, types, src = "s", ctx = "c")>,
    '	it -\> (<compile(e, id, useDefs, types)>)
    ');";
    
// TODO restrict in type checker that a byparsing-guarded token should not
//		have arguments nor size?     
str compileDeclInStruct(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> byparsing (<Expr e>)`, str parentId, list[str] formalIds, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"<generateNestType(ty, types)> <makeId(id)> = <generateParsingInstruction(ty, [a | aargs <- args, Expr a <- aargs.args], [], useDefs, types, src = "new ByteStream(<compile(e, id, useDefs, types)>)")>;" ;
		 	    
		 		 
default str compileDeclInStruct(current:(DeclInStruct) `<Type ty> <DId id> <Arguments? args> <Size? size> <SideCondition? sideCondition>`, str parentId, list[str] formalIds, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"<generateNestType(ty, types)> <makeId(id)> = <generateParsingInstruction(ty, [a | aargs <- args, Expr a <- aargs.args], formalIds, useDefs, types)>;
	'<for (sc <- sideCondition){ bprintln(sc);><generateSideCondition(sc, parentId, id, ty, useDefs, types)>
	'<}>";

// Computed field

str compileDeclInStruct(current:(DeclInStruct) `<Type ty> <Id id> = <Expr e>`, str parentId, list[str] formalIds, rel[loc,loc] useDefs, map[loc, AType] types) =		 
	"<generateNestType(ty, types)> <id> = <compile(e, DUMMY_DID, useDefs, types)>;";


// Expressions

str compile(current: (Expr) `<IntDecLiteral nat>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	 "NestBigInteger.of(<nat>)";
	 
str compile(current: (Expr) `<IntHexLiteral hex>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestBigInteger.of(<hex.number>)";

// TODO ask Davy about second argument
str compile(current: (Expr) `<BytesHexLiteral hex>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(<hex>, <len>)"
	when len := ((size("<hex>") == 3)?1:(size("<hex>") - 2)/2);
	
str compile(current: (Expr) `<BytesDecLiteral dec>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(<dec.number>, <len>)"
	when len := ((toInt("<dec.number>") - 1) / 255) + 1;
	
str compile(current: (Expr) `<BytesBitLiteral bits>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"NestValue.of(<bits>, 1)";
	//when noUnderscoreNat := replaceAll("<bits>","_","");
	
str compile(current: (Expr) `<BytesStringLiteral string>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(<string>, __$ctx)";
		
str compile(current: (Expr) `\< <{SingleHexIntegerLiteral ","}+ bytes> \>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"NestValue.of(new byte[]{<intercalate(", ", ["(byte) <b>" | b <- bytes])>})";

str compile(current: (Expr) `<StringLiteral lit>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = "<lit.chars>";

str compile(current: (Expr) `it`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"it";

str compile(current: (Expr) `this`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<makeId(this)>";
	
str compile(current: (Expr) `<Id id>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<id>"
	when (Id) `this` !:= id && (Id) `it` !:= id,
		 isTokenType(types[current@\loc]);
		 
str compile(current: (Expr) `<Id id>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<id>"
	when !isTokenType(types[current@\loc]);
		 
str compile(current: (Expr) `<Id id> <Arguments args>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<javaName>.<id>(<intercalate(", ", [compile(e, this, useDefs, types) | e <- args.args])>)"
	when funType(_, _, _, javaName) := types[id@\loc];
	
str compile(current: (Expr) `<Id id> <Arguments args>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = {
		throw "Not yet implemented";
	}
	when !(types[id@\loc] is funType);
	
str compile(current:(Expr) `<Expr e>[<Expr init> : <Expr end>]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"TokenList.of(__$ctx, IntStream.rangeClosed(<compile(end, this, useDefs, types)>.intValueExact(), <compile(e, this, useDefs, types)>.length()+(<compile(init, this, useDefs, types)>.intValueExact()))
	'	.boxed().sorted(Collections.reverseOrder()).map(i -\> <compile(e, this, useDefs, types)>.get(i)).toArray(i -\> new UnsignedBytes[i]))";
	
str compile(current: (Expr) `( <Type accuType> <Id accuId> = <Expr init> | <Expr update> | <Id loopVar> \<- <Expr source>)`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<compile(source, this, useDefs, types)>
    '    		.stream().reduce(<compile(init, this, useDefs, types)>.asValue(), (<accuId>, <loopVar>) -\>
    '    				(<compile(update, this, useDefs, types)>),
    '    			(x,y) -\> x)";

str compile(current: (Expr) `<Expr e>.as[int]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<compile(e, this, useDefs, types)>.asValue().asInteger()";

str compile(current: (Expr) `<Expr e>.as[str]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<compile(e, this, useDefs, types)>.asValue().asString().get()";

str compile(current: (Expr) `<Expr e>.as[<Type t>]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = {
	throw "Cannot cast to <t>";
} when (Type) `int` !:= t,
	   (Type) `str` !:= t;

str compile(current: (Expr) `<Expr e>.<Id field>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	"<compile(e, this, useDefs, types)>.<field>";

str compile(current: (Expr) `(<Expr e>)`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) = 
	compile(e, this, useDefs, types);
	
// TODO check semantics for >= and <= 
str compile(current: (Expr) `<Expr e1> <ComparatorOperator uo> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)>.compareTo(<compile(e2, this, useDefs, types)>) <uo> 0)";
		 
str compile(current: (Expr) `<Expr e1> <EqualityOperator uo> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<op>(<compile(e1, this, useDefs, types)>.sameBytes(<compile(e2, this, useDefs, types)>))"
	when op := (uo is equality?"":"!"),
		 listType(byteType()) := types[e1@\loc],
		 listType(byteType()) := types[e2@\loc];
			 
str compile(current: (Expr) `<Expr e1> <EqualityOperator uo> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<op>(<compile(e1, this, useDefs, types)>.equals(<compile(e2, this, useDefs, types)>))"
	when op := (uo is equality?"":"!"),
		 listType(byteType()) != types[e1@\loc],
		 listType(byteType()) != types[e2@\loc];

str compile(current: (Expr) `<Expr e>[<Expr index>]`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<compile(e, this, useDefs, types)>.get(<compile(index, this, useDefs, types)>.intValueExact())";

str compile(current: (Expr) `- <Expr e>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"<compile(e, this, useDefs, types)>.negate()";

str compileBinary(str op, Expr e1, Expr e2, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)>).asValue().<op>(<compile(e2, this, useDefs, types)>)";
		 
str compileBinaryArithmetic(str op, Expr e1, Expr e2, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)>).<op>(<compile(e2, this, useDefs, types)>)";
		 
str compileBinaryInfix(str op, Expr e1, Expr e2, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	"(<compile(e1, this, useDefs, types)> <op> <compile(e2, this, useDefs, types)>)";
	
str compile(current: (Expr) `<Expr e1> \>\> <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("shr", e1, e2, this, useDefs, types);

str compile(current: (Expr) `<Expr e1> \<\< <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("shl", e1, e2, this, useDefs, types);

str compile(current: (Expr) `<Expr e1> || <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinaryInfix("||", e1, e2, this, useDefs, types);

str compile(current: (Expr) `<Expr e1> && <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinaryInfix("&&", e1, e2, this, useDefs, types);

		 
str compile(current: (Expr) `<Expr e1> & <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("and", e1, e2, this, useDefs, types);
	
str compile(current: (Expr) `<Expr e1> | <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinary("or", e1, e2, this, useDefs, types);	
	
str compile(current: (Expr) `<Expr e1> - <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinaryArithmetic("subtract", e1, e2, this, useDefs, types);
	
str compile(current: (Expr) `<Expr e1> + <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinaryArithmetic("add", e1, e2, this, useDefs, types);
	
str compile(current: (Expr) `<Expr e1> * <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
	compileBinaryArithmetic("multiply", e1, e2, this, useDefs, types);
	
	
str compile(current: (Expr) `<Expr e1> ++ <Expr e2>`, DId this, rel[loc,loc] useDefs, map[loc, AType] types) =
    "TokenList.of(__$ctx, <compile(e1, this, useDefs, types)>, <compile(e2, this, useDefs, types)>)";
    
str compile(current: (Expr) e, DId this, rel[loc,loc] useDefs, map[loc, AType] types){
    throw "Expression not yet implemented: <e>";
}
		 
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

void compileBirdModule(start[Program] pt, TModel model, PathConfig pcfg) {
    str moduleName = "<pt.top.moduleName>";
    println("Compiling: <moduleName>");
    tuple[str fqn, str text] compiled = compile(pt.top, getUseDef(model), getFacts(model));
    println("Compiled");
    path = replaceAll(compiled.fqn, ".", "/") + ".java";
    println("Writing to: <pcfg.target + path>");
    writeFile(pcfg.target + path, compiled.text);
}
