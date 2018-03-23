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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
	companion object {
		private const val TAG = "CrashHandler"
		@SuppressLint("StaticFieldLeak")
		private lateinit var context: Context

		@JvmStatic
		fun getInstance(context: Context): CrashHandler {
			Companion.context = context.applicationContext
			return CrashHandler()
		}
	}

	interface AutoCleanListener {
		fun cleanDone()
		fun cleanError(ex: Exception)
	}

	interface CatchExceptionListener {
		fun catchException(catchException: CatchException)
	}

	private var fileNamePrefix = "crash"
	private var fileNameSuffix = "txt"
	private var autoCleanTime = 3 * 24 * 60 * 60 * 1000
	private var isDebug = false
	private var isAutoClean = false
	private var dir = File(context.cacheDir, "crashHandler")
	private lateinit var defaultCrashHandler: Thread.UncaughtExceptionHandler
	private var catchExceptionListener: CatchExceptionListener? = null

	fun debug(): CrashHandler {
		isDebug = true
		return this
	}

	fun autoClean(dayTime: Int): CrashHandler {
		if (dayTime <= 0) throw Exception("time can not less than zero! ")
		autoCleanTime = dayTime * 24 * 60 * 60 * 1000
		return this
	}

	fun setDir(dir: File): CrashHandler {
		if (!dir.exists()) dir.mkdirs()
		this.dir = dir
		return this
	}

	fun setDir(dirPath: String): CrashHandler {
		val file = File(dirPath)
		if (!file.exists()) file.mkdirs()
		this.dir = file
		return this
	}

	fun setDir(listener: () -> File): CrashHandler {
		val file = listener()
		if (!file.exists()) file.mkdirs()
		this.dir = file
		return this
	}

	fun setPrefix(prefix: String): CrashHandler {
		if (TextUtils.isEmpty(prefix)) Log.w(TAG, "file prefix is null")
		fileNamePrefix = prefix
		return this
	}

	fun setSuffix(suffix: String): CrashHandler {
		if (TextUtils.isEmpty(suffix)) Log.w(TAG, "file suffix is null")
		fileNameSuffix = suffix
		return this
	}

	fun clean(autoCleanListener: AutoCleanListener) {
		clean({ autoCleanListener.cleanDone() }, { ex -> autoCleanListener.cleanError(ex) })
	}

	fun clean(listener: () -> Unit, errorListener: (Exception) -> Unit) {
		if (!isAutoClean) {
			errorListener(RuntimeException("auto clean is disabled"))
		}
		if (isDebug) Log.d(TAG, "clean: time: $autoCleanTime")
		val calendar = Calendar.getInstance()
		if (dir.exists() || dir.mkdirs()) for (file in dir.listFiles()) {
			if (file.name.startsWith(fileNamePrefix) && file.name.endsWith(fileNameSuffix)) {
				val modified = file.lastModified()
				if (isDebug) {
					Log.d(TAG, "clean: fileName: ${file.name}")
					Log.d(TAG, "clean: fileLastModified: $modified")
				}
				if (calendar.timeInMillis - modified >= autoCleanTime) file.delete()
			}
		}
		listener()
	}

	fun doOnCatch(listener: (CatchException) -> Unit): CrashHandler {
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
		Thread.setDefaultUncaughtExceptionHandler(this)
	}

	override fun uncaughtException(t: Thread?, e: Throwable) {
		dumpExceptionToFile(e)
		e.printStackTrace()
		defaultCrashHandler.uncaughtException(t, e)
	}

	private fun dumpExceptionToFile(ex: Throwable) {
		if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) Log.w(TAG, "dumpExceptionToFile: sdcard is not mounted! ")
		if (!dir.exists() && !dir.mkdirs()) {
			Log.w(TAG, "dumpExceptionToFile: Dir is not exist! ")
			return
		}

		val time = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA).format(Calendar.getInstance().time)
		val file = File(dir, fileNamePrefix + time + '.' + fileNameSuffix)
		try {
			val printWriter = PrintWriter(BufferedWriter(FileWriter(file)))
			//导出时间
			printWriter.println(time)

			//导出手机信息
			val packageManager = context.packageManager
			val packageInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
			val version = "${packageInfo.versionName}_${packageInfo.versionCode}"
			val androidVersion = "${Build.VERSION.RELEASE}_${Build.VERSION.SDK_INT}"
			val vendor = Build.MANUFACTURER
			val model = Build.MODEL
			printWriter.println("Version: $version")
			printWriter.println("Android Version: $androidVersion")
			printWriter.println("Vendor: $vendor")
			printWriter.println("Model: $model")
			printWriter.println()
			ex.printStackTrace(printWriter)
			printWriter.close()
			catchExceptionListener?.catchException(CatchException(version, androidVersion, vendor, model, ex, file))
		} catch (e: Exception) {
			Log.wtf(TAG, "dump exception failed! ", e)
		}
	}
}