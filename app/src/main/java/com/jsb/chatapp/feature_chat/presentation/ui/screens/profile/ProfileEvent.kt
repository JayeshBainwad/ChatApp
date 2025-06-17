package com.jsb.chatapp.feature_chat.presentation.ui.screens.profile

import android.net.Uri

sealed class ProfileEvent {
    data class OnUsernameChanged(val username: String) : ProfileEvent()
    data class OnNameChanged(val name: String) : ProfileEvent()
    data class OnPhoneChanged(val phone: String) : ProfileEvent()
    data class OnBioChanged(val bio: String) : ProfileEvent() // Add bio event
    data class OnAvatarSelected(val uri: Uri) : ProfileEvent()
    object UpdateProfile : ProfileEvent()
    object LoadProfile : ProfileEvent()
    object ResetSaveFlag : ProfileEvent()
}