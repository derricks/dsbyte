package com.derrickschneider.dsbyte

import scala.collection.mutable._

import java.io.OutputStream

/** This class does the actual work of applying functions to write data out of (or pull it from) appropriate streams. It's constructed based on the template
 *  parsed by DSByteParser
 *  @author Derrick Schneider, derrick.schneider@gmail.com
 *  @copyright 2013, Derrick Schneider
 */
 
class PackerUnpacker(val fieldDefs:List[FieldDef]) {
   val BITS_IN_BYTE = 8
   
   /** Pack values from a variable list of arguments */
   def packValues(out: OutputStream,objs: Any*):Unit = 
      writeFieldDefs(fieldDefs,out,new ArrayValueExtractor(objs.toArray))
   
   /** Pack values out of a map */
   def packValues(out: OutputStream, map: scala.collection.immutable.Map[String,Any]) = {
       writeFieldDefs(fieldDefs,out,new MapValueExtractor(map))
   }
   
   /** Packs values from a bean */
   def packValuesFromBean(out: OutputStream, bean: Any) = 
       writeFieldDefs(fieldDefs,out,new BeanValueExtractor(bean))
   
      
   /** Recursive implementation of outputting field defs */
   private def writeFieldDefs(fieldDefs: List[FieldDef],out: OutputStream,values: ValueExtractor): Unit = {
      fieldDefs match {
          case (head: LiteralFieldDef) :: tail => {
                    writeLiteralField(head,out)
                    writeFieldDefs(tail,out,values)
               }
          case (head : ListFieldDef) :: tail => {
                    writeListField(head,out,values)
                    writeFieldDefs(tail,out,values)
               }
          case (head: FieldDef) :: tail => {
                    writePassedField(head,out,values)
                    writeFieldDefs(tail,out,values)
               }
          case Nil =>
      }
   }
   
   //todo: refactor the similarities to writePassedField
   /** Output a list. A list comes with a PackerUnpacker object for the embedded template, which has to be used
       for each element of the list. */
   private def writeListField(fieldDef: ListFieldDef, out: OutputStream, values: ValueExtractor): Unit = {
      val list = values.extractValue(fieldDef.fieldId)
      
      list match {
         case l: List[Any] => l.foreach((item: Any) => fieldDef.packerUnpacker.packValues(out,item))
         case other => throw new IllegalArgumentException("Only Lists are currently supported")
      }
   }
   
   /** Given a passed-in field definition and a list get the field and write it out to the stream */
   private def writePassedField(fieldDef: FieldDef, out: OutputStream, values: ValueExtractor): Unit = {
      val value = values.extractValue(fieldDef.fieldId)
      writeValue(fieldDef,value,out)
   }
   

    private def writeValue(fieldDef: FieldDef,value: Any, out: OutputStream): Unit = {
          // use the fieldDef's converter to ensure the type has been converted to the type
          // the user specified, regardless of the native type of value
          fieldDef.converter.convert(value) match {
             case lvalue: Long => writeLong(longSize(fieldDef,lvalue),lvalue,out)
             case ivalue: Integer => writeInteger(intSize(fieldDef,ivalue),ivalue,out)
             case bvalue: Boolean => writeInteger(if (fieldDef.size == DSByteParser.UNKNOWN_SIZE) PackerUnpacker.BOOL_SIZE else fieldDef.size,if (bvalue) 1 else 0,out)
             case svalue: String => writeString(stringSize(fieldDef,svalue),svalue,out)
             case other => throw new UnsupportedOperationException("type " + other.getClass + " not yet supported")
          }   
    }
    
    /** Determine the size of the string to write, which is either the length of the string or the size explicitly stated
        in the field definition
    */
    private def stringSize(fieldDef: FieldDef,value: String): Int = 
       fieldDef.size match {
           case DSByteParser.UNKNOWN_SIZE => value.length
           case other => other
       }
    
    /** Determine the size to use for the int, which is either the default or the one specified by the field def */
    private def intSize(fieldDef: FieldDef, value: Int) : Int = 
      fieldDef.size match {
          case DSByteParser.UNKNOWN_SIZE => PackerUnpacker.INT_SIZE
          case other => other
      }
      
   /** Determine the size to use for a long, which is either the default or the one specified by the field def */
   private def longSize(fieldDef: FieldDef, value: Long) : Int = 
       fieldDef.size match {
          case DSByteParser.UNKNOWN_SIZE => PackerUnpacker.LONG_SIZE
          case other => other
       }
   
   /** Write out a literal field definition: figure out the value and write out to the stream as appropriate
     */
   private def writeLiteralField(fieldDef: LiteralFieldDef, out:OutputStream): Unit =
       writeValue(fieldDef,fieldDef.value,out)
   
   private def writeString(size: Int, value: String, out: OutputStream): Unit = {
      out.write(value.getBytes,0,size)
   }
   
   // cached results of standard calculations
   private val rightShiftAmount = (0 to 7).map(BITS_IN_BYTE * _).toArray // covers longs and ints
   private val be_int_masks = Array(0x000000ff,0x0000ff00,0x00ff0000,0xff000000)
   private val be_long_masks =  Array(0x00000000000000ffL,0x000000000000ff00L,0x0000000000ff0000L,0x00000000ff000000L,
                                      0x000000ff00000000L,0x0000ff0000000000L,0x00ff000000000000L,0xff00000000000000L)
   
   private def writeInteger(size: Int, value: Int, out: OutputStream): Unit = {
      // the logic: from 1 to n, we want space size - n in the final array to be the full value masked with 0xFF to the power of n
      //            with appropriate conversions to bytes and ints as needed
      // so for instance
      // 0xDEADBEEF ->
      // 0xDEADBEEF & 0xFF000000 -> 0xde in slot 0 (0xFF shifted left 3)
      // 0xDEADBEEF & 0x00FF0000 -> 0xad in slot 1 (0xFF to the power of 3)
      // 0xDEADBEEF & 0x0000FF00 in slot 2
      // 0xDEADBEEF & 0x000000FF in slot 3
      // this will give you a bigendian int (the highest byte to the left)
      
      (size - 1 to 0 by -1).foreach( (x:Int) => out.write(((value & be_int_masks(x)) >>> rightShiftAmount(x)).toByte))
   }
   
   /** Same as writing an int but uses appropriate masking constants */
   private def writeLong(size: Int, value: Long, out: OutputStream): Unit = {
       (size - 1 to 0 by -1).foreach( (x:Int) => out.write(((value & be_long_masks(x)) >>> rightShiftAmount(x)).toByte))
   }
   
}

object PackerUnpacker {
   val INT_SIZE = 4
   val LONG_SIZE = 8
   val BOOL_SIZE = 1
   
   // utility methods
   def inferSize(item: Any) = item match {
       case _item: Int => INT_SIZE
       case _item: Long => LONG_SIZE
       case _item: Boolean => BOOL_SIZE
       case _ => 0
   }
   
   def inferType(item:Int) =  DSByteParser.INTEGER_FIELD
   def inferType(item:Long) =  DSByteParser.LONG_FIELD
   def inferType(item:Boolean) =  DSByteParser.BOOLEAN_FIELD
   def inferType(item:Array[Byte]) =  DSByteParser.BYTES_FIElD
   def inferType(item:String) =  DSByteParser.STRING_FIELD
   def inferType(item:List[Any]) =  DSByteParser.LIST_FIELD
   def inferType(item:Any) = DSByteParser.UNKNOWN_FIELD
   
}