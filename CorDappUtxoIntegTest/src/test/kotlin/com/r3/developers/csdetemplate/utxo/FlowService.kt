package com.r3.developers.csdetemplate.utxo

import net.corda.flow.rpcops.v1.types.request.StartFlowParameters
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface FlowService {

    @POST("flow/{hash}")
    fun start(@Path("hash") hash: String, @Body request: StartFlowParameters): Call<FlowStatusResponse>
}
