package org.jetbrains.kotlin.native.interop.indexer

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type.getArgumentTypes
import org.jetbrains.org.objectweb.asm.Type.getReturnType
import java.util.jar.JarFile

/**
 * Visits a Java class and builds an ObjCClass
 */
class J2ObjCParser: ClassVisitor(Opcodes.ASM5) {

  var className:String = ""
  val methodDescriptors = mutableListOf<Triple<String, String, Int>>()
  val parameterNames = mutableListOf<List<String>>()

  override fun visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String?,
                     superName: String?,
                     interfaces: Array<out String>?) {
    className = name
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String?,
                           exceptions: Array<out String>?): MethodVisitor? {

    val methodBuilder = MethodBuilder(parameterNames)
    methodDescriptors.add(Triple(name, descriptor, access))
    return methodBuilder
  }

  /**
   * Generates an ObjCClass out of data collected while visiting
   *
   * @return An ObjCClass that matches a Java class
   */
  fun buildClass(): ObjCClass {
    val methods = (methodDescriptors zip parameterNames).map { buildClassMethod(it.first.first, it.first.second, it.first.third, it.second)}

    val generatedClass = ObjCClassImpl(
      name = className,
      isForwardDeclaration = false,
      binaryName = null,
      location = Location(HeaderId("")) // Leaving headerId empty for now
    )
    generatedClass.methods.addAll(methods)
    generatedClass.baseClass = ObjCClassImpl(
      name = "NSObject",
      binaryName = null,
      isForwardDeclaration = false,
      location = Location(headerId = HeaderId("usr/include/objc/NSObject.h")) // TODO: When implementing inheritance check for proper base class
    )
    return generatedClass
  }

  /**
   * Creates a ObjCMethod out of a
   *
   * @param methodName Name of method
   * @param methodDesc Descriptor of method
   * @param access Integer that describes access level of this method
   * @param paramNames List of parameter names of this method taken from MethodBuilder
   */
  private fun buildClassMethod(methodName: String, methodDesc: String, access: Int, paramNames: List<String>): ObjCMethod {
    if (methodName == "<init>") {
      return ObjCMethod(
        selector = "init",
        encoding = "[]",
        parameters = listOf<Parameter>(), // TODO: Support constructor arguments
        returnType = ObjCInstanceType(nullability = ObjCPointer.Nullability.Unspecified),
        isVariadic = false,
        isClass = false,
        nsConsumesSelf = true,
        nsReturnsRetained = true,
        isOptional = false,
        isInit = true,
        isExplicitlyDesignatedInitializer = false)
    } else {
      val selector = StringBuilder(methodName)
      val methodParameters = parseMethodParameters(methodDesc, paramNames)
      val methodReturnType = parseMethodReturnType(methodDesc)
      if (methodParameters.size >= 1) methodParameters.subList(1,methodParameters.size).forEach { selector.append(":" + it.name) }
      return ObjCMethod(
        selector = if (methodParameters.size > 1) "$selector:" else if (methodParameters.size == 1) "$methodName:" else methodName,
        encoding = "[]", //TODO: Implement encoding properly
        parameters = methodParameters,
        returnType = methodReturnType,
        isVariadic = false,
        isClass = access == Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, // TODO: Currently only handles Public instance and Public static methods, true when static, false when instance
        nsConsumesSelf = false,
        nsReturnsRetained = false,
        isOptional = false,
        isInit = false,
        isExplicitlyDesignatedInitializer = false)
    }
  }

  /**
   * Parses an ASM method's parameters and returns a list of Kotlin parameters
   *
   * @param method ASM method descriptor
   * @return A list of Kotlin parameters with associated types/names
   */

  private fun parseMethodParameters(methodDesc: String, paramNames: List<String>): List<Parameter> {
    val methodParameters = mutableListOf<Parameter>()
    val parameterTypes = getArgumentTypes(methodDesc)

    for (i in 0 until parameterTypes.size) {
      val paramName = paramNames.get(i)
      when (parameterTypes.get(i).className) {
        "boolean" -> methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "byte" -> methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "char" ->  methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "double" ->  methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "float" ->  methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "int" -> methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "long" ->  methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        "short" ->  methodParameters.add(Parameter(name = paramName, type = parseType(parameterTypes.get(i)), nsConsumed = false))
        else -> {
          throw NotImplementedError("Have not implemented this type yet: " + parameterTypes.get(i).className)
        }
      }
    }
    return methodParameters
  }

  /**
   * Parses an ASM method's return type and returns a Kotlin type
   *
   * @param method ASM method descriptor
   * @return Type corresponding to method's return type
   */
  private fun parseMethodReturnType(methodDesc: String): Type {
    return parseType(getReturnType(methodDesc))
  }

  /**
   * Helper function to parse ASM types and return a Kotlin type
   *
   * @param type ASM type
   * @return Kotlin type
   */
  private fun parseType(type: org.jetbrains.org.objectweb.asm.Type): Type {
    when (type.className) {
      "boolean" -> return ObjCBoolType
      "byte" -> return IntegerType(size = 1, spelling = "byte", isSigned = true)
      "char" -> return CharType
      "double" -> return FloatingType(size = 8, spelling = "double")
      "float" -> return FloatingType(size = 4, spelling = "float")
      "int" -> return IntegerType(size = 4, spelling = "int", isSigned = true)
      "long" -> return IntegerType(size = 8, spelling = "long", isSigned = true)
      "short" -> return IntegerType(size = 2, spelling = "short", isSigned = true)
      "void" -> return VoidType
      else -> {
        throw NotImplementedError("Have not implemented this type yet: " + type.className)
      }
    }
  }

}

/**
 * Visits methods from J2ObjCParser and collects parameter names
 *
 * @param paramNames List of parameter name strings to be added to
 */
private class MethodBuilder(val paramNames: MutableCollection<List<String>>): MethodVisitor(Opcodes.ASM5) {
  val params = mutableListOf<String>()

  override fun visitParameter(name: String, access: Int) {
    params.add(name)
  }

  override fun visitEnd() {
    paramNames.add(params)
    super.visitEnd()
  }
}

/**
 * Creates a list of ByteArrays of each .class file in a .jar file
 *
 * @param jarFile JarFile of java library wanting to be converted to klib
 * @return List of ByteArrays of each class file in the jar
 */
fun loadClassDataFromJar(jarFile: JarFile): Sequence<ByteArray> {
  return jarFile.entries().iterator().asSequence()
    .filter { it.name.endsWith(".class") }
    .map { jarFile.getInputStream(it).readBytes() }
}

/**
 * Helper function to load a jar file and create and populate a J2ObjC NativeIndex and return an IndexerResult with it
 *
 * @param jarFiles List of Java .class files to be loaded
 * @return A IndexerResult with a J2ObjCNativeIndex that is populated from jarFiles
 */
fun buildJ2ObjcNativeIndex(jarFiles: List<String>): IndexerResult {
  val jarClassData = loadClassDataFromJar(JarFile(jarFiles[0])) // Currently only reads one jar file
  val j2objcClasses = mutableListOf<ObjCClass>()
  for (cls in jarClassData) {
    val reader = ClassReader(cls)
    val parser = J2ObjCParser()
    reader.accept(parser, 0)
    j2objcClasses.add(parser.buildClass())
  }
  return IndexerResult(J2ObjCNativeIndex(j2objcClasses), CompilationWithPCH(emptyList<String>(), Language.J2ObjC))
}