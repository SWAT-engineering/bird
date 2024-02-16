module lang::bird::VisualizeGrammarBasic

import Content;
import util::IDEServices;

void visualize(str name, rel[tuple[loc,str], tuple[loc,str]] g) {
    nodes = g<0> + g<1>;
    lookup = nodes<1,0>;

    // note, this is only until new rascal release, 0.34.0 will introduce dagre, and this should just be a simple rascal function.
    index = "\<!DOCTYPE html\>
        '\<html lang=\"en\"\>
        '    \<head\>
        '        \<script src=\"https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.28.1/cytoscape.min.js\" 
        '           integrity=\"sha512-RcuA+PEnJcg1caTn53YLhZ3bYVFXphzcPL1BjBoAwFiA3bErav+AndZz1xrqpAtv/8Waep2X+9zn8KWpwacUSA==\" 
        '           crossorigin=\"anonymous\" referrerpolicy=\"no-referrer\"\>\</script\>
        '        \<script src=\"https://cdnjs.cloudflare.com/ajax/libs/dagre/0.8.5/dagre.min.js\" 
        '           integrity=\"sha512-psLUZfcgPmi012lcpVHkWoOqyztollwCGu4w/mXijFMK/YcdUdP06voJNVOJ7f/dUIlO2tGlDLuypRyXX2lcvQ==\" 
        '           crossorigin=\"anonymous\" referrerpolicy=\"no-referrer\"\>\</script\>
        '        \<script src=\" https://cdn.jsdelivr.net/npm/cytoscape-dagre@2.5.0/cytoscape-dagre.min.js \"\>\</script\>
        '
        '        \<title\><name> Grammar\</title\>
        '        \<style\>
        '            body2 {
        '                width:100%;
        '                height: 100%;
        '            }
        '            #graph {
        '                width: 90vw;
        '                height: 90vh;
        '                display: block;
        '            }
        '
        '        \</style\>
        '    \</head\>
        '    \<body\>
        '       \<h2\><name> Grammar\</h2\>
        '        \<div id=\"graph\"\>\</div\>
        '        \<script\>
        '            function edge(from, to) {
        '                return { data : { source: from, target: to}};
        '            }
        '            var cy = cytoscape({
        '                container: document.getElementById(\'graph\'),
        '                elements: {
        '                    nodes : [
        '                       <for (<l, n> <- nodes) {>
        '                           { data : { id: \'<n>\' }},
        '                       <}>
        '                    ],
        '                    edges: [
        '                       <for (<<_, f>, <_,t>> <- g) {>
        '                           { data :  { source: \'<f>\', target: \'<t>\' }},
        '                       <}>
        '                    ]
        '                },
        '                style: [
        '                    {
        '                        selector: \'node\',
        '                        style: {
        '                            \'background-color\': \'#fff\',
        '                            \'background-opacity\': 0,
        '                            \'text-valign\': \'center\',
        '                            \'text-halign\':\'center\',
        '                            \'label\': \'data(id)\',
        '                            \'text-background-color\': \'#ccc\',
        '                            \'text-background-opacity\': 0.5,
        '                            \'text-background-shape\': \'round-rectangle\',
        '                            \'text-background-padding\': \'0.1em\'
        '
        '                        }
        '                    },
        '
        '                    {
        '                        selector: \'edge\',
        '                        style: {
        '                            \'width\': 3,
        '                            \'line-color\': \'#aaa\',
        '                            \'target-arrow-color\': \'#aaa\',
        '                            \'target-arrow-shape\': \'triangle\',
        '                            \'curve-style\': \'bezier\'
        '                        }
        '                    }
        '                ],
        '                layout: {
        '                    name: \'dagre\',
        '                }
        '            });
        '            cy.on(\'tap\', \'node\', function (evt) {
        '                const node = evt.target;
        '                fetch(\'/go/\' + node.id());
        '            });
        '        \</script\>
        '    \</body\>
        '\</html\>";

    showInteractiveContent(content("<name>Grammar", Response (Request req) {
        switch(req) {
            case get("/"): return response(index);
            case get(str url): {
                if (/go\/<id:.*>$/ := url && {t} := lookup[id]) {
                    edit(t);
                    return plain("Yeah! <id>");
                }
                return plain("error");
            }
        }
        return plain("error");
    }, viewColumn=2, title="<name> Grammar"));
}