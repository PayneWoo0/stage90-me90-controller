plugins {
    id("com.android.application")
}

android {
    namespace = "local.me90.controller"
    compileSdk = 35

    defaultConfig {
        applicationId = "local.me90.controller"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.2"
    }
}

android.applicationVariants.all {
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            "Stage90-v${android.defaultConfig.versionName}.apk"
    }
}
