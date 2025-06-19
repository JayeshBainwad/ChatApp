package com.jsb.chatapp.main_data.main_repository

import com.jsb.chatapp.main_data.main_datasource.MainDataSource
import com.jsb.chatapp.main_domain.main_model.User
import com.jsb.chatapp.util.Result
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val mainDataSource: MainDataSource
): MainRepository {
    override suspend fun getUserById(uid: String): Result<User> {
        return mainDataSource.getUserById(uid)
    }
}