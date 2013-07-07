package com.derrickschneider.dsbyte

/** This class contains the meta information about the fields that might be passed in, including which passed-in field is relevant
 *  and so forth.
 *  @author Derrick Schneider
 *  @copyright 2013, Derrick Schneider, derrick.schneider@gmail.com
 */
sealed class FieldDef(val size: Int, val fieldId: Any, val fieldType: String) {
   val needsFieldInject = fieldId != null
   val converter = ToTypeConverter.converter(fieldType)
}

case class LiteralFieldDef (override val size: Int, override val fieldType: String, val value: Any) extends FieldDef(size,None,fieldType)

case class ListFieldDef(override val size: Int, override val fieldId: Any, val packerUnpacker: PackerUnpacker) extends FieldDef(size,fieldId,DSByteParser.LIST_FIELD)