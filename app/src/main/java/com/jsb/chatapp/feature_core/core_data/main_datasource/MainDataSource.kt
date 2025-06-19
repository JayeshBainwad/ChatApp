package com.jsb.chatapp.feature_core.core_data.main_datasource

import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_core.main_util.Result

interface MainDataSource {

    suspend fun getUserById(uid: String): Result<User>
}