package com.r3.developers.csdetemplate.utxo

import net.corda.simulator.RequestData
import net.corda.simulator.SimulatedVirtualNode
import net.corda.simulator.Simulator
import net.corda.simulator.factories.SimulatorConfigurationBuilder
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

internal class IssueTokenFlowTest {

    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")
    private val charlieX500 = MemberX500Name.parse("CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB")
    private val danielX500 = MemberX500Name.parse("CN=Daniel, OU=Test Dept, O=R3, L=London, C=GB")

    private lateinit var simulator: Simulator
    private lateinit var aliceNode: SimulatedVirtualNode;
    private lateinit var bobNode: SimulatedVirtualNode;

    @BeforeEach
    fun setUp() {
        val configuration = SimulatorConfigurationBuilder.create().build()
        simulator = Simulator(configuration)

        aliceNode = simulator.createVirtualNode(aliceX500, IssueTokenFlow::class.java)
        bobNode = simulator.createVirtualNode(bobX500, IssueTokenFlow::class.java)
    }

    @AfterEach
    fun cleanUp() {
        simulator.close()
    }

    /*
    Simulator does not have/support NotaryLookup nor UtxoLedgerService for now (Fox1/Beta1) :(
     */
//    @Test
    fun issue() {
        val tokenIssueRequest = TokenIssueRequest(100, 2, bobX500)
        val requestData = RequestData.create("issueToBob", IssueTokenFlow::class.java, tokenIssueRequest)
        val callFlow = aliceNode.callFlow(requestData)
        Assertions.assertNotNull(callFlow)
    }
}