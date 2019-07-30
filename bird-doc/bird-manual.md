# Bird - A DSL to describe binary data formats


Bird is a DSL to describe binary file formats such as BMP, PNG, JPEG or data protocols such as HTTP, UDP. Such a description of a format is declarative: it describes how the format looks like but does not describe how it can be parsed.

In Bird each token definition has its own type. Users can define new structured types that correspond to parsers, making the process of encoding new binary specifications less error-prone. To execute these specifications, DAN generates calls to the Nest API, a Java library for parsing binary data. 

# Execution model

A program consists of one or more modules. A module, thus, starts with a declaration of its name, followed by the modules it depends on:

```
module foo

import bar1
import bar2

...
```

The body of the module consists of a series of type definitions, that can be interpreted as parsers.  A program starts being executed by parsing a stream of bytes using a token/type definition, such as:

```
struct Name {
	byte[] firstName[20]
	byte[] secondName[20]
}
```
`Name` defines a composed type and also specifies a parser. If we start the execution at `Name`, the result of that execution/parsing will be a data structure containing fields `firstName` and `secondName`. Consider another example of a type definition:

```
struct Info {
	Name name
	byte[] email[30]
}
```

`Info` defines another type, that depends on `Name`. If we start the execution at `Info`, the result of that execution will be a data structure containing fields `name` of the user-defined type `Name`, and field `email` of the primitive type `byte[]`.


# Types

The main metaphor in Bird is that types correspond to parsers. However, for computation purposes, we also need plain types that are not associated to any parsing process. Therefore, we can distinguish non-token types such as `string`, `int`, `bool`, and token types corresponding to a 8-bit word that is actually parsed (`byte`). Both kinds of types can be aggregated in list types, specified as `T[]`, where `T` is a type, e.g.: `int[]`, `string[]`, `byte[]`. Notice that that the latter example is a list that is composed of elements of the token type, and therefore is also considered to be a token type. As a syntactic convenience, the user can also specify the size of the word to be read at parsing time. For instance, to parse a 16-bit word we can write `u16`. The type of such definition corresponds to `byte[]` but the information of how many octets will be parsed is included in the type. Consider this definition:

```
struct WordSizeExample {
	byte[] a[2]
	u16 b
}
```

In this case, both `a` and `b` are equivalent, and define the same parser. 

**Attention:** Note that if one writes `u8`, this is not the same as `byte` even though both types will define a parser that only reads one byte. However, the type of the former is `byte[]` while the type of the latter is just`byte`.



Besides the aforementioned primitive types, users can define their own (token) types via `struct` or `choice` declarations.

In the following section, we review in detail the available primitive types and how to define new user-defined types.

## Conditions in primitive token types


<!--A simple program:

```
def OneByte: u8
```

It defines a type `OneByte` that works as an alias of `u8`. It also defines a parser that will exactly parse 1 byte. Therefore, we can expect `u8` and `OneByte` as interchangeable.

Most DAN definitions are two-pronged: they define types and  parsers. Consider:

```
u8[2] TwoBytes
```

-->

Primitive field definitions can also include conditions 
```
byte a ?(this != 65)
```

It defines field `a` of type `byte`. The value will only be assigned if at execution time, the parsed byte is not equal to 65, or, from another perspective, the parsed character is different from `A` (the character corresponding to 65). Notice that the special variable `this` in the condition refers to the field currently being defined. 

## Struct types

We know than we can define structured data that enrich the set of available types with user-defined ones:

```
struct Simple {
	u8 first
	u16 second
}
```

This definition introduces the user-defined type `Simple`, that corresponds to a composed type that contains fields first and second. The double notion of type/parsing is more evident in the case of structs, in the sense that the fields' order matters. When used as a specification for parsing, `Simple` will first parse a byte that will be assigned to field `first`, and then two consecutive bytes that will be assigned to `second`. Both fields have the type `byte[]`.

## Primitive non-token types

Not every type is parseable. Non-token types allow us to represent common primitive values that are not attached to a particular byte representation, such as strings, integers or booleans. As they do not define parsers, fields of a non-token type need to be initialized at the moment of declaration, for example:

```
struct Simple{
	int myOne = 1
}
```

A non-token type can be used to compute derived attributes inside an user-defined token type:

```
struct DerivedExample{
	byte[] token1[3]
	int offset1 = token1.offset
	byte[] token2
	int length2 =  token2.length
}
```

**Important:**  all list types have an implicit `length` field, and all token types, an implicit `offset` field.

<!--A derived attribute can also be of a token type, working as an alias. The key distinctive syntactic feature in these cases is, again, the use of `=`:

```
def Inner: struct{
	n: u8
}

def DerivedExamle: struct{
	inner: Inner
	
	number: u8 = inner.n
}
```
-->

For readability purposes, it is sometimes preferable to separate the token fields from the derived ones, as the token fields are the ones that "define" the parser. To do so, a dependency graph is calculated and therefore programmers can arrange the derived fields in any order. Coming back to the previous example, the following struct definitions are two valid alternative encodings:


```
struct DerivedExample2{
	byte[] token1[3]
	byte[] token2
	
	int offset1 = token1.offset
	int length2 = token2.length
}

struct DerivedExample3{
	int offset1 = token1.offset
	int length2 = token2.length
		
	byte[] token1[3]
	byte[] token2
}
```


## Choice types

We can also define choice types:

```
struct A{
	u8 content ? (this == "A")
}

struct B{
	u8 content ? (this == "B")
}

choice AB{
	A
	B
}
```

Choice types imply backtracking. In this case, an attempt to parse the input with type `A` will be made first. If this parsing does not succeed, then `B` will be tried, in strict declaration order. In other words, the disambiguation is driven by the parsing process. Note that the fields in a choice correspond to alternatives, therefore, they are not named.

Notice too that despite both alternatives featuring a field `content` of type `u8`, this field is not accessible if we have a reference to a value of type `AB`. In order to do this, we need to use _abstract fields_. An abstract field is a field declared inside a choice, whose implementation is abstract and must be defined in *each one of the alternatives*. Let us redefine `AB` using this concept:

```
choice AB2{
	abstract byte[] content
	A
	B
}
```

Since both alternatives have a field called content of type `byte[]`, this definition is valid. Thus, `AB2` exposes a field content that will hold the proper value, depending on which alternative is going to be taken. Notice that at the abvstract level one has to use the generic definition of list-based types `byte[]` instead of using the syntactic convenience of `u*`. This is because the only thing we know at the abstract level is that the abstract field will contain a list of bytes. The particularities of how this list is produced is onbly explicit in the tokens that "implement" the `content` field.

<!--```
Choice types might also have derived fields. In that case, we need to declare them in the outer scope, and assign them in each variant:


def DerivedChoice: choice {
	size: int
	variant1: struct{
		a: u8? (this == 65)
		rest: u8[2]
		size = 3
	}
	variant2: struct{
		a: u8?
		rest: u8[3]
		size = 4
	}
}
```-->


## Literals and conversions

In the previous example, notice the equality check performed in the `content` declarations . We are comparing `this` (which refers to the field `content` of type `byte[]` with a string literal ("A" and "B"), of type `string`. This comparison works because the literals "A" and "B" are of type `byte[]`. They are byte liyerals. Ther encoding used to intepret these characters as bytes is by default UTF-8, but this convention can be altered using "meta properties", explained below in this document. 

In contrast, if we would like to express the string literal corresponding to "A", the notation in Bird is `"A"S`.


The following table list the different types of literals available in Bird:

| Name | Type | Example| Notes
| --- | ---- | ----| --- |
| String Byte Literal | `byte[]` | "AB" | Encoding can be set using meta-property |
| Natural Number Byte Literal | `byte[]` | 42B | |
| Hexadecimal Number Byte Literal | `byte[]` | 0x2A | |
| Binary Number Byte Literal | `byte[]` | 0b0010_1010 | |
| String Byte Literal | `byte[]` | "AB" | Encoding can be set using meta-property |
| Integer Literal | `int` | -42 | |
| Hexadecimal Number Literal | `int` | 0x2AI | |
| Binary Number Literal | `int` | 0b0010_1010I | |
| Boolean Literal	| `bool` | false | |

Many times, bytes obtained from parsing need to be interpreted as primitive non-type values. For that, we use explicit conversions, as in this example:

```
struct Conversions {
	u8 size
	byte[] data[size.as[int]]
}
```

The size of an array declaration needs to be an integer so in this case we need to interpret the read size as an integer by using the conversion operator `.as[]`.



## Constructors of user-defined types

We can modularize programs by having parametric user-defined types:

```
struct MustBeOfCertainAge(int ageLimit) {
	byte[] name[20]
	u8 age ? (this.as[int] == ageLimit)
}
```

In this case, `MustBeOfCertainAge` acts as a constructor receiving one integer argument. 

In order to build parsers conforming to this definition we need to parameterize the construction of values of this type. Consider, for instance, this field declaration:

```
MustBeOfCertainAge person(18)
```

This field definition instantiate the `MustBeOfCertainAge` struct definition for the concrete argument `18`.
 
## Anonymous fields

Sometimes, a field is needed just for parsing purposes but it does not really need to have a name assigned. In that case, we can use the field name `_`, which means to ignore that field when accessing the fields of a value of such type:

```
struct Ignoring {
	   byte[] name[20]
	   u8 _ ? (this == ' ') // there must be a space after the name, but we do not name it
	   u16 age
	   byte[] _[4] ? (this == '     ') // there must be four spaces after the age, but we do not name it
   }
```

## Inline user-defined types

In the choice example, for defining types `AB` and `AB2` we also needed to define types `A` and `B`, exclusively in order to define the alternatives of the choice. In these situations, it might be more convenient to inline the struct definitions, as shown here:


```
choice AB3 {
	abstract byte[] content
	struct{
		u8 content ? (u8 == "A")
	}
	struct{
		u8 content ? (u8 == "B")
	}
}
```

The same applies for a struct definition, for example:

```
struct Info2 {
	struct{
		byte[] firstName[20]
		byte[] secondName[20]
	} name
	byte[] email[30]
}
```

In order to access the `firstName` field of the inner struct, assuming that we have a variable `i` of type `Info2`, we need to write `anInfo2.name.firstName`.

# Parsing

Sometimes it is necessary to write generic code that is independent of one specific format specification. In those cases, we want to have parameterized parsing. For this, we can use the `byparsing` expression, that parses a given list of bytes according to a token type specified as a type parameter. Consider this example:

```
struct Digit{
	u8 a ?(this.as[int] >=0 || this.as[int] <=9)
}

struct TwoXs<X>{
	byte[] first
	byte[] second
	X firstX byparsing (first)
	X secondX byparsing (second)
}

struct Simple{
	TwoXs<Digit> as
	X oneX = as.firstX
}
```

<!--Similarly, when we just want to have a parameteric field in the structure, you can also use it directly. So `TwoXs` could also have been written like:

```
struct TwoXs<X>{
	X firstX
	X secondX
}
```
-->
# Functions

We provide a simple mechanism for reuse through functions. We can declare "function bridges", that is, declare the signature of a function annotated with a Java implementation:

```
@(org.foo.VeryComplexEncoding.apply)
int veryComplexEncoding(int x)

struct Foo2 {
	u8 bar
	int x = veryComplexEncoding(bar)
}
```

The referred Java class in this example needs to have the following structure:

```
package org.foo;

...

public class VeryComplexEncoding {
	...
	public Integer apply(Integer x) {
		...
	}
}
```

# Meta properties

Properties that have to do with how to parse the token types are represented as annotations at the declaration level:

```
struct Block@(endianness=LittleIndian, encoding= ISO-8859-1, signedness = SIGNED) {
	u32 content ?(this == 0xDEADBEEF )
	byte[] content[4] ?(this == 0xCAFEBEEF)
}
```

This means that when parsing content to produce a value of type `Block`, the used encoding will be ISO-8859-1, and the bytes will be interpreted as to be signed and using little endian endianness. The default encoding is `UTF-8`, the default endianness is `Big Endian`, and the default signedness is `unsigned`.

Another meta-property is `offset`, that creates a new parsing pointer (disconnected from the context) and moves that parsing pointer to the specific (absolute) offset. If omitted, the default is to advance the normal pointer of the parent. Here an example where we start parsing from the 10th byte on:

```
struct Root {
    // parsing pointer is at position 0
    u8 oneByte
    // parsing pointer is at position 1
    u32 oneInt
    // parsing pointer is at position 5
    Block2 sim
    // parsing pointer is at position 5
    Block2 sim
    // parsing pointer is at position 5
    u8 oneByte
    // parsing pointer is at position 6
}

struct Block2@(offset=9) {
    // parsing pointer is at position 9
    bytye[] content[4]
    // parsing pointer is at position 13
}
```
