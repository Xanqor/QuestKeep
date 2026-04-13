package com.questkeep.domain.repository

import com.questkeep.domain.entities.Campaign
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    fun getCampaignsForUser(userId: String): Flow<List<Campaign>>
    fun getCampaign(campaignId: String): Flow<Campaign?>
    suspend fun createCampaign(campaign: Campaign): Result<Campaign>
    suspend fun joinCampaign(inviteCode: String, userId: String): Result<Campaign>
    suspend fun leaveCampaign(campaignId: String, userId: String): Result<Unit>
    suspend fun deleteCampaign(campaignId: String): Result<Unit>
    suspend fun updateCampaign(campaign: Campaign): Result<Campaign>
    suspend fun addCoGm(campaignId: String, userId: String): Result<Unit>
    suspend fun removeCoGm(campaignId: String, userId: String): Result<Unit>
}
