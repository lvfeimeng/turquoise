/*
 * Copyright (C) 2015 S.Violet
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

package sviolet.turquoise.utils.bitmap.loader.handler;

import android.content.Context;

import sviolet.turquoise.utils.bitmap.loader.BitmapLoader;

/**
 * 普通异常处理器默认实现<p/>
 *
 * 实现了基本的错误信息打印, 无日志级别控制<br/>
 *
 * Created by S.Violet on 2015/11/4.
 */
public class DefaultCommonExceptionHandler implements CommonExceptionHandler {

    @Override
    public void onCommonException(Context context, BitmapLoader bitmapLoader, Throwable throwable) {
        //直接输出错误信息
        throwable.printStackTrace();
    }

    @Override
    public void onDestroy() {

    }
}