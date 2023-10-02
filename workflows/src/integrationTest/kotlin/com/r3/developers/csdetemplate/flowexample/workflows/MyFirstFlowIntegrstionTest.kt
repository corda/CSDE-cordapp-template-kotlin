package com.r3.developers.csdetemplate.flowexample.workflows

import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlow
import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlowResponder
import com.r3.developers.csdetemplate.flowexample.workflows.MyFirstFlowStartArgs
import com.github.manosbatsis.corda5.testutils.integration.junit5.CombinedWorkerMode
import com.github.manosbatsis.corda5.testutils.integration.junit5.Corda5NodesConfig
import com.github.manosbatsis.corda5.testutils.integration.junit5.Corda5NodesExtension
import com.github.manosbatsis.corda5.testutils.integration.junit5.nodehandles.NodeHandles
import com.github.manosbatsis.corda5.testutils.rest.client.model.FlowRequest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

// Add the Corda5 nodes extension
@ExtendWith(Corda5NodesExtension::class)
class MyFirstFlowIntegrstionTest {

    // Optional
    val config = Corda5NodesConfig(
        authUsername = "admin",
        authPassword = "admin",
        baseUrl = "https://localhost:8888/api/v1/",
        httpMaxWaitSeconds = 120,
        debug = false,
        projectDir = Corda5NodesConfig.gradleRootDir,
        combinedWorkerMode = CombinedWorkerMode.SHARED
    )

    // Corda5 nodes extension provides the NodeHandles
    // based on config/static-network-config.json
    @Test
    fun `Test that MyFirstFLow call succeeds`(nodeHandles: NodeHandles) {
        // Get node handles
        val aliceNode = nodeHandles.getByCommonName("Alice")
        val bobNode = nodeHandles.getByCommonName("Bob")

        // Use a map with a String value VS MyFirstFlowStartArgs with X500Name
        // as the latter is not serialized properly
        val myFirstFlowStartArgs = mapOf("otherMember" to bobNode.memberX500Name.toString())
        // Call the flow as Alice
        val createdStatus = aliceNode.waitForFlow(
            FlowRequest(
                flowClassName = MyFirstFlow::class.java.canonicalName,
                requestBody = myFirstFlowStartArgs
            )
        )
        println("createdStatus: $createdStatus")
        assertTrue(createdStatus.isSuccess())
    }
}
