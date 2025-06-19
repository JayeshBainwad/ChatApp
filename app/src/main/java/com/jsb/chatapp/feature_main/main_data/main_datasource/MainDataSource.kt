package com.jsb.chatapp.main_data.main_datasource

import com.jsb.chatapp.main_domain.main_model.User
import com.jsb.chatapp.util.Result

interface MainDataSource {

    suspend fun getUserById(uid: String): Result<User>
}