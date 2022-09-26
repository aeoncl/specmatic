package `in`.specmatic.core.wsdl.parser

import `in`.specmatic.core.log.logger
import `in`.specmatic.core.pattern.XMLPattern
import `in`.specmatic.core.value.XMLNode
import `in`.specmatic.core.value.XMLValue

data class WSDLTypeInfo(val nodes: List<XMLValue> = emptyList(), val types: Map<String, XMLPattern> = emptyMap(), val namespacePrefixes: Set<String> = emptySet()) {
    fun getNamespaces(wsdlNamespaces: Map<String, String>): Map<String, String> {
        logger.debug(wsdlNamespaces.toString())
        logger.debug(namespacePrefixes.toString())

        return namespacePrefixes.toList().associateWith {
            wsdlNamespaces.getValue(it)
        }
    }

    fun plus(otherWSDLTypeInfo: WSDLTypeInfo): WSDLTypeInfo {
        return WSDLTypeInfo(
            this.nodes.plus(otherWSDLTypeInfo.nodes),
            this.types.plus(otherWSDLTypeInfo.types),
            this.namespacePrefixes.plus(otherWSDLTypeInfo.namespacePrefixes)
        )
    }

    val nodeTypeInfo: XMLNode
        get() {
            return XMLNode(TYPE_NODE_NAME, emptyMap(), nodes)
        }
}