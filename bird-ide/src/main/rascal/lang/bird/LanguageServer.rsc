module lang::bird::LanguageServer

import ParseTree;

import util::FileSystem;
import util::IDEServices;
import util::LanguageServer;
import util::Monitor;
import util::Reflective;
import vis::Graphs;
import IO;
import Location;
import Relation;
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
        inlayHint(birdHinter),
        callHierarchy(birdCallHierarchy, birdCalls)
    };
}

DocumentSymbolKind roleToSymbolKind(funId()) = \function();
DocumentSymbolKind roleToSymbolKind(structId()) = \struct();
DocumentSymbolKind roleToSymbolKind(variableId()) = \variable();
DocumentSymbolKind roleToSymbolKind(consId()) = \constructor();
DocumentSymbolKind roleToSymbolKind(fieldId()) = \field();
DocumentSymbolKind roleToSymbolKind(moduleId()) = \module();
DocumentSymbolKind roleToSymbolKind(paramId()) = \variable();

Focus computeFocusList(loc l) = computeFocusList(parse(#start[Program], l.top), l.begin.line, l.begin.column);

list[CallHierarchyItem] birdCallHierarchy(Focus focus) {
    tm = birdTModelFromTree(focus[-1]);

    if (defs:{_, *_} := tm.useDef[focus[0]@\loc]) {
        return [itemForLoc(d) | d <- defs];
    }

    if (TopLevelDecl decl <- focus) {
        // at definition
        return [itemForLoc(decl@\loc)];
    }

    return [];
}

lrel[CallHierarchyItem, loc] functionCalls(TopLevelDecl scope, TModel tm)
    = [<ci, id@\loc> | /(Expr) `<Id id> <Arguments _>` := scope, def <- tm.useDef[id@\loc], ci := itemForLoc(def)];

lrel[CallHierarchyItem, loc] parserCalls(TopLevelDecl scope, TModel tm)
    = [<ci, id@\loc> | /ModuleId id := scope, def <- tm.useDef[id@\loc], ci := itemForLoc(def)];

lrel[CallHierarchyItem, loc] birdCalls(callHierarchyItem(str name, _, loc defined, _), incoming()) {
    start[Program] prog = parse(#start[Program], defined.top);
    modName = "<prog.top.moduleName>";
    projFolder = defined.top;
    for (_ <- [0..size(findAll(modName, "::")) + 1]) {
        projFolder = projFolder.parent;
    }

    lrel[CallHierarchyItem, loc] calls = [];
    for (loc f <- find(projFolder, "bird"), contains(readFile(f), name)) {
        tm = birdTModelFromTree(parse(#start[Program], f));
        prog = parse(#start[Program], f);
        calls += [<ci, u> | /TopLevelDecl scope := prog, u <- invert(tm.useDef)[defined], isContainedIn(u, scope@\loc), ci := itemForLoc(scope@\loc)];
    }
    return calls;
}

lrel[CallHierarchyItem, loc] birdCalls(callHierarchyItem(_, _, loc defined, _), outgoing()) {
    tree = parse(#start[Program], defined.top);
    tm = birdTModelFromTree(tree);
    if (/TopLevelDecl scope := tree, defined := scope@\loc) {
        return functionCalls(scope, tm) + parserCalls(scope, tm);
    }
    return [];
}

CallHierarchyItem itemForLoc(loc l)
    = callHierarchyItem(def.id, roleToSymbolKind(def.idRole), l, l) // TODO Correct name of location
    when tm := birdTModelFromTree(parse(#start[Program], l.top))
       , def := tm.definitions[l]
       ;

@synopsis{Resolve all reachable ingoing/outgoing calls.}
rel[CallHierarchyItem, CallHierarchyItem] computeCallHierarchy(loc file, int line, int col) {
    tree = parse(#start[Program], file);
    focus = computeFocusList(tree, line, col);
    tm = birdTModelFromTree(tree);
    set[CallHierarchyItem] roots = {i | i <- birdCallHierarchy(focus)};
    rel[CallHierarchyItem, CallHierarchyItem] hierarchy = {};
    rel[CallHierarchyItem, CallHierarchyItem] oldHierarchy = {};
    do {
        set[CallHierarchyItem] newItems = (hierarchy<0> + hierarchy<1> + roots) - (oldHierarchy<0> + oldHierarchy<1>);
        oldHierarchy = hierarchy;
        for (current <- newItems) {
            focus = computeFocusList(tree, current.src.begin.line, current.src.begin.column);
            hierarchy += {<\in, current> | \in <- birdCalls(current, incoming())<0>};
            hierarchy += {<current, out> | out <- birdCalls(current, outgoing())<0>};
        }
    } while (hierarchy != oldHierarchy);

    print("Roots: ");
    iprintln(hierarchy<0> - hierarchy<1>);
    println();

    print("Leaves: ");
    iprintln(hierarchy<1> - hierarchy<0>);
    println();

    return hierarchy;
}

Content visualizeHierarchy(rel[CallHierarchyItem, CallHierarchyItem] hierarchy) = graph(toList(hierarchy));

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
    showInteractiveContent(graph(gg, \layout=defaultDagreLayout()), viewColumn=2, title="<name> Grammar");
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


list[loc] libs = [
    |jar+project://bird-core/target/lib/typepal.jar!/src|
];

void main() {
    registerLanguage(
        language(
            pathConfig(srcs=[|project://bird-core/src/main/rascal|, |project://bird-ide/src/main/rascal|, *libs]),
            "Bird",
            {"bird"},
            "lang::bird::LanguageServer",
            "birdLanguageContributor"));
}
