package run.qontract.core.pattern

import run.qontract.core.Resolver
import run.qontract.core.value.NoValue
import run.qontract.core.value.NullValue
import run.qontract.core.value.Value

data class LookupPattern(override val pattern: String, override val key: String? = null) : Pattern, Keyed {
    override fun withKey(key: String?): Pattern = this.copy(key = key)

    override fun matches(sampleData: Value?, resolver: Resolver) =
//            resolver.matchesPattern(key, resolver.getPattern(pattern), sampleData ?: NoValue)
            resolver.matchesPatternValue(key, pattern, sampleData ?: NullValue)

    override fun generate(resolver: Resolver) =
            when (key) {
                null -> resolver.getPattern(pattern).generate(resolver)
                else -> resolver.generate(key, resolver.getPattern(pattern))
            }

    override fun newBasedOn(row: Row, resolver: Resolver): List<Pattern> {
        return resolver.getPattern(pattern).newBasedOn(row, resolver)
    }

    override fun parse(value: String, resolver: Resolver): Value =
        resolver.getPattern(pattern).parse(value, resolver)

    fun resolvePattern(resolver: Resolver): Pattern = when(val definedPattern = resolver.getPattern(pattern)) {
        is LookupPattern -> definedPattern.resolvePattern(resolver)
        else -> definedPattern
    }
}
