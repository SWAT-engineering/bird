package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.BottomUpTokenVisitor;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenVisitor;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.OptionalToken;
import engineering.swat.nest.core.tokens.primitive.TerminatedToken;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class LinkedListSeekTest {

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
        assertEquals(NestBigInteger.of(9), LinkedListEntry.parse(wrap(TEST_DATA), Context.DEFAULT, NestBigInteger.ZERO).value);
    }

    @Test
    void testWorkingIterator() throws IOException {
        NestBigInteger result = LinkedListEntry.parse(wrap(TEST_DATA), Context.DEFAULT, NestBigInteger.ZERO)
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
        TokenList<LinkedListEntry> tokensFound = TokenList.untilParseFailure(wrap(TEST_DATA), Context.DEFAULT,
                (s, c) -> {
                    LinkedListEntry result = LinkedListEntry.parse(s, c, s.getOffset());
                    s.readUnsigned(6, c); // forward pointer by 6 bytes
                    return result;
                });
        assertEquals(4, tokensFound.length()); // 4 valid starts of the chain before we get into invalid data area
        assertEquals(NestBigInteger.of(2 + 3 + 4), tokensFound.get(0).value);
        assertEquals(NestBigInteger.of(4), tokensFound.get(1).value);
        assertEquals(NestBigInteger.of(3 + 4), tokensFound.get(2).value);
        assertEquals(NestBigInteger.of(10 + 0x10101010), tokensFound.get(3).value);
    }

    private static class LinkedListEntry extends UserDefinedToken {

        public final NestBigInteger value;
        private final Token parsed;

        private LinkedListEntry(NestBigInteger value, Token parsed) {
            this.value = value;
            this.parsed = parsed;
        }

        public static LinkedListEntry parse(ByteStream source, Context ctx, NestBigInteger offset) {
            AtomicReference<NestBigInteger> value = new AtomicReference<>();
            Token parsed = Choice.between(source, ctx,
                    (s, c) -> {
                        Node result = Node.parse(s, c, offset);
                        value.set(result.value);
                        return result;
                    },
                    (s, c) -> {
                        Leaf result = Leaf.parse(s, c, offset);
                        value.set(result.value);
                        return result;
                    }
            );
            return new LinkedListEntry(value.get(), parsed);
        }

        @Override
        protected Token[] parsedTokens() {
            return new Token[]{parsed};
        }
    }

    private static class Leaf extends UserDefinedToken {

        public final NestBigInteger value;
        public final UnsignedBytes next;
        public final UnsignedBytes rawValue;

        private Leaf(NestBigInteger value, UnsignedBytes next, UnsignedBytes rawValue) {
            this.value = value;
            this.next = next;
            this.rawValue = rawValue;
        }

        public static Leaf parse(ByteStream source, Context ctx, NestBigInteger offset) {
            source = source.fork(offset);
            UnsignedBytes next = source.readUnsigned(NestBigInteger.of(2), ctx);
            if (!next.asValue().sameBytes(NestValue.of(0x0000, 2))) {
                throw new ParseError("Lead.next", next);
            }
            UnsignedBytes rawValue = source.readUnsigned(NestBigInteger.of(4), ctx);
            NestBigInteger value = rawValue.asValue().asInteger(Sign.UNSIGNED);
            return new Leaf(value, rawValue, next);
        }

        @Override
        protected Token[] parsedTokens() {
            return new Token[]{next, rawValue};
        }
    }

    private static class Node extends UserDefinedToken {

        public final NestBigInteger value;
        public final UnsignedBytes next;
        public final UnsignedBytes rawValue;
        public final LinkedListEntry nextEntry;

        private Node(NestBigInteger value, UnsignedBytes next, UnsignedBytes rawValue, LinkedListEntry nextEntry) {
            this.value = value;
            this.next = next;
            this.rawValue = rawValue;
            this.nextEntry = nextEntry;
        }

        public static Node parse(ByteStream source, Context ctx, NestBigInteger offset) {
            source = source.fork(offset);
            UnsignedBytes next = source.readUnsigned(NestBigInteger.of(2), ctx);
            if (next.asValue().sameBytes(NestValue.of(0x0000, 2))) {
                throw new ParseError("Node.next", next);
            }
            UnsignedBytes rawValue = source.readUnsigned(NestBigInteger.of(4), ctx);
            LinkedListEntry nextEntry = LinkedListEntry.parse(source, ctx, next.asValue().asInteger(Sign.UNSIGNED));

            NestBigInteger value = nextEntry.value.add(rawValue.asValue().asInteger(Sign.UNSIGNED));
            return new Node(value, next, rawValue, nextEntry);
        }

        @Override
        protected Token[] parsedTokens() {
            return new Token[]{next, rawValue, nextEntry};
        }
    }

}
