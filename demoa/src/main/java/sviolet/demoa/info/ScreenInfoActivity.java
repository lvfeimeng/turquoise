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

package sviolet.demoa.info;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import sviolet.demoa.R;
import sviolet.demoa.common.DemoDescription;
import sviolet.turquoise.enhance.app.TActivity;
import sviolet.turquoise.enhance.app.annotation.inject.ResourceId;
import sviolet.turquoise.enhance.app.annotation.setting.ActivitySettings;
import sviolet.turquoise.util.common.CheckUtils;
import sviolet.turquoise.util.droid.MeasureUtils;
import sviolet.turquoise.utilx.tlogger.TLogger;

/**
 * 显示信息
 */
@DemoDescription(
        title = "Screen Info",
        type = "Info",
        info = "Screen info"
)

@ResourceId(R.layout.screen_info_main)
@ActivitySettings(
        statusBarColor = 0xFF30C0C0,
        navigationBarColor = 0xFF30C0C0
)
public class ScreenInfoActivity extends TActivity {

    @ResourceId(R.id.screen_info_main_screen_dimension)
    private EditText screenDimensionEditText;
    @ResourceId(R.id.screen_info_main_text)
    private TextView textView;

    @Override
    protected void onInitViews(Bundle savedInstanceState) {
        initEditText();
        refresh();
    }

    @Override
    protected void afterDestroy() {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initEditText(){
        screenDimensionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        });
    }

    /**
     * 刷新
     */
    private void refresh() {
        StringBuilder stringBuilder = new StringBuilder();
        printScreen(stringBuilder);
        textView.setText(stringBuilder.toString());
    }

    /**
     * 输出显示信息
     */
    private void printScreen(StringBuilder stringBuilder){
        int screenWidthPixels = MeasureUtils.getScreenWidth(this);
        int screenHeightPixels = MeasureUtils.getScreenHeight(this);

        stringBuilder.append("width(px): ");
        stringBuilder.append(screenWidthPixels);
        stringBuilder.append("\nheight(px): ");
        stringBuilder.append(screenHeightPixels);
        stringBuilder.append("\ndensity: ");
        stringBuilder.append(MeasureUtils.getScreenDensity(this));
        stringBuilder.append("\ndpi: ");
        stringBuilder.append(MeasureUtils.getScreenDensityDpi(this));

        String screenDimension = screenDimensionEditText.getText().toString();
        if (!CheckUtils.isEmpty(screenDimension)){
            try {
                float screenDimensionFloat = Float.parseFloat(screenDimension);
                float diagonalPixels = (float) Math.sqrt(screenWidthPixels * screenWidthPixels + screenHeightPixels * screenHeightPixels);
                float realDpi = diagonalPixels / screenDimensionFloat;
                stringBuilder.append("\nreal dpi: ");
                stringBuilder.append(realDpi);
                stringBuilder.append("\nreal dpc: ");
                stringBuilder.append(realDpi / 2.54f);
            } catch (Exception e){
                TLogger.get(this).e("error while parsing screen dimension", e);
                stringBuilder.append("\nreal dpi: error");
                stringBuilder.append("\nreal dpc: error");
            }
        }
    }

}
