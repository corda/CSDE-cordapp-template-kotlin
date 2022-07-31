package com.r3.hellocorda

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable

data class ArraySumArgs(val data: IntArray = IntArray(0))
data class IntArrayDTO(val values: IntArray)
data class ResultMessage(val sum: Int)

class ArraySumRPCFlow : RPCStartableFlow {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val args = requestBody.getRequestBodyAs<ArraySumArgs>(jsonMarshallingService)
        val retVal = flowEngine.subFlow(ArraySumSubFlow(IntArrayDTO(args.data)))
        return jsonMarshallingService.format(ResultMessage(retVal))
    }
}

class ArraySumSubFlow(val todo: IntArrayDTO) : SubFlow<Int> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    override fun call(): Int {
        var sum = 0
        for (i in todo.values) {
            sum += i
        }
        return sum
    }
}

