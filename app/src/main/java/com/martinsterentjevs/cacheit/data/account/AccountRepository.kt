package com.martinsterentjevs.cacheit.data.account

import com.martinsterentjevs.cacheit.network.account.AccountApi
import com.martinsterentjevs.cacheit.network.account.AccountProfileDto
import com.martinsterentjevs.cacheit.network.account.DeviceSessionDto
import java.util.UUID
import javax.inject.Inject

interface AccountRepository {
    suspend fun getAccount(): AccountProfileDto
    suspend fun listDevices(): List<DeviceSessionDto>
    suspend fun revokeDevice(deviceId: UUID)
    suspend fun deleteAccount()
}

internal class AccountRepositoryImpl
@Inject constructor(private val accountApi: AccountApi):AccountRepository{
    override suspend fun getAccount(): AccountProfileDto = accountApi.getAccount()

    override suspend fun listDevices(): List<DeviceSessionDto> = accountApi.getDevices()

    override suspend fun revokeDevice(deviceId: UUID) = accountApi.removeDevice(deviceId)

    override suspend fun deleteAccount() = accountApi.deleteAccount()
}