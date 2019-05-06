package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import engineering.swat.nest.examples.formats.jpeg.JPEG.Format;
import engineering.swat.nest.examples.formats.png.PNG.PNG$;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        NestedFiles<PNGorJPEG> result = NestedFiles.parse(wrap(TEST_DATA), Context.DEFAULT, PNGorJPEG::parse).get();
        assertEquals(data_end, result.size().intValueExact());
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100;  i++ ) {
          new FatLikeNestingTests().parseNestedPNGorJPEG();
        }

    }


    private static class PNGorJPEG extends UserDefinedToken {
        private final Token entry;

        private PNGorJPEG(Token entry) {
            this.entry = entry;
        }

        public static Optional<PNGorJPEG> parse(ByteStream source, Context ctx) {
            Optional<Token> entry = Choice.between(source, ctx,
                    Case.of(PNG$::parse, x -> {}),
                    Case.of(Format::parse, x -> {})
            );
            if (!entry.isPresent()) {
                ctx.fail("PNGorJPEG choice failed {}", source);
                return Optional.empty();
            }
            return Optional.of(new PNGorJPEG(entry.get()));
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

        public static <T extends Token> Optional<NestedFiles<T>> parse(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<T>> tParser) {
            ctx = ctx.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            Optional<UnsignedBytes> header = source.readUnsigned(NestBigInteger.of(3), ctx);
            if (!header.isPresent() || !header.get().asString().get().equals("HDR")) {
                ctx.fail("NestedFiles.header {}", header);
                return Optional.empty();
            }
            TokenList<NestedFile<T>> files = TokenList.untilParseFailure(source, ctx, (s,c) -> NestedFile.parse(s, c, tParser));
            return Optional.of(new NestedFiles<>(header.get(), files));
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
        public static <T extends Token> Optional<NestedFile<T>> parse(ByteStream source, Context ctx, BiFunction<ByteStream, Context, Optional<T>> tParser) {
            final Optional<UnsignedBytes> size = source.readUnsigned(NestBigInteger.of(4), ctx);
            if (!size.isPresent()) {
                ctx.fail("NestedFile.size missing {}", source);
                return Optional.empty();
            }
            final Optional<UnsignedBytes> raw = source.readUnsigned(size.get().asValue().asInteger(Sign.UNSIGNED), ctx);
            if (!raw.isPresent()) {
                ctx.fail("NestedFile.raw missing {}", source);
                return Optional.empty();
            }
            final Optional<T> parsedFile = tParser.apply(new ByteStream(raw.get()), ctx);
            if (!parsedFile.isPresent()) {
                ctx.fail("NestedFile.parsedFile missing {}", source);
                return Optional.empty();
            }
            return Optional.of(new NestedFile<>(size.get(), raw.get(), parsedFile.get()));
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
