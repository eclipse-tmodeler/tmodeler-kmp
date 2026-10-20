package ai.tambo.tmodeler.engines

import ai.tambo.tmodeler.extension.intelligence.TAiEngine
import ai.tambo.tmodeler.extension.security.TSecurityEngine
import ai.tambo.tmodeler.extension.synchronization.TSyncEngine
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.support.TModelerTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Les moteurs : TSyncEngine (synchronisation), TSecurityEngine (sécurité), TAiEngine (intelligence).
 *
 * TModeler fonctionne SANS aucun moteur externe : il embarque des moteurs de base. Une extension (ou votre
 * propre classe) remplace un moteur en l'héritant et en le branchant avant `connect(...)` :
 *
 *     TModeler.start()
 *         .synchronization(MonSync())
 *         .security(MaSecurite())
 *         .intelligence(MonIa())
 *         .connect(...)
 *
 * Ces tests n'utilisent aucune extension : uniquement les classes de base et des sous-classes locales.
 */
class EnginesTest : TModelerTest() {

    private val tm = TModeler.start()
    private val originalSecurity = tm.securityEngine()
    private val originalAi = tm.aiEngine()

    @AfterTest
    fun restoreDefaults() {
        tm.security(originalSecurity)
        tm.intelligence(originalAi)
    }

    @Test
    fun `le moteur de synchronisation de base fournit une configuration serveur`() {
        val engine: TSyncEngine = tm.syncEngine()
        assertTrue(engine.newServerConfig() is TServerConfigBase)
    }

    @Test
    fun `le moteur d'intelligence de base renvoie la donnee telle quelle`() {
        assertEquals("bonjour", TAiEngine().predict("bonjour"))
    }

    @Test
    fun `brancher un moteur d'intelligence personnalise`() {
        class ShoutingAi : TAiEngine() {
            override fun predict(data: String) = data.uppercase()
        }

        tm.intelligence(ShoutingAi())

        assertEquals("BONJOUR", tm.aiEngine().predict("bonjour"))
    }

    @Test
    fun `brancher un moteur de securite personnalise`() {
        class RecordingSecurity : TSecurityEngine() {
            val protectedModels = mutableListOf<TBaseModel>()
            override fun protect(model: TBaseModel) { protectedModels += model }
        }
        val engine = RecordingSecurity()

        tm.security(engine)

        assertSame(engine, tm.securityEngine())
    }

    @Test
    fun `les setters de TModeler sont chainables`() {
        val chained = tm.security(originalSecurity).intelligence(originalAi)
        assertSame(tm, chained)
    }
}
