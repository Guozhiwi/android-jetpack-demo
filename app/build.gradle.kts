
plugins {
	alias(libs.plugins.com.android.application)
	alias(libs.plugins.org.jetbrains.kotlin.android)
	//使用ksp替代kapt 第2步，在模块的build.gradle中添加plugins
	alias(libs.plugins.ksp)
	id("kotlin-parcelize")
}

android {
	namespace = "org.zhiwei.jetpack"
	compileSdk = 34

	defaultConfig {
		applicationId = "org.zhiwei.jetpack"
		minSdk = 24
		targetSdk = 34
		versionCode = 200
		versionName = "2.0.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		viewBinding = true
		//因为dataBinding的module中开启了dataBinding，主module也要开启，否则报错Failed resolution of: Landroidx/databinding/DataBinderMapperImpl;
		dataBinding = true
	}

	configurations {
		// room 2.5.1中compiler依赖了老旧的com.intellij的annotations 12的包，与 org.jetbrains.annotations 13的包冲突了
		implementation.configure { exclude(group = "com.intellij", module = "annotations") }
	}
}

dependencies {

	// android official libs version
	implementation(libs.activity.ktx)
	implementation(libs.appcompat)
	implementation(libs.constraintlayout)
	implementation(libs.core.ktx)
	implementation(libs.fragment.ktx)
	implementation(libs.material)
	implementation(libs.recyclerview)
	implementation(libs.swiperefreshlayout)


	// androidx 或 google 相关库
	implementation(libs.androidx.biometric)
	implementation(libs.androidx.browser)
	implementation(libs.androidx.camera.camera2)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.androidx.dataStore.core)
	implementation(libs.androidx.dataStore.preferences)
	implementation(libs.androidx.drawerlayout)
	implementation(libs.androidx.emoji2)
	implementation(libs.androidx.exifinterface)
	implementation(libs.androidx.metrics)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.profileinstaller)
	implementation(libs.androidx.startup)
	implementation(libs.androidx.tracing)
	implementation(libs.androidx.webkit)

	// Jetpack Components libs version
	implementation(libs.lifecycle.livedata.ktx)
	implementation(libs.lifecycle.viewmodel.ktx)
	implementation(libs.navigation.fragment.ktx)
	implementation(libs.navigation.ui.ktx)
	implementation(libs.room.compiler)
	implementation(libs.room.runtime)
	implementation(libs.room.ktx)
	implementation(libs.paging.runtime.ktx)
	implementation(libs.work.runtime.ktx)

	// jetbrains official libs version
	implementation(libs.kotlin.stdlib)
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.kotlinx.datetime)
	implementation(libs.kotlinx.serialization.json)

	// 常用知名开源库
	implementation(libs.blankj.utils)
	implementation(libs.coil.kt)
	implementation(libs.coil.kt.svg)
	implementation(libs.gson)
	implementation(libs.koin)
	implementation(libs.okhttp.logging)
	implementation(libs.retrofit.core)
	implementation(libs.retrofit.kotlin.serialization)
	implementation(libs.retrofit2.converter.gson)

	//test libs version
	testImplementation(libs.turbine)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.test.ext.junit)
	androidTestImplementation(libs.espresso.core)

	//依赖其他模块
	implementation(project(":jetpack:databinding"))
	implementation(project(":jetpack:lifecycle"))
	implementation(project(":jetpack:livedata"))
	implementation(project(":jetpack:navigation"))
	implementation(project(":jetpack:paging"))
	implementation(project(":jetpack:room"))
	implementation(project(":jetpack:viewmodel"))
	implementation(project(":jetpack:work"))
	implementation(project(":kotlin"))
	implementation(project(":mvi"))
	implementation(project(":mvvm"))


}