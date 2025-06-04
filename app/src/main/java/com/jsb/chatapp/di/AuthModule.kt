package com.jsb.chatapp.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.data.datasource.AuthDataSource
import com.jsb.chatapp.feature_auth.data.datasource.AuthDataSourceImpl
import com.jsb.chatapp.feature_auth.data.repository.AuthRepository
import com.jsb.chatapp.feature_auth.data.repository.AuthRepositoryImpl
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.GoogleAuthUiClient
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
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
    }
}