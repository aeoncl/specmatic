package application

import `in`.specmatic.conversions.OpenApiSpecification
import `in`.specmatic.core.Feature
import `in`.specmatic.core.PassThroughHook
import `in`.specmatic.core.log.logger
import `in`.specmatic.mock.ScenarioStub
import `in`.specmatic.stub.createStubFromFeature
import `in`.specmatic.stub.loadContractStubsFromFiles
import `in`.specmatic.stub.loadContractStubsFromImplicitPaths
import `in`.specmatic.stub.openApicontractToMockScenarios
import org.springframework.stereotype.Component
import java.io.File

@Component
class StubLoaderEngine {
    fun loadStubs(contractPaths: List<String>, dataDirs: List<String>, exampleAsStub: Boolean = false ): List<Pair<Feature, List<ScenarioStub>>> {
        contractPaths.forEach { contractPath ->
            if(!File(contractPath).exists()) {
                logger.log("$contractPath does not exist.")
            }
        }
        var contractStubs = when {
            dataDirs.isNotEmpty() -> loadContractStubsFromFiles(contractPaths, dataDirs)
            else -> loadContractStubsFromImplicitPaths(contractPaths)
        }

//        val openApiSpec = OpenApiSpecification.fromFile(contractPaths.first())
//        contractStubs.map { featurePair ->
//            openApicontractToMockScenarios(featurePair.first, openApiSpec)
//
//        }

        if (exampleAsStub) {
            contractStubs = contractStubs.map { featurePair ->
                val stubs = createStubFromFeature(featurePair.first)
                Pair(featurePair.first, featurePair.second.plus(stubs))
            }
        }
        return contractStubs
    }
}