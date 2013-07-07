package com.derrickschneider.dsbyte

import java.io._
import org.scalatest.Suite

class PackerUnpackerSuite extends Suite {

    def testLiteralDecimalIntNormal() {       
       val bytes = getBytes("<<'123>>")
       assert(bytes(0) === 0x00)
       assert(bytes(1) === 0x00)
       assert(bytes(2) === 0x00)
       assert(bytes(3) === 123)
    }
    
    def testLiteralDecimalIntAbridged() {
        val bytes = getBytes("<<'123:2>>")
    
        assert(bytes(0) === 0x00)
        assert(bytes(1) === 123)
    }
    
    def testLiteralHexIntNormal() {
        val bytes = getBytes("<<'0xdeadbeef>>")
        
        // convert to byte here to make sure signing is correct (as an int it's a positive number, as a byte it's negative)
        assert(bytes(0) === 0xde.toByte)
        assert(bytes(1) === 0xad.toByte)
        assert(bytes(2) === 0xbe.toByte)
        assert(bytes(3) === 0xef.toByte)
    }
    
    def testLiteralStringNormal() {
        val bytes = getBytes("<<'\"hello\">>")
        
        assert(bytes(0) === 'h')
        assert(bytes(1) === 'e')
        assert(bytes(2) === 'l')
        assert(bytes(3) === 'l')
        assert(bytes(4) === 'o')
    }
    
    def testLiteralStringAbridged()  {
        val bytes = getBytes("<<'\"hello\":2>>")
    
        assert(bytes(0) === 'h')
        assert(bytes(1) === 'e')
        
        assert(bytes.length === 2)
    }
    
    def testPassedIntegerNormal() {
       val bytes = getBytesFromArgs("<<$0>>",0x69)
       assert(bytes(3) === 0x69.toByte)
    }
    
    def testPassedIntegerAbridged() {
        val bytes = getBytesFromArgs("<<$0:1>>",0xdead)
        assert(bytes(0) === 0xad.toByte)
    }
    
    def testPassedStringNormal() {
       val bytes = getBytesFromArgs("<<$0>>","testing")
       assert(bytes(0) === 't')
       assert(bytes(1) === 'e')
       assert(bytes(2) === 's')
       assert(bytes(3) === 't')
       assert(bytes(4) === 'i')
       assert(bytes(5) === 'n')
       assert(bytes(6) === 'g')
    }
    
    def testPassedStringAbridged() {
        val bytes = getBytesFromArgs("<<$0:3>>","testing")
        assert(bytes(0) === 't')
        assert(bytes(1) === 'e')
        assert(bytes(2) === 's')       
    }
    
    def testPassedLongNormal() {
       val bytes = getBytesFromArgs("<<$0>>",0xdeadbeefdeadbeefL)
       assert(bytes.length === 8)
       assert(bytes(0) === 0xde.toByte)
    }
    
    def testPassedLongAbridged() {
       val bytes = getBytesFromArgs("<<$0:3>>",0xdeadbeefdeadbeefL)
       assert(bytes.length === 3)
       assert(bytes(2) === 0xef.toByte)
    }
    
    def testPassedListOfSimpleArgs() {
       val bytes = getBytesFromArgs("<<$0(<<$0>>)>>",List(1,2,3))
       assert(bytes.length === 12)
       assert(bytes(3) === 1.toByte)
       assert(bytes(7) === 2.toByte)
       assert(bytes(11) === 3.toByte)
    }
    
    def testPassedIntAndStringFromMap() {
       val map = Map("key1"->0xcafe,"key2"->"hello")

       val pu = packerUnpacker("<<$key1,$key2>>")
       val bos = new ByteArrayOutputStream()
       pu.packValues(bos,map)
       
       val bytes = bos.toByteArray
       assert(bytes.length === 9)
       assert(bytes(2) === 0xca.toByte)
       assert(bytes(8) === 'o'.toByte)
    }
    
    def testIntConvertedToLongOnWrite() {
       val bytes = getBytesFromArgs("<<$0/long>>",1024)
       assert(bytes.length === 8)
       assert(bytes(6) === 0x04)
    }
    
    def testWritingBean() {
       val bean = new TestBean("testing",123)
       val pu = packerUnpacker("<<$name,$count>>")
       val bos = new ByteArrayOutputStream
       pu.packValuesFromBean(bos,bean)
       
       val bytes = bos.toByteArray
       assert(bytes.length === 11)
       assert(bytes(0) === 't'.toByte)
       assert(bytes(10) == 123.toByte)
    }
    
    // cache right-shift amount
    val rightShiftAmount = Array(BITS_IN_BYTE * 0,BITS_IN_BYTE * 1,BITS_IN_BYTE * 2, BITS_IN_BYTE * 3)
    val masks = Array(0x000000ff,0x0000ff00,0x00ff0000,0xff0000)    
    val BITS_IN_BYTE = 8

    /** Tests timing on a variety of different ways to calculate info. Note that there's no assertion as part of this
        test; it's just informational. Which is why it's often left with a "notRun" name
    */
    def notRunTestIntWritingPerformance() {
//     def testIntWritingPerformance() {
        val intToWrite = 0xdeadbeef
        val iterations = 1000
        val bos = new ByteArrayOutputStream(iterations * PackerUnpacker.INT_SIZE) // size bos to make sure it doesn't spend time copying
        val size = PackerUnpacker.INT_SIZE

        var now = System.currentTimeMillis
        
        // naive version: does it all inline
        for (i <- (1 to iterations)) {
           val bytes: Array[Byte] = (size to 1 by -1).map( (x:Int) => ((intToWrite & (0xFF << (BITS_IN_BYTE * (x - 1)))) >>> (BITS_IN_BYTE * (x - 1))).toByte).toArray
            bos.write(bytes)
        }
        println("naive int writing timing: " + ((System.currentTimeMillis - now).toFloat / iterations.toFloat))
        
        
        // cache mask bit shifting results
        bos.reset
        now = System.currentTimeMillis
        
        for (i <- (1 to iterations)) {
           val bytes: Array[Byte] = (size to 1 by -1).map( (x:Int) => ((intToWrite & masks(x - 1)) >>> (BITS_IN_BYTE * (x - 1))).toByte).toArray
            bos.write(bytes)
        }
        println("cached mask int writing timing: " + (System.currentTimeMillis - now).toFloat/iterations.toFloat)
        
        bos.reset
        now = System.currentTimeMillis
        
        for (i <- (1 to iterations)) {
           val bytes: Array[Byte] = (size to 1 by -1).map( (x:Int) => ((intToWrite & masks(x - 1)) >>> rightShiftAmount(x - 1)).toByte).toArray
            bos.write(bytes)
        }
        println("cached >>> int writing timing: " + (System.currentTimeMillis - now).toFloat/iterations.toFloat)
        
        
        bos.reset
        now = System.currentTimeMillis
        
        for (i <- (1 to iterations)) {
           (size to 1 by -1).foreach( (x:Int) => bos.write(((intToWrite & masks(x - 1)) >>> rightShiftAmount(x - 1)).toByte))
        }
        println("one-byte at a time array int writing timing: " + (System.currentTimeMillis - now).toFloat/iterations.toFloat)
        
        bos.reset
        now = System.currentTimeMillis
        
        for (i <- (1 to iterations)) {
           (size to 1 by -1).foreach( (x:Int) => bos.write(getNthByte(x - 1,intToWrite).toByte))
        }
        println("separate function int writing timing: " + (System.currentTimeMillis - now).toFloat/iterations.toFloat)
        
    }
    
    private def getNthByte(n: Int, value: Int): Byte = {
       ((value & masks(n)) >>> rightShiftAmount(n)).toByte
    }
        
    private def getBytes(template: String) = {
        val pu = packerUnpacker(template)
        val bos = new ByteArrayOutputStream()
        pu.packValues(bos)
    
        bos.toByteArray
    }
    
    private def getBytesFromArgs(template: String, args: Any*) = {
       val pu = packerUnpacker(template)
       val bos = new ByteArrayOutputStream()
       pu.packValues(bos,args: _*)
       
       bos.toByteArray
    }
    
    private def packerUnpacker(template: String): PackerUnpacker = {
       val parser = new DSByteParser()
       parser.parseAll(parser.template,template).get
    }
}

class TestBean(val name: String, val count: Int) {
    // for bean compatibility
    def getName() = name
    def getCount() = count
}