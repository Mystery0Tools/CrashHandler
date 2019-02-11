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
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("StaticFieldLeak")
object CrashHandler {
	private const val TAG = "CrashHandler"
	private lateinit var context: Context
	private var config = CrashConfig()
	private lateinit var defaultCrashHandler: Thread.UncaughtExceptionHandler
	private var autoCleanListener: AutoCleanListener? = null
	private var catchExceptionListener: CatchExceptionListener? = null

	private fun getContext(): Context {
		if (::context.isInitialized)
			return context
		throw Exception("this function need context, please call “CrashHandler.initWithContext(Context)” first.")
	}
	private fun getDir(): File {
		if (config.dir == null)
			config.dir = getContext().externalCacheDir
		if (!config.dir!!.exists())
			config.dir!!.mkdirs()
		return config.dir!!
	}

	fun setConfig(config: CrashConfig): CrashHandler {
		this.config = config
		return this
	}

	fun config(listener: (CrashConfig) -> Unit): CrashHandler {
		listener.invoke(config)
		return this
	}

	private fun clean() {
		try {
			if (!config.isAutoClean) {
				if (config.isDebug) Log.d(TAG, "auto clean is disabled")
				return
			}
			if (config.dir == null || !config.dir!!.exists()) {
				Log.w(TAG, "crash dir not exist")
				return
			}
			val dir = getDir()
			if (config.isDebug) Log.d(TAG, "clean: time: ${config.autoCleanTime}")
			val calendar = Calendar.getInstance()
			if (dir.exists() || dir.mkdirs())
				for (file in dir.listFiles()) {
					if (file.name.startsWith(config.fileNamePrefix) && file.name.endsWith(config.fileNameSuffix)) {
						val modified = file.lastModified()
						if (config.isDebug) {
							Log.d(TAG, "clean: fileName: ${file.name}")
							Log.d(TAG, "clean: fileLastModified: $modified")
						}
						if (calendar.timeInMillis - modified >= config.autoCleanTime) file.delete()
					}
				}
			autoCleanListener?.cleanDone()
		} catch (e: Exception) {
			autoCleanListener?.cleanError(e)
		}
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

	fun autoClean(listener: AutoCleanListener): CrashHandler {
		autoCleanListener = listener
		return this
	}

	fun initWithContext(context: Context) {
		this.context = context.applicationContext
		defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			dumpExceptionToFile(throwable)
			throwable.printStackTrace()
			defaultCrashHandler.uncaughtException(thread, throwable)
		}
		clean()
	}

	private fun dumpExceptionToFile(throwable: Throwable) {
		if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED)
			Log.e(TAG, "dumpExceptionToFile: sdcard is not mounted! ")
		val dir = getDir()
		if (!dir.exists() && !dir.mkdirs()) {
			Log.e(TAG, "dumpExceptionToFile: Dir is not exist! ")
			return
		}
		val time = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA).format(Calendar.getInstance().time)
		val file = File(dir, "${config.fileNamePrefix}$time.${config.fileNameSuffix}")
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
			throwable.printStackTrace(printWriter)
			printWriter.close()
			catchExceptionListener?.catchException(CatchException(version, androidVersion, vendor, model, throwable, file))
		} catch (e: Exception) {
			Log.e(TAG, "dump exception failed! ", e)
		}
	}

	interface AutoCleanListener {
		fun cleanDone()
		fun cleanError(ex: Exception)
	}

	interface CatchExceptionListener {
		fun catchException(catchException: CatchException)
	}
}