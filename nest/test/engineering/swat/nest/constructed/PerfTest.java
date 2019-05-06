package engineering.swat.nest.constructed;

import java.io.IOException;

public class PerfTest {
    public static void main(String[] args) throws IOException {
        System.in.read();
        for (int i = 0; i < 100;  i++ ) {
            new FatLikeNestingTests().parseNestedPNGorJPEG();
        }

    }

}
