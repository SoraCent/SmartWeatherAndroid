package com.soracent.smartweather.helper;

import android.content.Context;
import android.graphics.Color;
import android.widget.ScrollView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class CustomBackground {

    public static void setCustomBackground(String WeatherMainInfo, String weatherId, ScrollView WeatherMainScrollView, ConstraintLayout WeatherMain) {
        int resID;

        WeatherMainInfo = WeatherMainInfo.toLowerCase();
        Context context = WeatherMainScrollView.getContext();

        if (WeatherMainInfo.equals("clouds")) {
            resID = context.getResources().getIdentifier(WeatherMainInfo + weatherId + "_background", "drawable", context.getPackageName());
            WeatherMainScrollView.setBackgroundResource(resID);
        } else {
            resID = context.getResources().getIdentifier(WeatherMainInfo + "_background", "drawable", context.getPackageName());
            WeatherMainScrollView.setBackgroundResource(resID);
        }

        WeatherMain.setBackgroundColor(Color.parseColor("#99FFFFFF"));
    }

}
