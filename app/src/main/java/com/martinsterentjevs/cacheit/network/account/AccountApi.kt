package com.martinsterentjevs.cacheit.network.account

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

interface AccountApi {
    @GET("/account")
    suspend fun getAccount() : AccountProfileDto
    @GET("/account/devices")
    suspend fun getDevices() : List<DeviceSessionDto>
    @DELETE("/account")
    suspend fun deleteAccount()
    @DELETE("/account/devices/{deviceId}")
    suspend fun removeDevice(
        @Path("deviceId") deviceId: UUID
    )
}