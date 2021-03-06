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

package sviolet.turquoise.util.droid;

import android.content.Context;
import android.location.LocationManager;
import android.support.annotation.NonNull;

/**
 * 位置信息工具
 *
 * Created by S.Violet on 2017/8/19.
 */

public class LocationUtils {

    /**
     * 位置信息是否开启(包括高精度/GPS/网络)
     */
    public static boolean isEnabled(@NonNull Context context){
        LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null){
            return false;
        }
        return locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}
