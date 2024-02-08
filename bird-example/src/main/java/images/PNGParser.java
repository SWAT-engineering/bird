package images;

import engineering.swat.bird.generated.images.*;
import engineering.swat.nest.core.bytes.source.*;
import engineering.swat.nest.core.bytes.*;

import java.io.*;
import java.net.*;

public class PNGParser {
    
    public static void main(String[] args) throws IOException, URISyntaxException {
        var sl = openTestPNG();
        var p = PNG$.__$PNG.parse(sl, Context.DEFAULT);
        System.out.println(p);
        System.out.println("Done parsing, all bytes consumed: " + !sl.hasBytesRemaining());
    }

    private static ByteStream openTestPNG() throws IOException, URISyntaxException {
        try (var f = new FileInputStream("resources/test/truecolor.png")) {
            return new ByteStream(ByteSliceBuilder.convert(f, new URI("unknown:///test.png")));
        }
    }
}
