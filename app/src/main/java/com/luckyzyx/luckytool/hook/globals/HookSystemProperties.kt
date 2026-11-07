package com.luckyzyx.luckytool.hook.globals

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class HookSystemProperties(private val props: Map<String, Any>) : YukiBaseHooker() {
    override fun onHook() {
        if (props.isEmpty()) return
        //Source SystemProperties
        arrayOf(
            "android.os.SystemProperties",
            "com.oplus.wrapper.os.SystemProperties"
        ).forEach {
            it.toClassOrNull()?.resolve()?.apply {
                method { name = "get"; returnType = String::class }.hookAll {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Boolean -> result = value.toString()
                            is String -> result = value
                            is Int -> result = value.toString()
                            is Long -> result = value.toString()
                            else -> result = value.toString()
                        }
                    }
                }
                method { name = "getBoolean"; returnType = Boolean::class }.hookAll {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            "1" -> result = true
                            "0" -> result = false
                            "true" -> result = true
                            "false" -> result = false
                            is Boolean -> result = value
                            is Int -> result = value != 0
                            is Long -> result = value != 0L
                        }
                    }
                }
                method { name = "getInt"; returnType = Int::class }.hookAll {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Long -> result = value.toInt()
                            is Int -> result = value
                            is String -> result = value.toIntOrNull() ?: return@before
                            is Boolean -> result = if (value) 1 else 0
                        }
                    }
                }
                method { name = "getLong"; returnType = Long::class }.hookAll {
                    before {
                        val key = firstArg().get<String>()
                        if (key.isNullOrBlank()) return@before
                        when (val value = props[key]) {
                            null -> return@before
                            is Int -> result = value.toLong()
                            is Long -> result = value
                            is String -> result = value.toLongOrNull() ?: return@before
                            is Boolean -> result = if (value) 1L else 0L
                        }
                    }
                }
            }
        }
    }
}