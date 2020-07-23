/**
 *
 */
package com.github.react.mbbLibrary.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.github.react.mbbLibrary.constants.SPConsts;


public class SharedPreferencesUtil {

    private static SharedPreferences sharedPreferences;

    public static void init(Application application) {

        sharedPreferences = application.getSharedPreferences(SPConsts.SP_NAME, Context.MODE_PRIVATE);
    }


    public static int getSearchTextColor() {
        int color;
        color = sharedPreferences.getInt(SPConsts.SP_COLOR_SEARCH_TEXT, 0x80ff5722);
        return color;
    }


    public static void put(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value.getClass() == Boolean.class) {
            editor.putBoolean(key, (Boolean) value);
        }
        if (value.getClass() == String.class) {
            editor.putString(key, (String) value);
        }
        if (value.getClass() == Integer.class) {
            editor.putInt(key, ((Integer) value).intValue());
        }
        editor.commit();
    }


    public static void cleanStringValue(Context context, String... keys) {
        for (String key : keys) {
            SharedPreferences settings = context.getSharedPreferences(
                    SPConsts.SP_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if (settings.contains(key)) {
                editor.remove(key).commit();
            }
        }
    }


    public static void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SPConsts.SP_COLOR_SEARCH_TEXT);
        editor.commit();
    }


    public static float[] mapRGB(float[]rgb) {
        float[] result = new float[3];
        result[0] = rgb[0] / 255;
        result[1] = rgb[1] / 255;
        result[2] = rgb[2] / 255;
        return result;
    }

    public static float[] hextoRGB(String hex) {

        float[] rgbcolor = new float[3];
        rgbcolor[0] = Integer.valueOf( hex.substring( 1, 3 ), 16 );
        rgbcolor[1] = Integer.valueOf( hex.substring( 3, 5 ), 16 );
        rgbcolor[2] = Integer.valueOf( hex.substring( 5, 7 ), 16 );
        return mapRGB(rgbcolor);
    }

}
