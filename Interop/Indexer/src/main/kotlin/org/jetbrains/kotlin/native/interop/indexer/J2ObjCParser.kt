package org.jetbrains.kotlin.native.interop.indexer

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type.getArgumentTypes
import org.jetbrains.org.objectweb.asm.Type.getReturnType
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Visits a Java class and builds an ObjCClass
 */
class J2ObjCParser: ClassVisitor(Opcodes.ASM5) {

  var className = ""
  var superName = ""
  val methodDescriptors = mutableListOf<MethodDescriptor>()
  val parameterNames = mutableListOf<List<String>>()

  override fun visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String?,
                     superName: String,
                     interfaces: Array<out String>?) {
    className = name
    this.superName = superName
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String?,
                           exceptions: Array<out String>?): MethodVisitor? {

    val methodBuilder = MethodBuilder(parameterNames)
    methodDescriptors.add(MethodDescriptor(name, descriptor, access))
    return methodBuilder
  }

  /**
   * Generates an ObjCClass out of data collected while visiting
   *
   * @return An ObjCClass that matches a Java class
   */
  fun buildClass(): ObjCClass {
    val methods = (methodDescriptors zip parameterNames).map { buildClassMethod(it.first, it.second)}

    val generatedClass = ObjCClassImpl(
      name = className,
      isForwardDeclaration = false,
      binaryName = null,
      location = Location(HeaderId("")) // Leaving headerId empty for now.
    )
    generatedClass.methods.addAll(methods)
    if (superName == "java/lang/Object") {
      generatedClass.baseClass = ObjCClassImpl(
        name = "NSObject",
        binaryName = null,
        isForwardDeclaration = false,
        location = Location(headerId = HeaderId("usr/include/objc/NSObject.h"))
      )
    } else {
      generatedClass.baseClass = ObjCClassImpl(
        name = superName,
        binaryName = null,
        isForwardDeclaration = false,
        location = Location(headerId = HeaderId(""))
      )
    }
    return generatedClass
  }

  /**
   * Creates a ObjCMethod out of method data and parameter names
   *
   * @param methodDescriptor A methodDescriptor containing the name, descriptor string, and access level of method
   * @param paramNames List of parameter names of this method taken from MethodBuilder
   * @return An ObjCMethod built from the descriptor
   */
  private fun buildClassMethod(methodDescriptor: MethodDescriptor, paramNames: List<String>): ObjCMethod {
    if (methodDescriptor.isConstructor) {
      return ObjCMethod(
        selector = "init",
        encoding = "[]",
        parameters = listOf<Parameter>(), // TODO: Support constructor arguments.
        returnType = ObjCInstanceType(nullability = ObjCPointer.Nullability.Unspecified),
        isVariadic = false,
        isClass = false,
        nsConsumesSelf = true,
        nsReturnsRetained = true,
        isOptional = false,
        isInit = true,
        isExplicitlyDesignatedInitializer = false)
    } else {
      val selector = StringBuilder(methodDescriptor.name)
      val methodParameters = parseMethodParameters(methodDescriptor.descriptor, paramNames)
      val methodReturnType = parseMethodReturnType(methodDescriptor.descriptor)
      if (methodParameters.size >= 1) methodParameters.subList(1,methodParameters.size).forEach { selector.append(":" + it.name) }
      return ObjCMethod(
        selector = if (methodParameters.size > 1) "$selector:" else if (methodParameters.size == 1) "${methodDescriptor.name}:" else methodDescriptor.name,
        encoding = "[]", //TODO: Implement encoding properly
        parameters = methodParameters,
        returnType = methodReturnType,
        isVariadic = false,
        isClass = methodDescriptor.access == Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, // TODO: Currently only handles Public instance and Public static methods, true when static, false when instance.
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
   * @param methodDesc A methodDescriptor containing the name, descriptor string, and access level of method
   * @param paramNames List of parameter names of methods from [methodDesc]
   * @return A list of Kotlin parameters with associated types/names
   */

  private fun parseMethodParameters(methodDesc: String, paramNames: List<String>): List<Parameter> {
    val parameterTypes = getArgumentTypes(methodDesc)

    return parameterTypes.mapIndexed { i, paramType ->
      when (paramType.className) {
        "boolean", "byte", "char", "double", "float", "int", "long", "short" ->
         Parameter(name = paramNames.get(i), type = parseType(parameterTypes.get(i)), nsConsumed = false)
        else -> TODO("Have not implemented this type yet: ${parameterTypes.get(i).className}")
      }
    }
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
    return when (type.className) {
      "boolean" -> ObjCBoolType
      "byte" -> IntegerType(size = 1, spelling = "byte", isSigned = true)
      "char" -> CharType
      "double" -> FloatingType(size = 8, spelling = "double")
      "float" -> FloatingType(size = 4, spelling = "float")
      "int" -> IntegerType(size = 4, spelling = "int", isSigned = true)
      "long" -> IntegerType(size = 8, spelling = "long", isSigned = true)
      "short" -> IntegerType(size = 2, spelling = "short", isSigned = true)
      "void" -> VoidType
      else -> TODO("Have not implemented this type yet: ${type.className}")
    }
  }

}

data class MethodDescriptor(val name: String, val descriptor: String, val access: Int) {
  val isConstructor: Boolean = (name == "<init>")
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
 * Helper function to load a jar file and create and populate a J2ObjC NativeIndex and return an IndexerResult with it
 *
 * @param jarFiles List of Java .jar file locations to be loaded
 * @return A IndexerResult with a J2ObjCNativeIndex that is populated from jarFiles
 */
fun buildJ2ObjcNativeIndex(jarFiles: List<String>): IndexerResult {
  val jarFile = JarFile(jarFiles[0])

  val j2objcClasses = jarFile.use { it.entries().iterator().asSequence()
    .filter{ it.name.endsWith(".class")}.map {
      val parser = J2ObjCParser()
      ClassReader(jarFile.getInputStream(it).readBytes()).accept(parser,0)
      parser.buildClass()
    }.toList()
  }

  return IndexerResult(J2ObjCNativeIndex(j2objcClasses), CompilationWithPCH(emptyList<String>(), Language.J2ObjC))
}
