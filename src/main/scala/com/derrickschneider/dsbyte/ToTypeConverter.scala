package com.derrickschneider.dsbyte

/** Classes that support converting from one type of value to another. This is to handle the case where you, say, specify
  * $0/long but pass an int. The intent is clearly to write the int into a long's space (8 bytes). 
  * PackerUnpacker will, by default, assume that an int gets 4 bytes. So this would convert it into a long so that
  * PackerUnpacker writes it into 8 bytes
  *
  * @author Derrick Schneider, derrick.schneider@gmail.com
  * @copyright 2013, Derrick Schneider
  */
  
class ToTypeConverter {
   // base class just returns the value as is
   def convert(value: Any) = value
}

class ToLongConverter extends ToTypeConverter {
   override def convert(value: Any) =
       value match {
          case l: Long => l
          case i: Int => i.toLong
          case b: Boolean => if (b) 1.toLong else 0.toLong
          case other => throw new IllegalArgumentException("Can not convert type " + other.getClass + " to long ")
       }
}

class ToIntConverter extends ToTypeConverter {
    override def convert(value: Any) =
        value match {
            case i: Int => i
            case l: Long => l.toInt
            case b: Boolean => if (b) 1 else 0
            case other => throw new IllegalArgumentException("Can not convert type " + other.getClass + " to int")
        }
}

class ToStringConverter extends ToTypeConverter {
    override def convert(value: Any) = value.toString
}

class ToBooleanConverter extends ToTypeConverter {
    override def convert(value: Any) = 
        value match {
           case i: Int => i > 0
           case l: Long => l > 0
           case s: String => s.toLowerCase == "true"
           case other => throw new IllegalArgumentException("Can not convert type " + other.getClass + " to boolean")
        }
}

object ToTypeConverter {
   // toTypeConverters can be singletons, so just use constant instances
   private val TO_UNKNOWN = new ToTypeConverter
   private val TO_LONG = new ToLongConverter
   private val TO_INT = new ToIntConverter
   private val TO_BOOLEAN = new ToBooleanConverter
   private val TO_STRING = new ToStringConverter
   
   /** Factory method for retrieving the appropriate converter given a field type */
   def converter(fieldType: String) =
       fieldType match {
           case DSByteParser.UNKNOWN_FIELD => TO_UNKNOWN
           case DSByteParser.INTEGER_FIELD => TO_INT
           case DSByteParser.LONG_FIELD => TO_LONG
           case DSByteParser.BOOLEAN_FIELD => TO_BOOLEAN
           case DSByteParser.STRING_FIELD => TO_STRING
           case other => TO_UNKNOWN
       }
}

