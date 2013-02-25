package svm

import model.{Method, OpCode, ClassFile}

class Class(val classFile: ClassFile,
            classes: String => Class,
            val statics: collection.mutable.Map[String, Any] = collection.mutable.Map.empty){


  def method(name: String, desc: String): Option[Method] = {
    classFile.methods
             .find(m => m.name == name && m.desc == desc)
             .orElse(Option(classFile.superName).flatMap(x => classes(x).method(name, desc)))
  }

  def name = classFile.name

  def isInstanceOf(desc: String, classes: String => Class): Boolean = {
    classFile.name == desc ||
    classFile.interfaces.contains(desc) ||
    (classFile.superName != null && classes(classFile.superName).isInstanceOf(desc, classes))
  }
}

object Object{
  def initMembers(cls: Class, classes: String => Class): List[(String, Any)] = {
    cls.classFile.fields.map{f =>
      f.name -> initField(f.desc)
    } ++ (cls.classFile.superName match{
      case null => Nil
      case x => initMembers(classes(x), classes)
    } )
  }
  def initField(desc: String) = {
    desc(0) match{
      case 'B' => 0: Byte
      case 'C' => 0: Char
      case 'I' => 0
      case 'J' => 0L
      case 'F' => 0F
      case 'D' => 0D
      case 'S' => 0: Short
      case 'Z' => false
      case 'L' => null
      case '[' => null
    }
  }

  def fromVirtual(x: Any): Any = x match{
    case null => null
    case x: Boolean => x
    case x: Byte => x
    case x: Char => x
    case x: Short => x
    case x: Int => x
    case x: Long => x
    case x: Float => x
    case x: Double => x
    case x: Array[Any] =>
      val newArray = new Array[Any](x.length)
      for (i <- 0 until x.length){
        newArray(i) = fromVirtual(x(i))
      }
    case x: svm.Object if x.cls.name == "java/lang/String" =>
      new String(x.members("value").asInstanceOf[Array[Char]])
  }
}
class Object(val cls: Class, classes: String => Class){
  val members = collection.mutable.Map(
    Object.initMembers(cls, classes):_*
  )

  override def toString = {
    s"Object(${cls.name}, ${members}})"
  }
}

object Access{
  val Public    = 0x0001 // 1
  val Private   = 0x0002 // 2
  val Protected = 0x0004 // 4
  val Static    = 0x0008 // 8
  val Final     = 0x0010 // 16
  val Super     = 0x0020 // 32
  val Volatile  = 0x0040 // 64
  val Transient = 0x0080 // 128
  val Native    = 0x0100 // 256
  val Interface = 0x0200 // 512
  val Abstract  = 0x0400 // 1024
  val Strict    = 0x0800 // 2048
}