import PublishConfig.configPublications
import PublishConfig.configPublish

plugins {
	id("com.android.library")
	id("kotlin-android")
	`maven-publish`
}

android {
	compileSdk = 30

	defaultConfig {
		minSdk = 14
		targetSdk = 30
		versionCode = 1
		versionName = "1.0"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.31")
}

publishing {
	configPublish(project)

	publications {
		create<MavenPublication>("maven") {
			configPublications()
		}
	}
}