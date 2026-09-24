# Keep Kotlin serialization-generated serializers and metadata used by the app.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
}

# Keep Firebase Functions / Firestore model access used by Kotlin code.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Google Sign-In / Credentials APIs.
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# Keep Billing classes referenced by the app and Play SDKs.
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**

# Keep coroutines basics.
-dontwarn kotlinx.coroutines.**

# Keep Compose generated/resource classes used reflectively or by generated accessors.
-keep class org.jetbrains.compose.resources.** { *; }
-keep class com.andrey.beautyplanner.generated.resources.** { *; }

# Keep app models that may be serialized/deserialized.
-keep class com.andrey.beautyplanner.** { *; }
-keep class com.andrey.beautyplanner.remote.** { *; }