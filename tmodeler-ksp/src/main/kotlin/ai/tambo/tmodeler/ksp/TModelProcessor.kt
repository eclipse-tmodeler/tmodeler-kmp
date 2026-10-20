package ai.tambo.tmodeler.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

class TModelProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var generated = false
    private val roots = setOf("ai.tambo.tmodeler.model.TModel", "ai.tambo.tmodeler.model.TModelGb")
    private val frameworkClasses = roots + setOf(
        "ai.tambo.tmodeler.model.TModel0",
        "ai.tambo.tmodeler.model.TBaseModel",
        "ai.tambo.tmodeler.core.TobGb"
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        val files = resolver.getAllFiles().toList()
        val classes = files.asSequence().flatMap { classesIn(it.declarations) }.toList()
        // KSP 1.9 validate() recurses indefinitely through self-bounded model types.
        val deferred = classes.filter { cls -> cls.superTypes.any { it.resolve().isError } }
        if (deferred.isNotEmpty()) {
            deferred.forEach { cls ->
                environment.logger.warn("TModeler: unresolved supertype in ${cls.qualifiedName?.asString()}: ${cls.superTypes.map { it.toString() }.toList()}", cls)
            }
            return deferred
        }

        val candidates = classes.filter { cls ->
            cls.qualifiedName?.asString() !in frameworkClasses &&
                cls.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() in roots } &&
                Modifier.ABSTRACT !in cls.modifiers && Modifier.SEALED !in cls.modifiers
        }.sortedBy { it.qualifiedName?.asString() }
        for (generic in candidates.filter { it.typeParameters.isNotEmpty() }) {
            environment.logger.warn(
                "TModeler: generic model ${generic.qualifiedName?.asString()} is not registered; only concrete non-generic models have generated factories.",
                generic
            )
        }
        val models = candidates.filter { it.typeParameters.isEmpty() }

        val propertiesByModel = models.associateWith { model ->
            val ancestors = model.getAllSuperTypes().mapNotNull { it.declaration as? KSClassDeclaration }
                .filter { it.qualifiedName?.asString() !in frameworkClasses }
            (sequenceOf(model) + ancestors).flatMap { it.getDeclaredProperties() }
                .distinctBy { it.simpleName.asString() }.filter { property ->
                    property.annotations.none {
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == "ai.tambo.tmodeler.model.TMignore"
                    }
                }.toList()
        }
        val unresolved = propertiesByModel.values.flatten().filter { it.type.resolve().isError }
        if (unresolved.isNotEmpty()) return unresolved
        val fieldsByModel = propertiesByModel.mapValues { (_, properties) ->
            properties.filter { isField(it) }.sortedBy { it.simpleName.asString() }
        }

        var invalid = false
        for ((model, fields) in fieldsByModel) {
            if (model.classKind != ClassKind.CLASS || model.typeParameters.isNotEmpty() ||
                !accessible(model) || Modifier.INNER in model.modifiers ||
                model.getConstructors().none { ctor ->
                    accessible(ctor) && ctor.parameters.all { it.hasDefault || it.isVararg }
                }
            ) {
                environment.logger.error(
                    "TModeler: ${model.qualifiedName?.asString()} must be an accessible, non-inner, non-generic class with a callable no-argument constructor.",
                    model
                )
                invalid = true
            }
            for (field in fields) {
                if (!accessible(field) || field.getter?.modifiers?.any { it == Modifier.PRIVATE || it == Modifier.PROTECTED } == true ||
                    field.type.resolve().isMarkedNullable
                ) {
                    environment.logger.error("TModeler: field ${field.simpleName.asString()} must be accessible and non-null, or marked @TMignore.", field)
                    invalid = true
                }
            }
        }
        if (invalid) return emptyList()

        val packageName = environment.options["tmodeler.registryPackage"] ?: "ai.tambo.tmodeler.generated"
        if (!packageName.split('.').all { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) }) {
            environment.logger.error("TModeler: invalid tmodeler.registryPackage: $packageName")
            return emptyList()
        }
        environment.codeGenerator.createNewFile(
            Dependencies(true, *files.toTypedArray()),
            packageName,
            "TModelRegistration_Generated"
        ).bufferedWriter().use { writer ->
            writer.appendLine("package ${reference(packageName)}")
            writer.appendLine()
            writer.appendLine("import ai.tambo.tmodeler.model.TModelRegistry")
            writer.appendLine()
            writer.appendLine("fun registerAllTModels() {")
            for ((model, fields) in fieldsByModel) {
                val type = reference(model.qualifiedName!!.asString())
                writer.appendLine("    TModelRegistry.register(")
                writer.appendLine("        $type::class,")
                writer.appendLine("        factory = { $type() },")
                writer.appendLine("        fieldsOf = { inst -> listOf(")
                for (field in fields) {
                    val name = field.simpleName.asString()
                    writer.appendLine("            inst.`$name`.apply { name = ${literal(name)} },")
                }
                writer.appendLine("        ) },")
                writer.appendLine("        packageName = ${literal(model.packageName.asString())}")
                writer.appendLine("    )")
            }
            writer.appendLine("}")
        }
        generated = true
        return emptyList()
    }

    private fun classesIn(declarations: Sequence<KSDeclaration>): Sequence<KSClassDeclaration> =
        declarations.filterIsInstance<KSClassDeclaration>().flatMap { sequenceOf(it) + classesIn(it.declarations) }

    private fun accessible(declaration: KSDeclaration): Boolean =
        Modifier.PRIVATE !in declaration.modifiers && Modifier.PROTECTED !in declaration.modifiers &&
            (declaration.parentDeclaration?.let { accessible(it) } ?: true)

    private fun isField(property: KSPropertyDeclaration): Boolean {
        val declaration = property.type.resolve().declaration as? KSClassDeclaration ?: return false
        return declaration.qualifiedName?.asString() == "ai.tambo.tmodeler.field.base.BaseField" ||
            declaration.getAllSuperTypes().any {
                it.declaration.qualifiedName?.asString() == "ai.tambo.tmodeler.field.base.BaseField"
            }
    }

    private fun reference(name: String): String = name.split('.').joinToString(".") { "`$it`" }

    private fun literal(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
}

class TModelProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = TModelProcessor(environment)
}
