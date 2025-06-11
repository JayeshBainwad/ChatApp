package com.jsb.chatapp.feature_chat.domain.usecase

data class ProfileUseCases(
    val getCurrentUser: GetCurrentUserUseCase,
    val updateUserProfile: UpdateUserProfileUseCase
)
