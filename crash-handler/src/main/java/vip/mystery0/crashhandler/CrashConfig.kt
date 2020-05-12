package vip.mystery0.crashhandler

import java.io.File
import java.util.concurrent.TimeUnit

class CrashConfig {
	var fileNamePrefix = "crash"
		private set
	var fileNameSuffix = "txt"
		private set
	var autoCleanTime: Long = 3 * 24 * 60 * 60 * 1000
		private set
	var isDebug = false
		private set
	var isAutoClean = false
		private set
	var dir: File? = null
		private set
	var version: String = "not defined version"
		private set

	fun setFileNamePrefix(fileNamePrefix: String): CrashConfig {
		this.fileNamePrefix = fileNamePrefix
		return this
	}

	fun setFileNameSuffix(fileNameSuffix: String): CrashConfig {
		this.fileNameSuffix = fileNameSuffix
		return this
	}

	fun setAutoCleanTime(autoCleanTime: Long): CrashConfig {
		this.autoCleanTime = autoCleanTime
		return this
	}

	fun setAutoCleanTime(autoCleanTime: Long, timeUnit: TimeUnit): CrashConfig {
		this.autoCleanTime = timeUnit.toMillis(autoCleanTime)
		return this
	}

	fun setDebug(isDebug: Boolean): CrashConfig {
		this.isDebug = isDebug
		return this
	}

	fun setAutoClean(isAutoClean: Boolean): CrashConfig {
		this.isAutoClean = isAutoClean
		return this
	}

	fun setDir(dir: File): CrashConfig {
		if (!dir.exists()) dir.mkdirs()
		this.dir = dir
		return this
	}

	fun setVersion(version: String): CrashConfig {
		this.version = version
		return this
	}
}