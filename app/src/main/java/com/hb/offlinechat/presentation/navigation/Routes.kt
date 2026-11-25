package com.hb.offlinechat.presentation.navigation

/**
 * Navigation routes for HB Offline Chat.
 * Each route is a stable string used by Navigation Compose.
 */
object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val ProfileSetup = "profile_setup"
    const val Home = "home"
    const val NearbyDevices = "nearby_devices"
    const val Connection = "connection"
    const val ChatOneToOne = "chat_one_to_one"
    const val ChatGroup = "chat_group"
    const val GroupList = "group_list"
    const val FileSharing = "file_sharing"
    const val VoiceMessages = "voice_messages"
    const val VoiceCall = "voice_call"
    const val Settings = "settings"
    const val PrivacySettings = "privacy_settings"
    const val About = "about"

    /** Argument keys */
    const val ArgDeviceId = "deviceId"
    const val ArgDeviceName = "deviceName"
    const val ArgConversationId = "conversationId"
    const val ArgGroupId = "groupId"
}
