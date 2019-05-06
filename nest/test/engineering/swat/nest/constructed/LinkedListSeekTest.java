package engineering.swat.nest.constructed;

import static engineering.swat.nest.CommonTestHelper.FAIL_LOG;
import static engineering.swat.nest.CommonTestHelper.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
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
        assertEquals(NestBigInteger.of(9), LinkedListEntry.parse(wrap(TEST_DATA), Context.DEFAULT.setLogTarget(
                CommonTestHelper.FAIL_LOG), NestBigInteger.ZERO).get().value);
    }

    @Test
    void testParseMultipleLists() throws IOException {
        TokenList<LinkedListEntry> tokensFound = TokenList.untilParseFailure(wrap(TEST_DATA), Context.DEFAULT.setLogTarget(FAIL_LOG),
                (s, c) -> {
                    Optional<LinkedListEntry> result = LinkedListEntry.parse(s, c, s.getOffset());
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

        public static Optional<LinkedListEntry> parse(ByteStream source, Context ctx, NestBigInteger offset) {
            AtomicReference<NestBigInteger> value = new AtomicReference<>();
            Optional<Token> parsed = Choice.between(source, ctx,
                    Case.of((s,c) -> Node.parse(s, c, offset), (l) -> value.set(l.value)),
                    Case.of((s,c) -> Leaf.parse(s, c, offset), (l) -> value.set(l.value))
            );
            if (!parsed.isPresent()) {
                ctx.fail("[LinkedListEntry] failed to parse any choice");
                return Optional.empty();
            }
            return Optional.of(new LinkedListEntry(value.get(), parsed.get()));
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return parsed.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return parsed.size();
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

        public static Optional<Leaf> parse(ByteStream source, Context ctx, NestBigInteger offset) {
            Optional<ByteStream> sourceFork = source.fork(offset);
            if (sourceFork.isPresent()) {
                source = sourceFork.get();
            }
            else {
                ctx.fail("[Leaf] end of stream reached");
                return Optional.empty();
            }
            Optional<UnsignedBytes> next = source.readUnsigned(NestBigInteger.of(2), ctx);
            if (!next.isPresent()) {
                ctx.fail("[Leaf::next] missing");
                return Optional.empty();
            }
            if (!next.get().asValue().sameBytes(NestValue.of(0x0000, 2))) {
                ctx.fail("[Leaf::next] incorrect");
                return Optional.empty();
            }
            Optional<UnsignedBytes> rawValue = source.readUnsigned(NestBigInteger.of(4), ctx);
            if (!rawValue.isPresent()) {
                ctx.fail("[Leaf::rawValue] missing");
                return Optional.empty();
            }
            NestBigInteger value = rawValue.get().asValue().asInteger(Sign.UNSIGNED);
            return Optional.of(new Leaf(value, rawValue.get(), next.get()));
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(next, rawValue);
        }

        @Override
        public NestBigInteger size() {
            return next.size().add(rawValue.size());
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

        public static Optional<Node> parse(ByteStream source, Context ctx, NestBigInteger offset) {
            Optional<ByteStream> sourceFork = source.fork(offset);
            if (sourceFork.isPresent()) {
                source = sourceFork.get();
            }
            else {
                ctx.fail("[Node] end of stream reached");
                return Optional.empty();
            }
            Optional<UnsignedBytes> next = source.readUnsigned(NestBigInteger.of(2), ctx);
            if (!next.isPresent()) {
                ctx.fail("[Node::next] missing");
                return Optional.empty();
            }
            if (next.get().asValue().sameBytes(NestValue.of(0x0000, 2))) {
                ctx.fail("[Node::next] incorrect");
                return Optional.empty();
            }
            Optional<UnsignedBytes> rawValue = source.readUnsigned(NestBigInteger.of(4), ctx);
            if (!rawValue.isPresent()) {
                ctx.fail("[Node::rawValue] missing");
                return Optional.empty();
            }
            Optional<LinkedListEntry> nextEntry = LinkedListEntry.parse(source, ctx, next.get().asValue().asInteger(Sign.UNSIGNED));
            if (!nextEntry.isPresent()) {
                ctx.fail("[Node::nextEntry] missing");
                return Optional.empty();
            }

            NestBigInteger value = nextEntry.get().value.add(rawValue.get().asValue().asInteger(Sign.UNSIGNED));
            return Optional.of(new Node(value, next.get(), rawValue.get(), nextEntry.get()));
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return buildTrackedView(next, rawValue, nextEntry);
        }

        @Override
        public NestBigInteger size() {
            return next.size().add(rawValue.size()).add(nextEntry.size());
        }
    }

}
