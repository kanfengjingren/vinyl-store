import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        // 国内镜像优先
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        maven { setUrl("https://repo.huaweicloud.com/repository/maven/") }

        // 官方源 + 包过滤，只让Android相关包走google仓库
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像优先
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        maven { setUrl("https://maven.aliyun.com/repository/public/") }

        google()
        mavenCentral()
    }
}

rootProject.name = "vinyl-store"
include(":app")