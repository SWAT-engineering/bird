module lang::bird::LanguageServer

import ParseTree;

import util::IDEServices;
import util::LanguageServer;
import util::Monitor;
import util::Reflective;
import vis::Graphs;
import IO;
import String;
import Set;

import lang::bird::Checker;
import lang::bird::Syntax;
import lang::bird::Generator2Nest;

set[LanguageService] birdLanguageContributor() {
    return {
        parsing(getBirdParser()),
        documentSymbol(birdOutliner),
        analysis(birdAnalyzer),
        build(birdBuilder),
        codeLens(birdLenses),
        execution(birdExecutor),
        inlayHint(birdHinter)
    };
}

list[DocumentSymbol] birdOutliner(start[Program] input) {
    jobStart("Bird Outliner");
    l = input.src.top;
    jobStep("Bird Outliner", l.file);
    list[DocumentSymbol] children = [];
    for (declaration <- input.top.declarations) {
        children += buildOutline(declaration);
    }
    jobEnd("Bird Outliner");
    return [symbol("<input.src.file>", DocumentSymbolKind::file(), input.src, children=children)];
}

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`struct <Id id> <TypeFormals? _> <Formals? _> <Annos? _> { <DeclInStruct* declarations> }`)
    = [symbol("struct <id>", struct(), current.src, children=[*buildOutline(decl) | decl <- declarations])];

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`choice <Id id> <Formals? _> <Annos? _> { <DeclInChoice* declarations> }`)
    = [symbol("choice <id>", array(), current.src, children=[*buildOutline(decl) | decl <- declarations])];

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`@(<JavaId _>) <Type _> <Id id> <Formals? _>`)
    = [symbol("function <id>", method(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInStruct)`<Type _> <DId id> <Arguments? _> <Size? _> <SideCondition? _>`)
    = [symbol("token <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInStruct)`<Type _> <Id id> = <Expr _>`)
    = [symbol("token <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInChoice)`abstract <Type _> <Id id>`)
    = [symbol("abstract <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInChoice)`<Type tp> <Arguments? _> <Size? _>`)
    = [symbol("token <tp>", field(), current.src)];

Summary birdAnalyzer(loc l, start[Program] input) = check(l, input)<1>;

tuple[TModel, Summary] check(loc l, start[Program] input) {
    jobStart("Bird Type checker");
    jobStep("Bird Type checker", l.file);
    pcfg = calculatePathConfig(input);
    tm = birdTModelFromTree(input, pathConf = pcfg);
    jobEnd("Bird Type checker");
    return <tm, summary(l,
        messages = {<message.at, message> | message <- tm.messages, message.at.top == l.top},
        definitions = tm.useDef,
        references = tm.useDef<1,0>)>;
}

Summary birdBuilder(loc l, start[Program] input) {
    <tm, sm> = check(l, input);
    if (tm.messages == []) {
        jobStart("Bird compiler");
        try {
            pcfg = calculatePathConfig(input);
            compileBirdModule(input, tm, "engineering.swat.bird.generated", pcfg);
        }
        catch e: {
            println("Bird compilation failed: <e>");
        }
        jobEnd("Bird compiler");
    }
    return sm;
} 

PathConfig calculatePathConfig(start[Program] input) {
    loc srcPath = input.src.top;
    for (Id _ <- input.top.moduleName.moduleName) {
        srcPath = srcPath.parent;
    }
    loc outputPath = srcPath.parent + "java";
    return pathConfig(srcs=[srcPath], target=outputPath);
}

list[InlayHint] birdHinter(start[Program] input) = [hint(s.src, " \<<generateHexArray(s)>\>", \type(), atEnd = true) | /BytesStringLiteral s := input];

str generateHexArray((BytesStringLiteral)`"<StringCharacter* chars>"`) = intercalate(", ", [*toHex(c) | c <- chars]);

list[str] toHex(StringCharacter chs) = [toHex(c) | c <- chars("<chs>")];
list[str] HEX_CHARS = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"];
str toHex(int n) = "0x<HEX_CHARS[n / 16]><HEX_CHARS[n % 16]>";

data Command = visualizeDependencies(loc decl, str name);

lrel[loc, Command] birdLenses(start[Program] input) {
    return [ <d.src,  visualizeDependencies(d.src, "<d.id>", title="Visualize <d.id>")> | /TopLevelDecl d := input, !(d is funDecl)];
}

value birdExecutor(visualizeDependencies(loc decl, str name)) {
    gg = buildGrammarGraph(decl);
    showInteractiveContent(graph(gg, cfg=cytoGraphConfig(\layout=defaultDagreLayout())), viewColumn=2, title="<name> Grammar");
    return ("result": true);
}

alias GrammarGraph = rel[tuple[loc, str], tuple[loc, str]];

GrammarGraph buildGrammarGraph(loc decl) {
    input = getBirdParser()(readFile(decl.top), decl.top);
    pcfg = calculatePathConfig(input);
    tm = birdTModelFromTree(input, pathConf = pcfg);
    GrammarGraph result = {};
    todo = {decl};
    done = {};
    iprintln(tm.definitions<0>);
    while (todo != {}) {
        <j, todo> = takeOneFrom(todo);
        done += j;
        println("Processing <j>");
        if (j notin tm.definitions) {
            println("Nothing");
            continue;
        }
        def = tm.definitions[j];
        lhs = <def.defined, def.id>;
        for (<_, _, fieldId(), _, /tp:structType(tpn, _)> <- tm.defines[def.defined]) {

            if (f:<_, tpn, tpn, structId(), loc tpl, defType(tp)> <- tm.defines) {
                println(f);
                result += <lhs, <tpl, tpn>>;
                if (tpl notin done) {
                    todo += tpl;
                }
            }
        }
    }
    return result;
}


default value birdExecutor(value v) {
    throw  "Missing case for <v>";
}

void main() {
    registerLanguage(
        language(
            getProjectPathConfig(|project://bird-ide|, mode=interpreter())[messages=[]],
            "Bird",
            {"bird"},
            "lang::bird::LanguageServer",
            "birdLanguageContributor"));
}

