package com.derrickschneider.dsbyte

import scala.util.parsing.combinator._
import org.scalatest.Suite

class DSByteParserSuite extends Suite {

   def testTwoGenericFields() {
       assert(doesPass("<<$0,$1>>"))
   }
   
   def testOneFieldWithSizeAndType {
       assert(doesPass("<<$0:1/boolean>>"))
   }
   
   def testOneListFieldWithTwoGenericElements {
       assert(doesPass("<<$0/list(<<$0,$1>>)>>"))  
   }
   
   def testLiteralHexField {
       assert(doesPass("<<'0xdeadbeef>>"))
   }
   
   def testLiteralString {
       assert(doesPass("<<'\"hello\">>"))
   }
   
   def testParsingOfIncorrectTemplates() {
       assert(!(doesPass("<< $0")))
   }
   
   def testResultHasCorrectFields() {
       val parseResult = parserResult("<<$0,$1,'123>>")
       val fields = parseResult.get.fieldDefs
       
       // this will test fields length by default
       val first :: second :: third :: Nil = fields
              
       // this will test proper construction
       assert(first.isInstanceOf[FieldDef])
       assert(second.isInstanceOf[FieldDef])
       assert(third.isInstanceOf[LiteralFieldDef])
   }
   
   // todo: do lists get handled properly (including exceptions)
   
   private def doesPass(templateString: String): Boolean = {
       val result = parserResult(templateString)
       result.successful
   }
   
   private def parserResult(template: String) = {
       val parser = new DSByteParser()
       parser.parseAll(parser.template,template)
   }
}