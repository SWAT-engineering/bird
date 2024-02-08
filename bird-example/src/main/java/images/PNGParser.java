package images;

import engineering.swat.bird.generated.images.*;
import engineering.swat.nest.core.bytes.source.*;
import engineering.swat.nest.core.bytes.*;

import java.io.*;
import java.net.*;

public class PNGParser {
    
    public static void main(String[] args) throws IOException, URISyntaxException {
        try (var f = new FileInputStream("resources/test/truecolor.png")) {
            var sl = new ByteStream(ByteSliceBuilder.convert(f, new URI("unknown:///test.png")));
            System.out.println(PNG$.__$PNG.parse(sl, Context.DEFAULT));
            System.out.println("Done parsing, any bytes left: " + sl.hasBytesRemaining());
        }
    }
}
