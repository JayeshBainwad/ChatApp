package com.jsb.chatapp.feature_main.main_data.main_datasource

import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_main.main_util.Result

interface MainDataSource {

    suspend fun getUserById(uid: String): Result<User>
}