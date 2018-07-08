package vip.mystery0.crashhandler

class CrashConfig {
	var fileNamePrefix = "crash"
	var fileNameSuffix = "txt"
	var autoCleanTime: Long = 3 * 24 * 60 * 60 * 1000
	var isDebug = false
	var isAutoClean = false
	var dirName = "CrashHandler"
	var inSDCard = true

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

	fun setDebug(isDebug: Boolean): CrashConfig {
		this.isDebug = isDebug
		return this
	}

	fun setAutoClean(isAutoClean: Boolean): CrashConfig {
		this.isAutoClean = isAutoClean
		return this
	}

	fun setDirName(dirName: String): CrashConfig {
		this.dirName = dirName
		return this
	}

	fun setInSDCard(inSDCard: Boolean): CrashConfig {
		this.inSDCard = inSDCard
		return this
	}
}