pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CurrencyMobile"
include(":core")
// Модуль :app требует Android SDK. Без него можно собрать и протестировать только логику:
// SKIP_ANDROID=1 ./gradlew :core:test
if (System.getenv("SKIP_ANDROID") == null) {
    include(":app")
}
