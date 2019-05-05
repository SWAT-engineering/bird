package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import engineering.swat.nest.examples.formats.jpeg.JPEG;
import engineering.swat.nest.examples.formats.png.PNG;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class FatLikeNestingTests {
    private static final int data_end;
    private static final byte[] TEST_DATA = new byte[16*1024*1024];

    static {
        ByteBuffer target = ByteBuffer.wrap(TEST_DATA);
        target.order(ByteOrder.LITTLE_ENDIAN);
        target.put(new byte[] {'H','D','R'});
        produceFiles().forEach(b -> writeFile(b, target));
        data_end = target.position();
        Arrays.fill(TEST_DATA, target.position(), TEST_DATA.length - 1, (byte)0xFF);
    }

    private static void writeFile(byte[] data, ByteBuffer target) {
        target.putInt(data.length);
        target.put(data);
    }

    private static Iterable<byte[]> produceFiles() {
        List<byte[]> result = Stream
                .concat(CommonTestHelper.findResources(".jpg"), CommonTestHelper.findResources(".png")).map(path -> {
                    try {
                        return Files.readAllBytes(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
        Collections.shuffle(result);
        return result;
    }

    @Test
    void parseNestedPNGorJPEG() {
        NestedFiles<PNGorJPEG> result = NestedFiles.parse(wrap(TEST_DATA), Context.DEFAULT, PNGorJPEG::parse);
        assertEquals(data_end, result.size().intValueExact());
    }


    private static class PNGorJPEG extends UserDefinedToken {
        private final Token entry;

        private PNGorJPEG(Token entry) {
            this.entry = entry;
        }

        public static PNGorJPEG parse(ByteStream source, Context ctx) {
            Token entry = Choice.between(source, ctx,
                    Case.of(PNG.PNG$::parse, x -> {}),
                    Case.of(JPEG.Format::parse, x -> {})
            );
            return new PNGorJPEG(entry);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return entry.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return entry.size();
        }
    }


    private static class NestedFiles<T extends Token> extends UserDefinedToken {
        public final UnsignedBytes header;
        public final TokenList<NestedFile<T>> files;

        private NestedFiles(UnsignedBytes header, TokenList<NestedFile<T>> files) {
            this.header = header;
            this.files = files;
        }

        public static <T extends Token> NestedFiles<T> parse(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> tParser) {
            ctx = ctx.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            UnsignedBytes header = source.readUnsigned(NestBigInteger.of(3), ctx);
            if (!header.asValue().asString().equals("HDR")) {
                throw new ParseError("NestedFiles.header", header);
            }
            TokenList<NestedFile<T>> files = TokenList.untilParseFailure(source, ctx, (s,c) -> NestedFile.parse(s, c, tParser));
            return new NestedFiles<>(header, files);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(header, files);
        }

        @Override
        public NestBigInteger size() {
            return header.size().add(files.size());
        }
    }

    private static class NestedFile<T extends Token> extends UserDefinedToken {
        public final UnsignedBytes size;
        public final UnsignedBytes raw;
        public final T parsedFile;

        private NestedFile(UnsignedBytes size, UnsignedBytes raw, T parsedFile) {
            this.size = size;
            this.raw = raw;
            this.parsedFile = parsedFile;
        }
        public static <T extends Token> NestedFile<T> parse(ByteStream source, Context ctx, BiFunction<ByteStream, Context, T> tParser) {
            final UnsignedBytes size = source.readUnsigned(NestBigInteger.of(4), ctx);
            final UnsignedBytes raw = source.readUnsigned(size.asValue().asInteger(Sign.UNSIGNED), ctx);
            final T parsedFile = tParser.apply(new ByteStream(raw), ctx);
            return new NestedFile<>(size, raw, parsedFile);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(size, raw);
        }

        @Override
        public NestBigInteger size() {
            return size.size().add(raw.size());
        }
    }

}
