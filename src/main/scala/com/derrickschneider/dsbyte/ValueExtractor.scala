package com.derrickschneider.dsbyte

import java.beans._

/** Defines an interface for extracting values from a given source.
 *  @author Derrick Schneider, derrick.schneider@gmail.com
 *  @copyright 2013, Derrick Schneider
 */
 
/** Base class defines the behavior */
abstract class ValueExtractor {
    def extractValue(id: Any): Any
}    

/** An implementation of ValueExtractor that can fetch an array element */
class ArrayValueExtractor(val source: Array[Any]) extends ValueExtractor {
    override def extractValue(id: Any): Any = 
        id match {
           case index: Int => source(index)
           case other => throw new IllegalArgumentException("Trying to use something other than an index to access elements in an array of values")
        }
}

/** An implementation of ValueExtractor that can pull values from a Map */
class MapValueExtractor(val source: Map[String,Any]) extends ValueExtractor {
     override def extractValue(id: Any): Any =
         id match {
             case key: String => if (source.get(key).isEmpty) throw new IllegalArgumentException("key " + key + " not in map") else source.get(key).get
             case other => throw new IllegalArgumentException("Trying to use something other than a key to access elements in a map")
         }
}

/** An implementation of BeanExtractor that can pull bean properties */
class BeanValueExtractor(val source: Any) extends ValueExtractor {
    override def extractValue(id: Any): Any =
         id match {
             case prop: String => {
                 val props = Introspector.getBeanInfo(source.getClass).getPropertyDescriptors
                 val propDescriptor = props.find( _.getName == prop)
                 val method = propDescriptor match {
                     case None => throw new IllegalArgumentException(prop + " is not a property of the object")
                     case Some(pd) => pd.getReadMethod
                 }
                 method.invoke(source)
             }
             case prop: Int => Introspector.getBeanInfo(source.getClass).getPropertyDescriptors.apply(prop)
             case other => throw new IllegalArgumentException("Could not find property " + id + " in " + source)
         }
}