package com.derrickschneider.dsbyte

import scala.util.matching.Regex
import scala.util.parsing.combinator._

/** Parser definition that takes a a dsbyte template and converts it into an object that can be used
  * for packing/unpacking binary according to the template spec
  * @author Derrick Schneider, derrick.schneider@gmail.com
  * @copyright 2013, Derrick Schneider
  */
class DSByteParser extends JavaTokenParsers {
   
   val HEX_REGEX = new Regex("[A-Fa-f0-9]+")
   
   def template: Parser[PackerUnpacker] = "<<"~>repsep(field,",")<~">>" ^^ (x => new PackerUnpacker(x))
   
   def field: Parser[FieldDef] = literalField | variableField // | ignoreField
   
   def literalField: Parser[FieldDef] = "'"~>(number | strippedStringLiteral )~opt(size)~opt(basicFieldType) ^^ {
       case value~None~None => new LiteralFieldDef(PackerUnpacker.inferSize(value),PackerUnpacker.inferType(value),value)
       case value~Some(size)~None => new LiteralFieldDef(size,PackerUnpacker.inferType(value),value)
       case value~None~Some(fieldType) => new LiteralFieldDef(PackerUnpacker.inferSize(value),fieldType,value)
       case value~Some(size)~Some(fieldType) => new LiteralFieldDef(size,fieldType,value)
   }
   
   def variableField: Parser[FieldDef] = ("$"~>identifier~opt(size)~opt(basicFieldType)~opt(listDef)) ^^ {
       // handle list types first so we can ensure no one else uses template argument
       case fieldId~None~Some("list")~Some(pu) => new ListFieldDef(DSByteParser.UNKNOWN_SIZE,fieldId,pu)
       case fieldId~Some(size)~Some("list")~Some(pu) => new ListFieldDef(size,fieldId,pu)
       
       // implicit lists
       case fieldId~None~None~Some(pu) => new ListFieldDef(DSByteParser.UNKNOWN_SIZE,fieldId,pu)
       case fieldId~Some(size)~None~Some(pu) => new ListFieldDef(size,fieldId,pu)
       
       // but anyone who's not a list who declares a template is wrong
       case fieldId~_Size~_Type~Some(pu) => throw new IllegalArgumentException("field is defined as a non-list but specifies a list template")
       
       // we don't care about 
       case fieldId~None~None~None => new FieldDef(DSByteParser.UNKNOWN_SIZE, fieldId, DSByteParser.UNKNOWN_FIELD)
       case fieldId~None~Some(fieldType)~None => new FieldDef(DSByteParser.UNKNOWN_SIZE,fieldId,fieldType)
       case fieldId~Some(size)~None~None => new FieldDef(size,fieldId,DSByteParser.UNKNOWN_FIELD)
       case fieldId~Some(size)~Some(fieldType)~None => new FieldDef(size,fieldId,fieldType)
   } 
   
   def listDef: Parser[PackerUnpacker] = "("~>template<~")"
      
//   def ignoreField: Parser[FieldDef] = "_"~>identifier~opt(size)~opt(basicFieldType)
   
   // use a long in hex conversion because of overflow issues with large hex values (0x0a000000 and above)
   def number: Parser[Any] = 
       (hexNumber ^^ (java.lang.Long.parseLong(_,16).toInt)) | (wholeNumber ^^ (_.toInt)) | (floatingPointNumber ^^ (_.toFloat)) 
       
   def size: Parser[Int] = ":"~>wholeNumber ^^ (_.toInt)
   def basicFieldType: Parser[String] = "/"~>("integer" | "long" | "boolean" | "bytes" | "string" | "list") 
   
   def hexNumber: Parser[String] = "0x"~>regex(HEX_REGEX)
   
   // stringLiteral by itself returns the double quotes. In this case we don't want them
   def strippedStringLiteral: Parser[String] = stringLiteral ^^ (s => s.substring(1,s.length - 1))
   
   def identifier: Parser[Any] = (wholeNumber ^^ (_.toInt)) | ident
   
}

object DSByteParser {
   // constants
   val UNKNOWN_SIZE = 0
   val INTEGER_FIELD = "integer"
   val LONG_FIELD = "long"
   val BOOLEAN_FIELD = "boolean"
   val BYTES_FIElD = "bytes"
   val STRING_FIELD = "string"
   val LIST_FIELD = "list"
   val UNKNOWN_FIELD = "unknown"
}