package com.r3.hellocorda

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable

data class ArraySumArgs(
    val data: IntArray? = IntArray(0)
)

data class IntArrayAndRange(val values: IntArray, val start: Int, val end: Int)

data class ResultMessage(
    val sum: Int
)

class ArraySumRPCFlowOnly : RPCStartableFlow {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        // val inputs = requestBody.getRequestBodyAs<InputMessage>(jsonMarshallingService)
        val args = requestBody.getRequestBodyAs<ArraySumArgs>(jsonMarshallingService)

        var retVal = 0
        if (args.data != null) {
            for (v in args.data) {
                retVal += v
            }
            // retVal = flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(args.data, 0, args.data.size - 1)))
        }
        return jsonMarshallingService.format(ResultMessage(retVal))
    }
}

class ArraySumNFlows : RPCStartableFlow {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        // val inputs = requestBody.getRequestBodyAs<InputMessage>(jsonMarshallingService)
        val args = requestBody.getRequestBodyAs<ArraySumArgs>(jsonMarshallingService)

        var retVal = 0
        if (args.data != null) {
            // Partition the array
            var partSize = args.data.size / PARTITION_COUNT
            if (partSize < MIN_ARRAY_LEN) {
                partSize = MIN_ARRAY_LEN
            }
            var start: Int
            var end = 0
            do {
                start = end
                end = start + partSize - 1
                if(end >= args.data.size) {
                    end = args.data.size - 1
                }
                // Start a flow
                retVal = flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(args.data, start, end)))
            }
            while(end < args.data.size - 1)
        }
        return jsonMarshallingService.format(ResultMessage(retVal))
    }

    companion object {
        val PARTITION_COUNT = 12
        val MIN_ARRAY_LEN = 2
    }
}

class ArraySumSubFlow(val todo: IntArrayAndRange) : SubFlow<Int> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    override fun call(): Int {
        if (todo.start >= todo.end) return 0
        var sum: Int = 0
        val values = todo.values
        if (values.size <= MIN_ARRAY_LEN) {
            for (i in todo.start..todo.end) {
                sum += values[i]
            }
        } else {
            val mid = todo.end - todo.start / 2 + todo.start
            sum = flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(values, todo.start, mid))) +
                flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(values, mid, todo.end)))
        }
        return sum
    }
    companion object {
        val MIN_ARRAY_LEN = 2
    }
}

/*
class ArraySumNFlows : RPCStartableFlow {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        // val inputs = requestBody.getRequestBodyAs<InputMessage>(jsonMarshallingService)
        val args = requestBody.getRequestBodyAs<ArraySumArgs>(jsonMarshallingService)

        var retVal = 0
        if (args.data != null) {
            retVal = flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(args.data, 0, args.data.size - 1)))
        }
        return jsonMarshallingService.format(ResultMessage(retVal))
    }
}

class ArraySumCrazySubFlow(val todo: IntArrayAndRange) : SubFlow<Int> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    override fun call(): Int {
        if (todo.start >= todo.end) return 0
        var sum: Int = 0
        val values = todo.values
        if (values.size <= MIN_ARRAY_LEN) {
            for (i in todo.start..todo.end) {
                sum += values[i]
            }
        } else {
            val mid = todo.end - todo.start / 2 + todo.start
            sum = flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(values, todo.start, mid))) +
                flowEngine.subFlow(ArraySumSubFlow(IntArrayAndRange(values, mid, todo.end)))
        }
        return sum
    }
    companion object {
        val MIN_ARRAY_LEN = 2
    }
}
*/
