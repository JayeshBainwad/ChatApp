package com.jsb.chatapp.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.data.auth_datasource.AuthDataSource
import com.jsb.chatapp.feature_auth.data.auth_datasource.AuthDataSourceImpl
import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepository
import com.jsb.chatapp.feature_auth.data.auth_repository.AuthRepositoryImpl
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.GoogleAuthUiClient
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import com.jsb.chatapp.feature_chat.data.chat_datasource.ChatDatasource
import com.jsb.chatapp.feature_chat.data.chat_datasource.ChatDatasourceImpl
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepositoryImpl
import com.jsb.chatapp.feature_chat.domain.usecase.ChatUseCases
import com.jsb.chatapp.feature_chat.domain.usecase.GetChatsForUserUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.GetCurrentUserUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.IsUsernameAvailableUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.ListenForMessagesUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.ProfileUseCases
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateFcmTokenUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateUserProfileUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthDataSource(
        authDataSourceImpl: AuthDataSourceImpl
    ): AuthDataSource

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindChatDataSource(
        chatDatasourceImpl: ChatDatasourceImpl
    ): ChatDatasource

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }

        @Provides
        @Singleton
        fun provideSignInClient(@ApplicationContext context: Context): SignInClient {
            return Identity.getSignInClient(context)
        }

        @Provides
        @Singleton
        fun provideGoogleAuthUiClient(
            @ApplicationContext context: Context
        ): GoogleAuthUiClient {
            return GoogleAuthUiClient(
                context = context,
                oneTapClient = Identity.getSignInClient(context)
            )
        }

        @Provides
        @Singleton
        fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
            return UserPreferences(context)
        }

        @Provides
        @Singleton
        fun provideChatUseCases(
            getChatsForUser: GetChatsForUserUseCase,
            listenForMessages: ListenForMessagesUseCase
        ): ChatUseCases {
            return ChatUseCases(
                getChatsForUser = getChatsForUser,
                listenForMessages = listenForMessages
            )
        }

        @Provides
        @Singleton
        fun provideProfileUseCases(repository: AuthRepository): ProfileUseCases {
            return ProfileUseCases(
                getCurrentUser = GetCurrentUserUseCase(repository),
                updateUserProfile = UpdateUserProfileUseCase(repository)
            )
        }

        @Provides
        @Singleton
        fun provideIsUsernameAvailableUseCase(repository: AuthRepository): IsUsernameAvailableUseCase {
            return IsUsernameAvailableUseCase(repository)
        }

        @Provides
        @Singleton
        fun provideUpdateFcmTokenUseCase(repository: ChatRepository): UpdateFcmTokenUseCase =
            UpdateFcmTokenUseCase(repository)

    }
}