package `in`.specmatic.core.value

import `in`.specmatic.core.ExampleDeclarations
import `in`.specmatic.core.Result
import `in`.specmatic.core.pattern.ExactValuePattern
import `in`.specmatic.core.pattern.Pattern
import `in`.specmatic.core.pattern.StringPattern
import io.ktor.http.*
import io.ktor.util.*
import org.w3c.dom.Document
import org.w3c.dom.Node

data class BinaryValue(val byteArray: ByteArray = ByteArray(0)) : Value, ScalarValue, XMLValue {
    override val httpContentType = "application/octet-stream"

    override fun valueErrorSnippet(): String = displayableValue()

    @OptIn(InternalAPI::class)
    override fun displayableValue(): String = toStringLiteral().quote()
    override fun toStringLiteral() = byteArray.toString()
    override fun displayableType(): String = "binary"
    override fun exactMatchElseType(): Pattern = ExactValuePattern(this)

    override fun build(document: Document): Node = document.createTextNode(byteArray.toString())

    override fun matchFailure(): Result.Failure =
        Result.Failure("Unexpected child value found: $byteArray")

    override fun addSchema(schema: XMLNode): XMLValue = this

    override fun listOf(valueList: List<Value>): Value {
        return JSONArrayValue(valueList)
    }

    override fun type(): Pattern = StringPattern()

    override fun typeDeclarationWithKey(
        key: String,
        types: Map<String, Pattern>,
        exampleDeclarations: ExampleDeclarations
    ): Pair<TypeDeclaration, ExampleDeclarations> =
        primitiveTypeDeclarationWithKey(key, types, exampleDeclarations, displayableType(), byteArray.toString())

    override fun typeDeclarationWithoutKey(
        exampleKey: String,
        types: Map<String, Pattern>,
        exampleDeclarations: ExampleDeclarations
    ): Pair<TypeDeclaration, ExampleDeclarations> =
        primitiveTypeDeclarationWithoutKey(
            exampleKey,
            types,
            exampleDeclarations,
            displayableType(),
            byteArray.toString()
        )

    override val nativeValue: ByteArray
        get() = byteArray

    override fun toString() = byteArray.toString()
}
