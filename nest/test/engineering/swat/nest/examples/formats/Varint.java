package engineering.swat.nest.examples.formats;

import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.ByteUtils;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.Sign;
import engineering.swat.nest.core.bytes.TrackedByteSlice;
import engineering.swat.nest.core.nontokens.NestBigInteger;
import engineering.swat.nest.core.nontokens.NestValue;
import engineering.swat.nest.core.tokens.Token;
import engineering.swat.nest.core.tokens.UserDefinedToken;
import engineering.swat.nest.core.tokens.operations.Choice;
import engineering.swat.nest.core.tokens.primitive.TokenList;
import engineering.swat.nest.core.tokens.primitive.UnsignedBytes;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Varint {
    // based on varint.bird

    public static class LEB128 extends UserDefinedToken {
        public final TokenList<UnsignedBytes> raw;
        public final UnsignedBytes lastOne;
        public final NestBigInteger value;

        private LEB128(TokenList<UnsignedBytes> raw, UnsignedBytes lastOne,
                NestBigInteger value) {
            this.raw = raw;
            this.lastOne = lastOne;
            this.value = value;
        }

        public static Optional<LEB128> parse(ByteStream source, Context ctx) {
            Optional<TokenList<UnsignedBytes>> raw = TokenList.parseWhile(source, ctx,
                    (s, c) -> s.readUnsigned(1, c),
                    it -> !(it.asValue().and(NestValue.of(0b1000_0000, 1)).sameBytes(NestValue.of(0, 1)))
            );
            if (!raw.isPresent()) {
                ctx.fail("LEB128.raw missing from {}", source);
                return Optional.empty();
            }
            Optional<UnsignedBytes> lastOne = source.readUnsigned(1, ctx);
            if (!lastOne.isPresent()) {
                ctx.fail("LEB128.lastOne missing from {}", source);
                return Optional.empty();
            }
            NestValue ac = lastOne.get().asValue();
            // reverse slice
            for (int index = raw.get().length() - 1; index >= 0; index--) {
                ac = (ac.shl(NestBigInteger.of(7))
                        .or(raw.get().get(index).asValue().and(NestValue.of(0b0111_1111, 1))));
            }
            NestBigInteger value = ac.asInteger(Sign.UNSIGNED);
            return Optional.of(new LEB128(raw.get(), lastOne.get(), value));
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

        public static Optional<PrefixVarint> parse(ByteStream source, Context ctx) {
            AtomicReference<NestBigInteger> value = new AtomicReference<>();
            Optional<Token> entry = Choice.between(source, ctx,
                    (s,c) -> {
                        Optional<PrefixVarint$1> result = PrefixVarint$1.parse(s, c);
                        if(result.isPresent()) {
                            value.set(result.get().value);
                        }
                        return result;
                    },
                    (s,c) -> {
                        Optional<PrefixVarint$2> result = PrefixVarint$2.parse(s, c);
                        if(result.isPresent()) {
                            value.set(result.get().value);
                        }
                        return result;
                    },
                    (s,c) -> {
                        Optional<PrefixVarint$3> result = PrefixVarint$3.parse(s, c);
                        if(result.isPresent()) {
                            value.set(result.get().value);
                        }
                        return result;
                    }
            );
            if (!entry.isPresent()) {
                ctx.fail("PrefixVarint missing from {}", source);
                return Optional.empty();
            }
            return Optional.of(new PrefixVarint(entry.get(), value.get()));
        }

        private static class PrefixVarint$1 extends UserDefinedToken {
            public final UnsignedBytes prefixHeader;
            public final NestBigInteger value;

            private PrefixVarint$1(UnsignedBytes prefixHeader, NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.value = value;
            }

            public static Optional<PrefixVarint$1> parse(ByteStream source, Context ctx) {
                Optional<UnsignedBytes> prefixHeader = source.readUnsigned(1, ctx);
                if (!prefixHeader.isPresent() || !prefixHeader.get().asValue().and(NestValue.of(0b1, 1)).sameBytes(NestValue.of(0b1, 1))) {
                    ctx.fail("PrefixVarint$1.prefixHeader {}", prefixHeader);
                    return Optional.empty();
                }
                NestBigInteger value = prefixHeader.get().asValue().shr(NestBigInteger.ONE).asInteger(Sign.UNSIGNED);
                return Optional.of(new PrefixVarint$1(prefixHeader.get(), value));
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
            public final UnsignedBytes prefixHeader;
            public final NestBigInteger prefixLength;
            public final UnsignedBytes rest;
            public final NestBigInteger value;

            private PrefixVarint$2(UnsignedBytes prefixHeader,
                    NestBigInteger prefixLength, UnsignedBytes rest,
                    NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.prefixLength = prefixLength;
                this.rest = rest;
                this.value = value;
            }


            public static Optional<PrefixVarint$2> parse(ByteStream source, Context ctx) {
                Optional<UnsignedBytes> prefixHeader = source.readUnsigned(1, ctx);
                if (!prefixHeader.isPresent() || prefixHeader.get().asValue().sameBytes(NestValue.of(0x00, 1))) {
                    ctx.fail("PrefixVarint$2.prefixHeader {}", prefixHeader);
                    return Optional.empty();
                }
                NestBigInteger prefixLength = PrefixVarintUserDefined.trailingZeroes(prefixHeader.get());
                Optional<UnsignedBytes> rest = source.readUnsigned(prefixLength, ctx.setByteOrder(ByteOrder.LITTLE_ENDIAN));
                if (!rest.isPresent()) {
                    ctx.fail("PrefixVarint$2.rest missing from {}", source);
                    return Optional.empty();
                }
                NestBigInteger value = prefixHeader.get().asValue().shr(prefixLength.add(NestBigInteger.ONE)).
                        or(rest.get().asValue().shl(NestBigInteger.of(8).subtract(prefixLength).subtract(NestBigInteger.ONE)))
                        .asInteger(Sign.UNSIGNED);
                return Optional.of(new PrefixVarint$2(prefixHeader.get(), prefixLength, rest.get(), value));
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
            public final UnsignedBytes prefixHeader;
            public final UnsignedBytes fullValue;
            public final NestBigInteger value;

            private PrefixVarint$3(UnsignedBytes prefixHeader, UnsignedBytes fullValue, NestBigInteger value) {
                this.prefixHeader = prefixHeader;
                this.fullValue = fullValue;
                this.value = value;
            }

            public static Optional<PrefixVarint$3> parse(ByteStream source, Context ctx) {
                Optional<UnsignedBytes> prefixHeader = source.readUnsigned(1, ctx);
                if (!prefixHeader.isPresent() || !prefixHeader.get().asValue().sameBytes(NestValue.of(0x00, 1))) {
                    ctx.fail("PrefixVarint$3.prefixHeader {}", prefixHeader);
                    return Optional.empty();
                }
                Optional<UnsignedBytes> fullValue = source.readUnsigned(8, ctx);
                if (!fullValue.isPresent()) {
                    ctx.fail("PrefixVarint$3.fullValue missing from {}", source);
                    return Optional.empty();
                }
                NestBigInteger value = fullValue.get().asValue().asInteger(Sign.UNSIGNED);
                return Optional.of(new PrefixVarint$3(prefixHeader.get(), fullValue.get(), value));
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
