package org.jetbrains.kotlin.native.interop.gen

class StubIrImportsCollector(
        private val currectPackage: String
) : StubIrVisitor<Nothing?, Unit> {

    val result: MutableSet<String> = mutableSetOf()

    override fun visitClass(element: ClassStub, data: Nothing?) {
        element.interfaces.forEach(this::importStubTypePackage)
        element.superClassInit?.type?.let(this::importStubTypePackage)
        element.children.forEach { it.accept(this, data) }
    }

    override fun visitTypealias(element: TypealiasStub, data: Nothing?) {
       element.aliasee.let(this::importStubTypePackage)
    }

    override fun visitFunction(element: FunctionStub, data: Nothing?) {

        element.returnType.let(this::importStubTypePackage)
        element.receiver?.type?.let(this::importStubTypePackage)
        element.parameters.map(FunctionParameterStub::type).forEach(this::importStubTypePackage)
    }

    override fun visitProperty(element: PropertyStub, data: Nothing?) {
        element.receiverType?.let(this::importStubTypePackage)
        element.type.let(this::importStubTypePackage)
    }

    override fun visitConstructor(constructorStub: ConstructorStub, data: Nothing?) {
        constructorStub.parameters.map(FunctionParameterStub::type).forEach(this::importStubTypePackage)
    }

    override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: Nothing?) {
        propertyAccessor.parameters.map(FunctionParameterStub::type).forEach(this::importStubTypePackage)
    }

    override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: Nothing?) {
        simpleStubContainer.simpleContainers.forEach { visitSimpleStubContainer(it, data) }
        simpleStubContainer.children.forEach { it.accept(this, data) }
    }

    private fun importStubTypePackage(stubType: StubType) {
        when (stubType) {
            is ClassifierStubType -> {
                importPackage(stubType.classifier.pkg)
                stubType.typeArguments
                        .filterIsInstance<TypeArgumentStub>()
                        .forEach(this::importTypeArgumentPackage)
            }
            is AbbreviatedType -> {
                importStubTypePackage(stubType.underlyingType)
                importPackage(stubType.abbreviatedClassifier.pkg)
            }
        }
    }

    private fun importTypeArgumentPackage(argumentStub: TypeArgumentStub) {
        importStubTypePackage(argumentStub.type)
    }

    private fun importPackage(pkg: String) {
        if (pkg == "kotlin" || pkg == "kotlinx.cinterop") return
        if (currectPackage != pkg) result += pkg
    }
}