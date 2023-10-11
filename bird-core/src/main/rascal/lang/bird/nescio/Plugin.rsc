module lang::bird::nescio::Plugin

import lang::nescio::API;
import lang::nescio::Checker;

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::bird::Generator2Nest;
import lang::bird::nescio::NescioPlugin;
import lang::bird::nescio::PathCompiler;

import ParseTree;
import IO;
import util::IDE;
import util::Reflective;
import lang::manifest::IO;
import List;

private str BIRD_LANG_NAME = "bird";
private str NESCIO_LANG_NAME = "nescio";

data BirdNescioManifest
 = birdNescioManifest(
      list[str] Source = ["src"],
      str Target = "generated"
	);
	
data PathConfig(str basePkg = "engineering.swat.bird.generated");
 
private loc configFile(loc file) =  project(file) + "META-INF" + "RASCAL.MF"; 

private loc project(loc file) {
   assert file.scheme == "project";
   return |project:///|[authority = file.authority];
}

PathConfig getDefaultPathConfig() = pathConfig(srcs = [], libs = []);

PathConfig config(loc file) {
   assert file.scheme == "project";

   p = project(file);
   cfgFile = configFile(file);
   mf = readManifest(#BirdNescioManifest, cfgFile); 
   
   cfg = getDefaultPathConfig();
   
   cfg.srcs += [ p + s | s <- mf.Source];
   
   cfg.srcs = [s | s <- toSet(cfg.srcs)];
   
   if (/^\|/ := mf.Target) {
      cfg.target = readTextValueString(#loc, mf.Target);
   }
   else {
      cfg.target = p + mf.Target;
   }
   
   return cfg;
}

Contribution commonSyntaxProperties 
    = syntaxProperties(
        fences = {<"{","}">,<"(",")">}, 
        lineComment = "//", 
        blockComment = <"/*","*","*/">
    );
    
Tree checkBird(Tree input){
    model = birdTModelFromTree(input, pathConf = config(input@\loc)); // your function that collects & solves
    types = getFacts(model);
  
  return input[@messages={*getMessages(model)}]
              [@hyperlinks=getUseDef(model)]
              [@docs=(l:"<prettyPrintAType(types[l])>" | l <- types)]
         ; 
}


set[Message] buildBird(start[Program] input) {
  pcfg = config(input@\loc);
  model = birdTModelFromTree(input, pathConf = pcfg);
  if (getMessages(model) == []) {
  	try
  		compileBirdModule(input, model, pcfg.basePkg, pcfg);
  	catch x: {
  		println("Exception thrown: <x>");
  	}
  }
  return {*getMessages(model)};
}

Tree checkNescio(Tree input) = {
	pcfg = config(input@\loc);
	LanguageConf birdConf = languageConf(birdGraphCalculator(pcfg), buildBirdModulesComputer(pcfg.srcs), buildBirdModuleMapper);
    model = nescioTModelFromTree(input, pathConf = pcfg, langsConfig = (BIRD_LANG_NAME:birdConf)); // your function that collects & solves
    types = getFacts(model);
  
  return input[@messages={*getMessages(model)}]
              [@hyperlinks=getUseDef(model)]
              [@docs=(l:"<prettyPrintAType(types[l])>" | l <- types)]
         ; 
}; 

set[Message] buildNescio(Tree input) {
  pcfg = config(input@\loc);
  LanguageConf birdConf = languageConf(birdGraphCalculator(pcfg), buildBirdModulesComputer(pcfg.srcs), buildBirdModuleMapper);
  model = nescioTModelFromTree(input, pathConf = pcfg, langsConfig = (BIRD_LANG_NAME:birdConf));
  if (getMessages(model) == []) {
  	try
  		compileNescioForBird(input, pcfg.basePkg, pcfg);
  	catch x: {
  		throw x;
  	}
  }
  return {*getMessages(model)};
}


void main() {
	println("Registering plugin...");
	
	registerLanguage(NESCIO_LANG_NAME, "nescio", Tree(str src, loc org) {
		return parseNescio(src, org);
 	});
 	
	registerContributions(NESCIO_LANG_NAME, {
        commonSyntaxProperties,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkNescio),
        builder(buildNescio)
    });

	registerLanguage(BIRD_LANG_NAME, "bird", Tree(str src, loc org) {
		return parse(#start[Program], src, org);
 	});
 	
 	registerContributions(BIRD_LANG_NAME, {
        commonSyntaxProperties,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkBird),
        builder(buildBird)
    });
}