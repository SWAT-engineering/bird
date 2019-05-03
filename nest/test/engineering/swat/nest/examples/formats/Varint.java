package engineering.swat.nest.examples.formats;

import engineering.swat.nest.core.ParseError;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.TokenList;
import engineering.swat.nest.core.tokens.UnsignedByte;
import engineering.swat.nest.core.tokens.UnsignedBytes;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.operations.Choice.Case;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicReference;

public class Varint {
    // based on varint.bird

    public static class LEB128 extends UserDefinedToken {
        public final TokenList<UnsignedByte> raw;
        public final NestBigInteger value;

        private LEB128(TokenList<UnsignedByte> raw, NestBigInteger value) {
            this.raw = raw;
            this.value = value;
        }

        public static LEB128 parse(ByteStream source, Context ctx) {
            TokenList<UnsignedByte> raw = TokenList.parseWhile(source, ctx,
                    (s, c) -> s.readUnsigned(c),
                    it -> (it.asValue().and(NestValue.of(0b1000_0000, 1)).sameBytes(NestValue.of(0, 1)))
            );
            NestValue ac = NestValue.of(0, 16);
            // reverse slice
            for (int index = raw.length() - 1; index >= 0; index--) {
                ac = (ac.shl(NestBigInteger.of(7))
                        .or(raw.get(index).asValue().and(NestValue.of(0b0111_1111, 1))));
            }
            NestBigInteger value = ac.asInteger(Sign.UNSIGNED);
            return new LEB128(raw, value);
        }

        @Override
        public TrackedByteSlice getTrackedBytes() {
            return raw.getTrackedBytes();
        }

        @Override
        public NestBigInteger size() {
            return raw.size();
        }
    }

    public static class PrefixVarint extends UserDefinedToken {
        private final Token entry;
        public final NestBigInteger value;

        private PrefixVarint(Token entry, NestBigInteger value) {
            this.entry = entry;
            this.value = value;
        }

        public static PrefixVarint parse(ByteStream source, Context ctx) {
            AtomicReference<NestBigInteger> value = new AtomicReference<>();
            Token entry = Choice.between(source, ctx,
                    Case.of(PrefixVarint$1::parse, c -> value.set(c.value)),
                    Case.of(PrefixVarint$2::parse, c -> value.set(c.value)),
                    Case.of(PrefixVarint$3::parse, c -> value.set(c.value))
            );
            return new PrefixVarint(entry, value.get());
        }

        private static class PrefixVarint$1 extends UserDefinedToken {
            public final UnsignedByte prefixHeader;
            public final NestBigInteger value;

            private PrefixVarint$1(UnsignedByte prefixHeader, NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.value = value;
            }

            public static PrefixVarint$1 parse(ByteStream source, Context ctx) {
                UnsignedByte prefixHeader = source.readUnsigned( ctx);
                if (!prefixHeader.asValue().and(NestValue.of(0b1, 1)).sameBytes(NestValue.of(0b1, 1))) {
                   throw new ParseError("PrefixVarint$1.prefixHeader", prefixHeader);
                }
                NestBigInteger value = prefixHeader.asValue().shr(NestValue.of(1, 1)).asInteger(Sign.UNSIGNED);
                return new PrefixVarint$1(prefixHeader, value);
            }

            @Override
            public TrackedByteSlice getTrackedBytes() {
                return prefixHeader.getTrackedBytes();
            }

            @Override
            public NestBigInteger size() {
                return prefixHeader.size();
            }
        }

        private static class PrefixVarint$2 extends UserDefinedToken {
            public final UnsignedByte prefixHeader;
            public final NestBigInteger prefixLength;
            public final UnsignedBytes rest;
            public final NestValue restValue;
            public final NestBigInteger value;

            private PrefixVarint$2(UnsignedByte prefixHeader,
                    NestBigInteger prefixLength, UnsignedBytes rest,
                    NestValue restValue, NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.prefixLength = prefixLength;
                this.rest = rest;
                this.restValue = restValue;
                this.value = value;
            }


            public static PrefixVarint$2 parse(ByteStream source, Context ctx) {
                UnsignedByte prefixHeader = source.readUnsigned(ctx);
                if (!(prefixHeader.asValue().sameBytes(NestValue.of(0x00, 1)))) {
                    throw new ParseError("PrefixVarint$2.prefixHeader", prefixHeader);
                }
                NestBigInteger prefixLength = PrefixVarintUserDefined.trailingZeroes(prefixHeader);
                UnsignedBytes rest = source.readUnsigned(prefixLength, ctx);
                NestValue ac = NestValue.of(0, 16);
                for (UnsignedByte r : rest) {
                    ac = ac.shl(NestValue.of(8, 1)).or(r.asValue());
                }
                NestValue restValue = ac;
                NestBigInteger value = prefixHeader.asValue().shr(prefixLength.add(NestBigInteger.TWO)).
                        or(restValue.shl(NestBigInteger.of(8).subtract(prefixLength)))
                        .asInteger(Sign.UNSIGNED);
                return new PrefixVarint$2(prefixHeader, prefixLength, rest, restValue, value);
            }

            @Override
            public TrackedByteSlice getTrackedBytes() {
                return buildTrackedView(prefixHeader, rest) ;
            }

            @Override
            public NestBigInteger size() {
                return prefixHeader.size().add(rest.size());
            }
        }

        private static class PrefixVarint$3 extends UserDefinedToken {
            public final UnsignedByte prefixHeader;
            public final UnsignedBytes fullValue;
            public final NestBigInteger value;

            private PrefixVarint$3(UnsignedByte prefixHeader, UnsignedBytes fullValue, NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.fullValue = fullValue;
                this.value = value;
            }

            public static PrefixVarint$3 parse(ByteStream source, Context ctx) {
                UnsignedByte prefixHeader = source.readUnsigned( ctx);
                if (!prefixHeader.asValue().sameBytes(NestValue.of(0x00, 1))) {
                    throw new ParseError("PrefixVarint$3.prefixHeader", prefixHeader);
                }
                UnsignedBytes fullValue = source.readUnsigned(8, ctx);
                NestBigInteger value = fullValue.asValue().asInteger(Sign.UNSIGNED);
                return new PrefixVarint$3(prefixHeader, fullValue, value);
            }

            @Override
            public TrackedByteSlice getTrackedBytes() {
                return buildTrackedView(prefixHeader, fullValue);
            }

            @Override
            public NestBigInteger size() {
                return prefixHeader.size().add(fullValue.size());
            }
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

    private static class PrefixVarintUserDefined {

        public static NestBigInteger trailingZeroes(Token token) {
            byte[] bytes = token.getTrackedBytes().allBytes();
            ByteUtils.reverseBytes(bytes);
            BitSet bits = BitSet.valueOf(bytes);
            if (bits.isEmpty()) {
                return NestBigInteger.of(bytes.length * 8);
            }
            return NestBigInteger.of(bits.nextSetBit(0));
        }
    }
}
