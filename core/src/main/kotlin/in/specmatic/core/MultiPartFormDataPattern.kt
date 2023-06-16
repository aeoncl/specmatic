package `in`.specmatic.core

import `in`.specmatic.core.Result.Failure
import `in`.specmatic.core.Result.Success
import `in`.specmatic.core.pattern.*
import `in`.specmatic.core.value.StringValue
import java.io.File
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.name

sealed class MultiPartFormDataPattern(open val name: String, open val contentType: String?) {
    abstract fun newBasedOn(row: Row, resolver: Resolver): List<MultiPartFormDataPattern?>
    abstract fun generate(resolver: Resolver): MultiPartFormDataValue
    abstract fun matches(value: MultiPartFormDataValue, resolver: Resolver): Result
    abstract fun nonOptional(): MultiPartFormDataPattern
}

data class MultiPartContentPattern(override val name: String, val content: Pattern, override val contentType: String? = null) : MultiPartFormDataPattern(name, contentType) {
    override fun newBasedOn(row: Row, resolver: Resolver): List<MultiPartContentPattern?> =
            newBasedOn(row, withoutOptionality(name), content, resolver).map { newContent: Pattern -> MultiPartContentPattern(
                withoutOptionality(name),
                newContent,
                contentType
            ) }.let {
                when{
                    isOptional(name) && !row.containsField(withoutOptionality(name)) -> listOf(null).plus(it)
                    else -> it
                }
            }

    override fun generate(resolver: Resolver): MultiPartFormDataValue =
            MultiPartContentValue(name, resolver.withCyclePrevention(content, content::generate), specifiedContentType = contentType)

    override fun matches(value: MultiPartFormDataValue, resolver: Resolver): Result {
        if(withoutOptionality(name) != value.name)
            return Failure("The contract expected a part name to be $name, but got ${value.name}", failureReason = FailureReason.PartNameMisMatch)

//        if(contentType != null && value.contentType != null && contentType != value.contentType)
//            return Failure("Expected $contentType, but got ${value.contentType}")

        return when(value) {
            is MultiPartFileValue -> {
                try {
                    val parsedContent = try { content.parse(value.content.toStringLiteral(), resolver) } catch (e: Throwable) { StringValue(value.content.toStringLiteral()) }
                    resolver.matchesPattern(name, content, parsedContent)
                } catch (e: ContractException) {
                    Failure(e.report(), breadCrumb = "content")
                } catch (e: Throwable) {
                    Failure("Expected a ${content.typeName} but got ${value.content.toStringLiteral()}", breadCrumb = "content")
                }
            }
            is MultiPartContentValue -> {
                if(value.content is StringValue) {
                    return try {
                        val parsedContent = try { content.parse(value.content.toStringLiteral(), resolver) } catch (e: Throwable) { StringValue(value.content.toStringLiteral()) }
                        resolver.matchesPattern(name, content, parsedContent)
                    } catch (e: ContractException) {
                        Failure(e.report(), breadCrumb = "content")
                    } catch (e: Throwable) {
                        Failure("Expected a ${content.typeName} but got ${value.content.toStringLiteral()}", breadCrumb = "content")
                    }
                } else {
                    content.matches(value.content, resolver)
                }
            }
        }
    }

    override fun nonOptional(): MultiPartFormDataPattern {
        return copy(name = withoutOptionality(name))
    }
}

data class MultiPartFilePattern(override val name: String, val filename: Pattern, override val contentType: String? = null, val contentEncoding: String? = null) : MultiPartFormDataPattern(name, contentType) {
    override fun newBasedOn(row: Row, resolver: Resolver): List<MultiPartFormDataPattern?> {
        val rowKey = "${name}_filename"
        return listOf(this.copy(filename = if(row.containsField(rowKey)) ExactValuePattern(StringValue(row.getField(rowKey))) else filename))
    }

    override fun generate(resolver: Resolver): MultiPartFormDataValue =
            MultiPartFileValue(name, resolver.withCyclePrevention(filename, filename::generate).toStringLiteral(), contentType ?: "", contentEncoding)

    override fun matches(value: MultiPartFormDataValue, resolver: Resolver): Result {
        return when {
            value !is MultiPartFileValue -> Failure("The contract expected a file, but got content instead.")
            name != value.name -> Failure("The contract expected a part name to be $name, but got ${value.name}.", failureReason = FailureReason.PartNameMisMatch)
            fileContentMismatch(value, resolver) -> fileContentMismatchError(value, resolver)
            //TODO: Fix below comment
//            contentType != null && value.contentType != null && value.contentType != contentType -> Failure("The contract expected ${contentType.let { "content type $contentType" }}, but got ${value.contentType?.let { "content type $value.contentType" } ?: "no content type."}.")
            contentEncoding != null && value.contentEncoding != contentEncoding -> {
                val contentEncodingMessage = contentEncoding.let { "content encoding $contentEncoding" }
                val receivedContentEncodingMessage = value.contentEncoding?.let { "content encoding ${value.contentEncoding}" }
                        ?: "no content encoding"

                Failure("The contract expected ${contentEncodingMessage}, but got ${receivedContentEncodingMessage}.", breadCrumb = "contentEncoding")
            }
            else -> Success()
        }
    }

    private fun fileContentMismatchError(
        value: MultiPartFileValue,
        resolver: Resolver
    ) = when(filename) {
        is ExactValuePattern -> {
            Failure(
                "In the part named $name, the contents in request did not match the value in file ${filename.pattern.toStringLiteral()}",
                failureReason = FailureReason.PartNameMisMatch
            )
        }
        else -> Failure(
            "In the part named $name, the contract expected the filename to be ${filename.typeName}, but got ${value.filename}.",
            failureReason = FailureReason.PartNameMisMatch,
            cause = filename.matches(StringValue(value.filename), resolver) as Failure
        )
    }

    private fun fileContentMismatch(
        value: MultiPartFileValue,
        resolver: Resolver
    ): Boolean {
        return when(filename) {
            is ExactValuePattern -> {
                val patternFilePath = filename.pattern.toStringLiteral()
                val bytes = File(patternFilePath).canonicalFile.also {
                    if(!it.exists()) {
                        println(it.canonicalFile.path + " does not exist")
                        throw Exception(it.canonicalFile.path + " does not exist")
                    }
                }.readBytes()
                val contentBytes = value.content.bytes
                !bytes.contentEquals(contentBytes)
            }
            else ->
                !filename.matches(StringValue(value.filename), resolver).isSuccess()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun fileNameFromPath(patternFilePath: String) = Path(patternFilePath).name

    override fun nonOptional(): MultiPartFormDataPattern {
        return copy(name = withoutOptionality(name))
    }
}
