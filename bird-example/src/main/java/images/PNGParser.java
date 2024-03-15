package images;

import engineering.swat.bird.generated.images.*;
import engineering.swat.nest.core.bytes.source.*;
import engineering.swat.nest.core.bytes.*;

import java.io.*;
import java.net.*;

public class PNGParser {
    
    public static void main(String[] args) throws IOException, URISyntaxException {
        var png = openTestPNG();
        var jpg = openTestJPG();
        try {
            System.out.println(PNG$.__$PNG.parse(png, Context.DEFAULT));
            System.out.println("Done parsing, all bytes consumed: " + !png.hasBytesRemaining());
        } catch (Exception e) {
            System.err.println("Failure to parse png: " + e);
        }
        try {
            System.out.println(PNG$.__$PNG.parse(jpg, Context.DEFAULT));
            System.out.println("Done parsing, all bytes consumed: " + !jpg.hasBytesRemaining());
        } catch (Exception e) {
            System.err.println("Failure to parse jpg: " + e);
        }
    }

    private static ByteStream openTestPNG() throws IOException, URISyntaxException {
        try (var f = new FileInputStream("resources/test/truecolor.png")) {
            return new ByteStream(ByteSliceBuilder.convert(f, new URI("unknown:///test.png")));
        }
    }

    private static ByteStream openTestJPG() throws IOException, URISyntaxException {
        try (var f = new FileInputStream("resources/test/20160108-162501.jpg")) {
            return new ByteStream(ByteSliceBuilder.convert(f, new URI("unknown:///test.jpg")));
        }
    }
}
