package engineering.swat.nest.tests;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import engineering.swat.bird.generated.AorB$.__$AorB;
import engineering.swat.bird.generated.JPEG$;
import engineering.swat.bird.generated.PNG$;
import engineering.swat.bird.generated.fatLikeNesting1$.__$NestedFiles;
import engineering.swat.bird.generated.fatLikeNesting1$.__$PNGorJPEG;
import engineering.swat.bird.generated.linkedList1$.__$LinkedListEntry;
import engineering.swat.bird.generated.rep1$.__$Rep1;
import engineering.swat.bird.generated.rep1$.__$Rep2;
import engineering.swat.bird.generated.typeParameters1$.__$GenT;
import engineering.swat.bird.generated.typeParameters1$.__$Main;
import engineering.swat.bird.generated.typeParameters1$.__$Main2;
import engineering.swat.bird.generated.varint$;
import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.tokens.BottomUpTokenVisitor;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;


public class AllTests {
	@ParameterizedTest
	@MethodSource("jpegProvider")
	public void jpegFilesSucceed(Path jpegFile) throws IOException, URISyntaxException {
		ByteStream stream = new ByteStream(ByteSliceBuilder.convert(Files.newInputStream(jpegFile), jpegFile.toUri()));
		assertNotNull(JPEG$.__$Format.parse(stream, Context.DEFAULT));
		assertFalse(stream.hasBytesRemaining(), "Did not consume the whole file: " + stream.getOffset() + " of " + Files.size(jpegFile));
	}
	
	private static Stream<Path> jpegProvider() {
		return CommonTestHelper.findResources(".jpg");
	}
	

	@ParameterizedTest
	@MethodSource("pngProvider")
	public void pngFilesSucceed(Path pngFile) throws IOException, URISyntaxException {
		ByteStream stream = new ByteStream(ByteSliceBuilder.convert(Files.newInputStream(pngFile), pngFile.toUri()));
		assertNotNull(PNG$.__$PNG.parse(stream, Context.DEFAULT));
		assertFalse(stream.hasBytesRemaining(), "Did not consume the whole file: " + stream.getOffset() + " of " + Files.size(pngFile));
	}
	
	private static Stream<Path> pngProvider() {
        return CommonTestHelper.findResources(".png");
	}
	

    private static final long[] TEST_VALUES = {1, 32, 128, 255, 0x1FF, 0xFFFF, 1L << 18, 1L << 60, 0xAAABBCCCCL, 0xABCDEF0123L, 5463458053L};

    private static LongStream getTestValues() {
        return LongStream.of(TEST_VALUES);
    }
    
    @ParameterizedTest
    @MethodSource("getTestValues")
    void leb128Test(long value) {
        NestBigInteger result = varint$.__$LEB128.parse(wrap(leb64(value)), Context.DEFAULT).value;
        assertEquals(NestBigInteger.of(value), result, String.format("Expected: 0x%x got 0x%x", value, result.longValueExact()));
    }

    @ParameterizedTest
    @Disabled
    @MethodSource("getTestValues")
    void prefixTest(long value) {
        NestBigInteger result = varint$.__$PrefixVarint.parse(wrap(prefix(value)), Context.DEFAULT).value;
        assertEquals(NestBigInteger.of(value), result, String.format("Expected: 0x%x got 0x%x", value, result.longValueExact()));
    }
    
    private static byte[] leb64(long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        do {
            int newByte = (int)(value & 0x7F) ;
            value >>>= 7;
            if (value != 0)
                newByte |= 0x80; // set high order bit to signal more bytes comint
            output.write(newByte);
        } while (value != 0);
        return output.toByteArray();
    }

    private static byte[] prefix(long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int bits = 64 - Long.numberOfLeadingZeros(value | 1);

        int bytes = 1 + (bits - 1) / 7;

        if (bits > 56) {
            output.write(0);
            bytes = 8;
        } else {
            value = (2 * value + 1) << (bytes - 1);
        }
        for (int n = 0; n < bytes; n++) {
            output.write((int)(value & 0xff));
            value >>>= 8;
        }
        return output.toByteArray();
    }
    
    
    @Test
    void parseGeneratedUnevenBytes() {
    	engineering.swat.bird.generated.while1$.__$WhileUnevenParse parsed = engineering.swat.bird.generated.while1$.__$WhileUnevenParse.parse(wrap(1, 3, 5, 7, 8), Context.DEFAULT);
        assertEquals(4, parsed.contents.length());
        assertEquals(8, parsed.terminatedAt.getByteAt(NestBigInteger.ZERO));
    }
    
    @Test
    void tieWorks() {
    	engineering.swat.bird.generated.tie1$.__$Tie1 parsed = engineering.swat.bird.generated.tie1$.__$Tie1.parse(wrap(1, 2, 3, 4), Context.DEFAULT);
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data1.asValue().asInteger().intValueExact());
        assertEquals(0x03 << 8 | 0x04, parsed.other.data2.asValue().asInteger().intValueExact());
    }

    @Test
    void tieFlipGeneratedWorks() {
    	engineering.swat.bird.generated.tie1$.__$Tie2 parsed = engineering.swat.bird.generated.tie1$.__$Tie2.parse(wrap(1, 2, 3, 4), Context.DEFAULT);
        assertEquals(4, parsed.size().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.data1.asValue().asInteger().intValueExact());
        assertEquals(0x01 << 8 | 0x02, parsed.other.data2.asValue().asInteger().intValueExact());
    }
	@Test
	void checkGeneratedCyclicNestingWorks() {
		byte[] input = "Hcdcdeefdeggh0".getBytes(StandardCharsets.US_ASCII);
		assertEquals(input.length, engineering.swat.bird.generated.nesting_and_cycles$.__$Start.parse(wrap(input), Context.DEFAULT).size().intValueExact());
		
	}

	private static final byte[] TEST_DATA = new byte[512];

    static {
        Arrays.fill(TEST_DATA, (byte)0xFF); // fill the array with junk that we shouldn't parse
        // HEAD of chain 1
        TEST_DATA[0] = 0; // next pointer
        TEST_DATA[1] = 12; // continue at byte 12
        TEST_DATA[2] = 0;
        TEST_DATA[3] = 0;
        TEST_DATA[4] = 0;
        TEST_DATA[5] = 2; // value is 2

        TEST_DATA[12] = 0; // next pointer
        TEST_DATA[13] = 6; // continues at byte 6
        TEST_DATA[14] = 0;
        TEST_DATA[15] = 0;
        TEST_DATA[16] = 0;
        TEST_DATA[17] = 3; // value is 3

        TEST_DATA[6] = 0; // next pointer
        TEST_DATA[7] = 0; // points nothing, so end of chain
        TEST_DATA[8] = 0;
        TEST_DATA[9] = 0;
        TEST_DATA[10] = 0;
        TEST_DATA[11] = 4; // value is 4


        // HEAD of chain 2
        TEST_DATA[18] = 0; // next pointer
        TEST_DATA[19] = 58;
        TEST_DATA[20] = 0;
        TEST_DATA[21] = 0;
        TEST_DATA[22] = 0;
        TEST_DATA[23] = 10;

        TEST_DATA[58] = 0; // leaf
        TEST_DATA[59] = 0;
        TEST_DATA[60] = 0x10;
        TEST_DATA[61] = 0x10;
        TEST_DATA[62] = 0x10;
        TEST_DATA[63] = 0x10;
    }

    @Test
    void testWorkingSeekingLinkedList() throws IOException {
        assertEquals(NestBigInteger.of(9), __$LinkedListEntry.parse(wrap(TEST_DATA), Context.DEFAULT, NestBigInteger.ZERO).value);
    }

    @Test
    void testWorkingIterator() throws IOException {
        NestBigInteger result = __$LinkedListEntry.parse(wrap(TEST_DATA), Context.DEFAULT, NestBigInteger.ZERO)
                .accept(new BottomUpTokenVisitor<>(
                        new TokenVisitor<NestBigInteger>() {
                            @Override
                            public NestBigInteger visitUserDefinedToken(UserDefinedToken value) {
                                return NestBigInteger.ZERO;
                            }

                            @Override
                            public NestBigInteger visitOptionalToken(OptionalToken<? extends Token> value) {
                                return NestBigInteger.ZERO;
                            }

                            @Override
                            public NestBigInteger visitTerminatedToken(
                                    TerminatedToken<? extends Token, ? extends Token> value) {
                                return NestBigInteger.ZERO;
                            }

                            @Override
                            public NestBigInteger visitTokenList(TokenList<? extends Token> value) {
                                return NestBigInteger.ZERO;
                            }

                            @Override
                            public NestBigInteger visitUnsignedBytes(UnsignedBytes value) {
                                return value.size();
                            }
                        }, NestBigInteger::add));
        assertEquals(result, NestBigInteger.of(18));
    }

    @Test
    void testParseMultipleLists() throws IOException {
        TokenList<__$LinkedListEntry> tokensFound = TokenList.untilParseFailure(wrap(TEST_DATA), Context.DEFAULT,
                (s, c) -> {
                    __$LinkedListEntry result = __$LinkedListEntry.parse(s, c, s.getOffset());
                    s.readUnsigned(6, c); // forward pointer by 6 bytes
                    return result;
                });
        assertEquals(4, tokensFound.length()); // 4 valid starts of the chain before we get into invalid data area
        assertEquals(NestBigInteger.of(2 + 3 + 4), tokensFound.get(0).value);
        assertEquals(NestBigInteger.of(4), tokensFound.get(1).value);
        assertEquals(NestBigInteger.of(3 + 4), tokensFound.get(2).value);
        assertEquals(NestBigInteger.of(10 + 0x10101010), tokensFound.get(3).value);
    }
    
    private static final int data_end;
    private static final byte[] TEST_DATA2 = new byte[16*1024*1024];

    static {
        ByteBuffer target = ByteBuffer.wrap(TEST_DATA2);
        target.order(ByteOrder.LITTLE_ENDIAN);
        target.put(new byte[] {'H','D','R'});
        produceFiles().forEach(b -> writeFile(b, target));
        data_end = target.position();
        Arrays.fill(TEST_DATA2, target.position(), TEST_DATA2.length - 1, (byte)0xFF);
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
        __$NestedFiles<__$PNGorJPEG> result = __$NestedFiles.parse(wrap(TEST_DATA2), Context.DEFAULT, __$PNGorJPEG::parse);
        assertEquals(data_end, result.size().intValueExact());
    }
    
	@Test
	void twoByteDoubleParseWithT() {
		__$GenT<UnsignedBytes> parsed = __$GenT.parse(wrap(1, 2), Context.DEFAULT, (s, c) -> s.readUnsigned(1, c));
		assertEquals(1, parsed.field1.getByteAt(NestBigInteger.ZERO));
		assertEquals(2, parsed.field2.getByteAt(NestBigInteger.ZERO));
	}
	
	@Test
	void twoByteDoubleParseWithMain() {
		__$Main main = __$Main.parse(wrap(1, 2), Context.DEFAULT);
		assertEquals(1, main.parsed.field1.getByteAt(NestBigInteger.ZERO));
		assertEquals(2, main.parsed.field2.getByteAt(NestBigInteger.ZERO));
	}
	
	@Test
	void twoByteDoubleParseWithMain2() {
		__$Main2 main = __$Main2.parse(wrap(1, 2), Context.DEFAULT);
		assertEquals(1, main.parsed.field1.a.getByteAt(NestBigInteger.ZERO));
	}
	
	@Test
	void testChoiceAParses() throws URISyntaxException {
		assertEquals(2, __$AorB.parse(wrap(1), Context.DEFAULT).virtualField.intValueExact());
	}

	@Test
	void testChoiceBParses() throws URISyntaxException {
		assertEquals(4, __$AorB.parse(wrap(2), Context.DEFAULT).virtualField.intValueExact());
		assertEquals(2, __$AorB.parse(wrap(2), Context.DEFAULT).x.getByteAt(NestBigInteger.ZERO));
	}

	@Test
	void testChoiceFails() throws URISyntaxException {
		assertThrows(ParseError.class, () -> {
			__$AorB.parse(wrap(3), Context.DEFAULT).virtualField.intValueExact();
		});
	}
	
	@Test
	void testUnbounded() {
		assertEquals(4, __$Rep1.parse(wrap(1,1,1,1), Context.DEFAULT).size().intValueExact());
	}

	@Test
	void testUnboundedStops() {
		assertEquals(4, __$Rep1.parse(wrap(1,1,1,1,2), Context.DEFAULT).size().intValueExact());
	}

	@Test
	void testBoundedStops() {
		assertEquals(3, __$Rep2.parse(wrap(1,1,1,1,2), Context.DEFAULT).size().intValueExact());
	}

	
	@Test
	void testBoundedThrows() {
		assertThrows(ParseError.class, () -> __$Rep2.parse(wrap(1,1,2), Context.DEFAULT));
	}


}
