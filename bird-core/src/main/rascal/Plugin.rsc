module Plugin

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::bird::Generator;
import ParseTree;

import IO;
import util::IDE;
import String;

import util::Reflective;
import lang::manifest::IO;

private str LANG_NAME = "BIRD";

Contribution commonSyntaxProperties 
    = syntaxProperties(
        fences = {<"{","}">,<"(",")">}, 
        lineComment = "//", 
        blockComment = <"/*","*","*/">
    );
    
Tree checkBird(Tree input){
    model = birdTModelFromTree(input); // your function that collects & solves
    types = getFacts(model);
  
  return input[@messages={*getMessages(model)}]
              [@hyperlinks=getUseDef(model)]
              [@docs=(l:"<prettyPrintAType(types[l])>" | l <- types)]
         ; 
}


Tree checkBird2(Tree input){
    model = birdTModelFromTree(input); // your function that collects & solves
    types = getFacts(model);
     
    Tree newInput = visit(input){
    	case (Type) `<Id id>` => (Type) `<Id id> \< \>`
    		when bprintln("t: <types[id@\loc]>")
    			 //structType(_) := types[id@\loc]
    };
    
    println(newInput);
    
    model = birdTModelFromTree(newInput); // your function that collects & solves
    types = getFacts(model);
    
    return newInput[@messages={*getMessages(model)}]
              [@hyperlinks=getUseDef(model)]
              [@docs=(l:"<prettyPrintAType(types[l])>" | l <- types)]
         ; 
}


Contribution compiler = builder(set[Message] (Tree tree) {
	  if (start[Program] prog := tree) {
        loc l = prog@\loc.top;
        l.extension = "java";
        <package, newprog> = compileBird(prog);
        str path = ((package =="")?"/":"<replaceAll(package, "." , "/")>/");
        newLoc =  |project://bird-core/bird-output/engineering/swat/formats<path><l.file>|;
        writeFile(newLoc, newprog);
        return {};
      }
      return {error("Not a <LANG_NAME> program", tree@\loc)};
   });
 
/*
void main() {
	registerLanguage(LANG_NAME, "bird", start[Program](str src, loc org) {
		return parse(#start[Program], src, org);
 	});
	
	registerContributions(LANG_NAME, {
        commonSyntaxProperties,
        //compiler,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkBird)
    });
}*/