# Nest design

## Origin

Nest is the infrastructure layer of BIRD, until it can be replaced by a version of Metal that does support the operations we need.

The goal of Nest is to make it easier to work with lists and map to regular java objects, such that the BIRD strucutures are more easily represented.



## Concepts of Nest

Nest has two main concepts:

- Values: values with which we can calculate (they can be derived of a Token or a java literal)
- Tokens: parsed values that have one or more bytes as source, they track their origin

### Values

Bird & Metal's design feature a mix of modes in the expressions they allow. Most operations are on the byte level, while some convert the bytes to an arbitrary precision integer (aka BigInteger).

There are two kinds of values:

- A byte backed value, for bitwise operations and byte comparison. It stores the byte in big-endian format and can perform operations on other byte backed values.
- A BigInteger, these can be used for calculations, but you cannot go back and get the byte backed value again.


### Tokens

Primitive tokens parses one or more bytes. The bytes are parsed in a certain context (Sign, Endianness, and the Encoding) and can be compared to other byte values. Primitive tokens can also consists of zero or more nested tokens, examples are: list, optional, terminated token. 


### Endianness

When interpreting multiple bytes as an single value, the order of the bytes is important to de precise about.
Big-Endian is the most intuitive as the order of the bytes is from left to right, like a byte array. Little-Endian is the reverse order.

The rules around endianness are as follows:
- In Nest (and Metal & Bird) tokens are always big endian bytes (as in the order they appear in the stream), when we compare bytes, they are compared in the same order that they appeared in the stream.
- When we translate a Token to a Value, we take into account the endianness (which is stored at parsing time). 
- Java Literals ignore the context and always use big-endianness (for example a byte array literal) and are translated to a byte backed value

### Sign

When converting a stream of bytes to an integer value, we have to take special care of the most significant bit as for signed values it signals a negative value.
This is only done when converting a Value to a BigInteger, all byte level operations assume unsigned values (else a bit shift right gets very confusing).


## Internals of Nest and their mapping to bird


 - `u8` to `UnsignedBytes` when parsed (I think `s8` should not exist but we need a `int` and `uint` type)
 - `u8` to `NestValue` when the result of a calculation
 - `u16` to `UnsignedBytes` when parsed
 - `u16` to `NestValue` when the result of a calculation
 - `u8[]` to `UnsignedBytes` (this is a special case so that a lot of operations are simpeler and faster)
 - `u16[]`to `TokenList<UnsignedBytes>`
 - `struct X` to `class X extends UserDefinedToken`
 - `X[]` to `TokenList<X>`
 - `y.as[int]` to `y.asValue().toInteger(Sign.SIGNED)`
 - `y.as[uint]` to `y.asValue().toInteger(Sign.UNSIGNED)`
 - `x != 0xEEFF` to `!x.asValue().sameBytes(NestValue.of(0xEEFF, 2))` or `x.sameBytes(NestValue.of(0xEEFF,2))` (second argument is amount of bytes)
 - `x != 0x00FF` to `!x.asValue().sameBytes(NestValue.of(0x00FF, 2))` (second argument is amount of bytes)
 - `x != 0xFF` to `!x.asValue().sameBytes(NestValue.of(0xFF, 1))` (second argument is amount of bytes)
 - `x != [0xEE, 0xFF]` to `!x.asValue().sameBytes(NestValue.of(new int[] {0xEE, 0xFF}))` (we will not use java byte type, as it is a signed byte, so we take the same technique as many others, we use only the last 8bits of an int)

### Complicated expression

```bird
struct X {
    u8[] data[4];
    uint value = (u8 ac = 0 | ((ac << 7) | (b & 0b0111_1111)) | b <- data[-1:0]).as[uint]; 
}
```

to

```java
public class X extends UserDefinedToken {
    public final UnsignedBytes data;
    public final NestBigInteger value;
    
    private X(UnsignedBytes data, NestBigInteger value) {
        this.data = data;
        this.value = value;
    }
    
    public static X parse(ByteStream source, Context ctx) {
        final UnsignedBytes data = source.readUnsigned(NestBigInteger.of(4), ctx);
        NestValue ac = NestValue.of(0, 1);
        for (int c = data.length() - 1; c >= 0; c--) {
            ac = ac.shl(NestBigInteger.of(7))
                .or(dataBytes.get(c).asValue().and(NestValue.of(0b0111_1111, 1)));
        }
        final NestBigInteger value = ac.asInteger(Sign.UNSIGNED);
        return X(data, value);
    }
}
```

The comprehension as to get a start type, it could also be an u8, as it will automatically grow.
 