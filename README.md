DSByte
===========
A work-in-progress DSL inspired by Erlang's bit-packing syntax for packing and unpacking binary from Java or Scala.

For the moment, you'll have to build this yourself if you want to try it out. It uses <a href="http://www.scala-sbt.org/">sbt</a> as the build tool.

Roadmap:

  * Support unpacking from binary to objects.
  * Helper classes to allow idiomatic Java.
  * Floats/doubles
  * Little endian support.
  * Performance
  * Proper jar packaging

Usage
-----
Packing with scala:
    
    val template = "<<$0>>" // see below for template syntax
    val parser = new DSByteParser()
    parser.parseAll(parser.template,template).get
    val packer = parser.get
    val out = new ByteArrayOutputStream()
    packer.packValues(bos,...)
 
Note that parsers are not currently cached by the system, so parseAll is fairly expensive.

Syntax
-------
The general syntax is:

    <<item[:size]/[type],...>>

Where item is one of:

    '<literal value> or $<variable>
    
And type is one of:
  
  * integer
  * long
  * boolean
  * string
  * bytes
  * list
    
    Note that "list" is followed by a new template in parentheses that defines how to write out each item in the list.
    
If type is not specified, the system will do its best to infer it.
    
If size is not specified, the natural size of the element will be used.

Template Examples
-----------------

Writes out the decimal number 123 across four bytes (because it's an integer by default).

    <<'123>> 

Writes out the decimal number 123 truncated to two bytes.

    <<'123:2>>    

Writes out the hexadecimal number 0xdeadbeef across four bytes.

    <<'0xdeadbeef>>

Writes out the string 'hello' (across five bytes, the length of the string).

    <<'\"hello\">>
    
Writes out the first two bytes ('h' and 'e') of the string "hello".

    <<'\"hello\":2>>"    

Writes out the first two items of an array or a list passed to the packer, inferring the types and sizes, followed by the literal decimal number 123. Note that the final output will vary in size, because $0 and $1 could be anything.

    <<$0, $1, '123>>
    
Writes out the 0th item of an array or list, which is assumed to be a list of items itself. For each item in that list, write it out by inferring the type and size. Note that within the nested template, $0 refers to the 0th element of the current item in the list, not the 0th item of the enclosing object. In other words, () defines a new scope.:

    <<$0(<<$0>>)>>
    
Writes out the key1 and key2 items of the passed-in object. If the passed-in object is a Map, these will be interpreted as keys within that Map. If it's another type of object, these will be interpreted as JavaBean properties.

    <<$key1,$key2>>
    
Writes out the first element, converting it to a long. If it's an int, it will be expanded to the appropriate size (eight bytes versus four).

    <<$0/long>>