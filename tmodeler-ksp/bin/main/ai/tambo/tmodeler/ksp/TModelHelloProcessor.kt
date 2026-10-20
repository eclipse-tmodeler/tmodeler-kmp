package ai.tambo.tmodeler.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

// Spike Phase 1 : valide juste que KSP tourne sur la cible wasmJs (pas de détection
// TModel/génération de code pour l'instant, cf. plan-reflector-web-kmp.md Phase 1).
class TModelHelloProcessor(
    private val logger: com.google.devtools.ksp.processing.KSPLogger
) : SymbolProcessor {

    private var done = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (done) return emptyList()
        done = true

        val classNames = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .map { it.qualifiedName?.asString() ?: it.simpleName.asString() }
            .toList()

        logger.warn("[tmodeler-ksp spike] ${classNames.size} classes visibles: ${classNames.take(20)}")
        return emptyList()
    }
}

class TModelHelloProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TModelHelloProcessor(environment.logger)
    }
}
