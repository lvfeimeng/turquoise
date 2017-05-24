/*
 * Copyright (C) 2015-2017 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/turquoise
 * Email: shepherdviolet@163.com
 */

package sviolet.turquoise.utilx.tlogger

import android.app.Application

/**
 * extensions for kotlin
 *
 * Created by S.Violet on 2017/5/23.
 */

fun Application?.tloggerSetGlobalLevel(level: Int?){
    TLoggerCenter.setGlobalLevel(level)
}

fun Application?.tloggerAddRules(rules: Map<String, Int>?){
    TLoggerCenter.addRules(rules)
}

fun Application?.tloggerResetRules(rules: Map<String, Int>?){
    TLoggerCenter.resetRules(rules)
}

fun Any?.loge(msg: String?) {
    TLoggerCenter.fetchLogger(this).e(msg)
}

fun Any?.loge(msg: String?, t: Throwable?) {
    TLoggerCenter.fetchLogger(this).e(msg, t)
}

fun Any?.loge(t: Throwable?) {
    TLoggerCenter.fetchLogger(this).e(t)
}

fun Any?.logw(msg: String?) {
    TLoggerCenter.fetchLogger(this).w(msg)
}

fun Any?.logw(msg: String?, t: Throwable?) {
    TLoggerCenter.fetchLogger(this).w(msg, t)
}

fun Any?.logw(t: Throwable?) {
    TLoggerCenter.fetchLogger(this).w(t)
}

fun Any?.logi(msg: String?) {
    TLoggerCenter.fetchLogger(this).i(msg)
}

fun Any?.logd(msg: String?) {
    TLoggerCenter.fetchLogger(this).d(msg)
}

fun Any?.getLogger() : TLogger {
    return TLoggerCenter.newLogger(this)
}
