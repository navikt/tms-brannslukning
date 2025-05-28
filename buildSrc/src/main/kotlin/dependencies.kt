import default.DependencyGroup
import default.KtorDefaults

object TmsVarselBuilder: DependencyGroup {
    override val groupId get() = "no.nav.tms.varsel"
    override val version get() = "1.0.4"

    val kotlinBuilder get() = dependency("kotlin-builder")
}

object Caffeine: DependencyGroup {
    override val groupId = "com.github.ben-manes.caffeine"
    override val version = "3.1.8"

    val caffeine get() = dependency("caffeine")
}

object Ktor303Server: KtorDefaults.ServerDefaults {
    override val version = "3.0.3"
}
object Ktor303Client: KtorDefaults.ClientDefaults {
    override val version = "3.0.3"
}
object Ktor303Serialization: KtorDefaults.SerializationDefaults {
    override val version = "3.0.3"
}
