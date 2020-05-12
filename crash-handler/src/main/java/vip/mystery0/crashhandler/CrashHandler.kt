/*
 * Created by Mystery0 on 18-3-23 下午4:46.
 * Copyright (c) 2018. All Rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vip.mystery0.crashhandler

import android.os.Build
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {
	private const val TAG = "CrashHandler"
	private var config = CrashConfig()
	private var defaultCrashHandler: Thread.UncaughtExceptionHandler? = null
	private var autoCleanListener: (File.() -> Unit)? = null
	private var catchExceptionListener: CatchExceptionListener? = null

	fun setConfig(config: CrashConfig): CrashHandler {
		this.config = config
		return this
	}

	fun config(listener: CrashConfig.() -> Unit): CrashHandler {
		listener(config)
		return this
	}

	private fun clean() {
		if (!config.isAutoClean) {
			log("自动清理已关闭", Log.DEBUG)
			return
		}
		if (config.dir == null || !config.dir!!.exists()) {
			log("日志目录不存在", Log.DEBUG)
			return
		}
		val dir = config.dir!!
		log("clean: time: ${config.autoCleanTime}", Log.DEBUG)
		val calendar = Calendar.getInstance()
		if (dir.exists() || dir.mkdirs())
			for (file in dir.listFiles()!!) {
				if (file.name.startsWith(config.fileNamePrefix) && file.name.endsWith(config.fileNameSuffix)) {
					val modified = file.lastModified()
					log("clean: fileName: ${file.name}", Log.DEBUG)
					log("clean: fileLastModified: $modified", Log.DEBUG)
					if (calendar.timeInMillis - modified >= config.autoCleanTime) autoCleanListener?.invoke(file)
				}
			}
	}

	fun autoClean(autoCleanTime: Long = 3 * 24 * 60 * 60 * 1000, listener: File.() -> Unit): CrashHandler {
		config.setAutoCleanTime(autoCleanTime)
		config.setAutoClean(true)
		this.autoCleanListener = listener
		return this
	}

	fun autoClean(autoCleanTime: Long, listener: AutoCleanListener): CrashHandler {
		config.setAutoCleanTime(autoCleanTime)
		config.setAutoClean(true)
		autoCleanListener = {
			listener.clean(this)
		}
		return this
	}

	fun doOnCatch(listener: CatchException.() -> Unit): CrashHandler {
		catchExceptionListener = object : CatchExceptionListener {
			override fun catchException(catchException: CatchException) {
				listener(catchException)
			}
		}
		return this
	}

	fun doOnCatch(listener: CatchExceptionListener): CrashHandler {
		catchExceptionListener = listener
		return this
	}

	fun init() {
		defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			dumpExceptionToFile(throwable)
			throwable.printStackTrace()
			defaultCrashHandler?.uncaughtException(thread, throwable)
		}
		clean()
	}

	private fun dumpExceptionToFile(throwable: Throwable) {
		val dir = config.dir
		if (dir == null || (!dir.exists() && !dir.mkdirs())) {
			log("dumpExceptionToFile: 目录不存在! ", Log.ERROR)
			return
		}
		val time = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA).format(Calendar.getInstance().time)
		val file = File(dir, "${config.fileNamePrefix}$time.${config.fileNameSuffix}")
		try {
			val printWriter = PrintWriter(BufferedWriter(FileWriter(file)))
			//导出时间
			printWriter.println(time)
			//导出手机信息
			val version = config.version
			val androidVersion = "${Build.VERSION.RELEASE}_${Build.VERSION.SDK_INT}"
			val vendor = Build.MANUFACTURER
			val model = Build.MODEL
			printWriter.println("===================================")
			printWriter.println("应用版本: $version")
			printWriter.println("Android版本: $androidVersion")
			printWriter.println("厂商: $vendor")
			printWriter.println("型号: $model")
			printWriter.println("===================================")
			printWriter.println()
			throwable.printStackTrace(printWriter)
			printWriter.close()
			catchExceptionListener?.catchException(CatchException(version, androidVersion, vendor, model, throwable, file))
		} catch (e: Exception) {
			Log.e(TAG, "dumpExceptionToFile: 异常信息导出失败! ", e)
		}
	}

	private fun log(message: String, level: Int = Log.INFO) {
		if (level >= Log.WARN || config.isDebug)
			Log.println(level, TAG, message)
	}

	interface AutoCleanListener {
		fun clean(file: File)
	}

	interface CatchExceptionListener {
		fun catchException(catchException: CatchException)
	}
}