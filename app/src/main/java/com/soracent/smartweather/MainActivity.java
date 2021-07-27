package com.soracent.smartweather;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.soracent.smartweather.helper.CustomBackground;
import com.soracent.smartweather.helper.WindDirection;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (mAccel > 24 && mAccelCurrent > 30) {
                mainCallFunction();
                Context context = getApplicationContext();
                CharSequence text = getString(R.string.reload_toaster);
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(500);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    ScrollView WeatherMainScrollView;
    ConstraintLayout WeatherMain;
    TextView WeatherTitle;
    TextView TemperatureContent;
    ImageView WeatherIcon;
    TextView WeatherDescription;
    TextView minTemp;
    TextView maxTemp;
    TextView feelTemp;
    TextView humidityContent;
    TextView pressureContent;
    TextView sunriseContent;
    TextView sunsetContent;
    TextView daylightContent;
    TextView windSpeedContent;
    TextView windDegContent;
    TextView lastUpdateContent;

    public static class Weather extends AsyncTask<String, Void,String> {

        @Override
        protected String doInBackground(String... address) {
            try {
                URL url = new URL(address[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int connectionResponseCode = connection.getResponseCode();

                if (connectionResponseCode == 200) {
                    // Alles Oke
                    connection.connect();

                    InputStream is = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);

                    int data = isr.read();
                    String content = "";
                    char ch;
                    while (data != -1) {
                        ch = (char) data;
                        content = content + ch;
                        data = isr.read();
                    }
                    return content;
                }
                else {
                    return Integer.toString(connectionResponseCode);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    protected void onStart() {
        super.onStart();

        //mainCallFunction();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mainCallFunction();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    public void mainCallFunction() {
        boolean conncted = CheckNetwork();

        if (conncted) {
            String CityName = "";

            Intent intent = getIntent();
            CityName = intent.getStringExtra("cityName");

            if(CityName == null || CityName.isEmpty()) {
                CityName = "Bern";
            }


            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String Api_key = sharedPref.getString("owmKey", "");

            WeatherCall(CityName, Api_key);
        } else {

            WeatherMain = findViewById(R.id.WeatherMain);
            WeatherMain.setVisibility(View.GONE);
            noInternetAlert();
        }
    }

    // Checkt Internet verbindung
    public boolean CheckNetwork() {
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        boolean conncted = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();

        return conncted;
    }

    // Json Wetter Call
    public void WeatherCall(String CityName, String Api_key) {
        WeatherMainScrollView = findViewById(R.id.WeatherMainScrollView);
        WeatherMain = findViewById(R.id.WeatherMain);
        WeatherTitle = findViewById(R.id.WeatherTitle);
        TemperatureContent = findViewById(R.id.TemperatureContent);
        WeatherIcon = findViewById(R.id.WeatherIcon);
        WeatherDescription = findViewById(R.id.WeatherDescription);
        minTemp = findViewById(R.id.minTemp);
        maxTemp = findViewById(R.id.maxTemp);
        feelTemp = findViewById(R.id.feelTemp);
        humidityContent = findViewById(R.id.humidityContent);
        pressureContent = findViewById(R.id.pressureContent);
        sunriseContent = findViewById(R.id.sunriseContent);
        sunsetContent = findViewById(R.id.sunsetContent);
        daylightContent = findViewById(R.id.daylightContent);
        windSpeedContent = findViewById(R.id.WindSpeedContent);
        windDegContent = findViewById(R.id.WindDegContent);
        lastUpdateContent = findViewById(R.id.lastUpdateContent);

        String content;
        int responseCode;
        Weather weather = new Weather();
        try {
            String api_lang;
            if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase("deutsch")) {
                api_lang = "de";
            } else {
                api_lang = "en";
            }
            content = weather.execute("https://api.openweathermap.org/data/2.5/weather?q=" + CityName + "&appid=" + Api_key + "&lang=" + api_lang + "&units=metric").get();

            // Falls der Content ein Code ist parse es zum Int aber wenn Content der Call Content ist parse nichts
            try {
                responseCode = Integer.parseInt(content);
            } catch (Exception e) {
                responseCode = 0;
            }

            if (responseCode == 400) {
                // Keine Stadt eingetragen Was nicht möglich ist
            }
            else if (responseCode == 401 && Api_key.isEmpty()) {
                // API Key nicht vorhanden
                WeatherMain.setVisibility(View.GONE);
                noApiKeyAlert();
            }
            else if (responseCode == 401 && !Api_key.isEmpty()) {
                // API Key Fehler
                WeatherMain.setVisibility(View.GONE);
                apiKeyErrorAlert();
            }
            else if (responseCode == 404) {
                // Stadt nicht gefunden
                WeatherMain.setVisibility(View.GONE);
                cityNotFoundAlert(CityName);
            } else {
                WeatherMain.setVisibility(View.VISIBLE);
                JSONObject jsonObject = new JSONObject(content);
                // Alles OK
                String WeatherMainInfo = "";
                String WeatherId = "";
                String description = "";
                String tempMain = "";
                String cityName = "";
                String IconId = "";
                String minTemperature = "";
                String maxTemperature = "";
                String feelTemperature = "";
                String humidity = "";
                String pressure = "";
                String windSpeed = "";
                String windDeg = "";
                String windDir = "";
                String countryCode = "";
                String SunriseTimestamp = "";
                String SunsetTimestamp = "";
                String lastUpdateTimestamp = "";

                // Get the data from Weather Array in JSON
                String weatherData = jsonObject.getString("weather");
                JSONArray weatherArray = new JSONArray(weatherData);

                for (int i = 0; i < weatherArray.length(); i++) {
                    JSONObject weatherPart = weatherArray.getJSONObject(i);
                    WeatherId = weatherPart.getString("id");
                    WeatherMainInfo = weatherPart.getString("main");
                    description = weatherPart.getString("description");
                    IconId = weatherPart.getString("icon");
                }

                // Get the data from Main in JSON, Like Temp.
                JSONObject mainWeatherData = jsonObject.getJSONObject("main");
                tempMain = mainWeatherData.getString("temp");
                minTemperature = mainWeatherData.getString("temp_min");
                maxTemperature = mainWeatherData.getString("temp_max");
                feelTemperature = mainWeatherData.getString("feels_like");
                pressure = mainWeatherData.getString("pressure");
                humidity = mainWeatherData.getString("humidity");

                // Round Temperature to 1 Digit
                DecimalFormat df = new DecimalFormat("#.#");
                Double tempMainD = Double.parseDouble(tempMain);
                Double minTempD = Double.parseDouble(minTemperature);
                Double maxTempD = Double.parseDouble(maxTemperature);
                Double feelTempD = Double.parseDouble(feelTemperature);

                tempMain = df.format(tempMainD);
                minTemperature = df.format(minTempD);
                maxTemperature = df.format(maxTempD);
                feelTemperature = df.format(feelTempD);

                // Get the data from Sys in JSON, like Countrycode.
                JSONObject sysWeatherData = jsonObject.getJSONObject("sys");
                countryCode = sysWeatherData.getString("country");
                SunriseTimestamp = sysWeatherData.getString("sunrise");
                SunsetTimestamp = sysWeatherData.getString("sunset");
                Long SunriseMilli = Long.parseLong(SunriseTimestamp);
                Long SunsetMilli = Long.parseLong(SunsetTimestamp);
                SunriseMilli = SunriseMilli*1000;
                SunsetMilli = SunsetMilli*1000;

                // Get the date from Wind in JSON, like Speed.
                JSONObject windWeatherData = jsonObject.getJSONObject("wind");
                windSpeed = windWeatherData.getString("speed");
                // Problem mit der API ist das wenn die Wind Richtung 0 ist, ist diese nicht gesetz!
                try {
                    windDeg = windWeatherData.getString("deg");
                } catch (Exception e) {
                    windDeg = "0";
                }

                // Get Wind Direction by WindDeg in Degrees, Like N or W for North and West, in helper folder
                windDir = WindDirection.getWindDirection(windDeg);

                // Get City Name
                cityName = jsonObject.getString("name");
                String cityTitle = getString(R.string.weather_title) + " " + cityName + ", " + countryCode;
                setCountryIcon(countryCode);

                // Set WeatherIcon
                String ImgUrl = "https://openweathermap.org/img/wn/" + IconId + "@2x.png";
                Picasso.get().load(ImgUrl).into(WeatherIcon);

                // Get Last Update
                lastUpdateTimestamp = jsonObject.getString("dt");
                Long lastUpdateMilli = Long.parseLong(lastUpdateTimestamp);
                lastUpdateMilli = lastUpdateMilli*1000;

                // Calculate Duration of Sunlight
                Long sunDifferenceMilli = Math.abs((SunriseMilli - SunsetMilli) / 1000);
                Long sunDifferenceHours = Math.abs(sunDifferenceMilli / 3600);
                sunDifferenceMilli = sunDifferenceMilli % 3600;
                Long sunDifferenceMinutes = Math.abs(sunDifferenceMilli / 60);

                // Convert Sunrise and Sunset and LastUpdate
                Date SunriseDate = new Date(SunriseMilli);
                Date SunsetDate = new Date(SunsetMilli);
                Date LastUpdateDate = new Date(lastUpdateMilli);
                DateFormat sunDateFormat = new SimpleDateFormat("HH:mm");
                DateFormat lastUpdateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String Sunrise = sunDateFormat.format(SunriseDate);
                String Sunset = sunDateFormat.format(SunsetDate);
                String lastUpdate = lastUpdateFormat.format(LastUpdateDate);

                // set Last updated Text
                String lastUpdateText = getString(R.string.last_updated_content) + " " + lastUpdate;

                // Custom Background
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Boolean custom_img = sharedPref.getBoolean("customImg", true);
                if (!WeatherMainInfo.isEmpty() && WeatherMainInfo != null && custom_img == true) {
                    CustomBackground.setCustomBackground(WeatherMainInfo, WeatherId, WeatherMainScrollView, WeatherMain);
                } else {
                    WeatherMainScrollView.setBackgroundResource(0);
                }

                WeatherTitle.setText(cityTitle);
                TemperatureContent.setText(tempMain + "°C");
                WeatherDescription.setText(description);
                minTemp.setText(minTemperature + "°C");
                maxTemp.setText(maxTemperature + "°C");
                feelTemp.setText(feelTemperature + "°C");
                humidityContent.setText(humidity + "%");
                pressureContent.setText(pressure + " hPa");
                sunriseContent.setText(Sunrise + " Uhr");
                sunsetContent.setText(Sunset + " Uhr");
                daylightContent.setText(sunDifferenceHours + " Std. " + sunDifferenceMinutes + " Min.");
                windSpeedContent.setText(windSpeed + " m/s");
                windDegContent.setText(windDir + " ("+ windDeg + "°)");
                lastUpdateContent.setText(lastUpdateText);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Custom button in Actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu((menu));
    }

    // Handle button Activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.SettingsButton) {
            Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settings);
        }

        if (id == R.id.SearchButton) {
            Intent search = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(search);
        }

        if (id == R.id.RefreshButton) {
            mainCallFunction();
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.reload_toaster);
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setCountryIcon(String countryCode) {
        String countryIconUrl = "https://www.countryflags.io/" + countryCode + "/flat/64.png";

        Picasso.get()
                .load(countryIconUrl)
                .into(new Target() {

                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
                        Drawable countryIcon = new BitmapDrawable(getResources(), resizedBitmap);
                        WeatherTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, countryIcon, null);
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        WeatherTitle.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    }
                });
    }

    private void noInternetAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.nointernet_button_p, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                        startActivity(getIntent());
                    }
                });
                dialogBuilder.setNegativeButton(R.string.nointernet_button_n, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        dialogBuilder.setMessage(R.string.nointernet_content).setTitle(R.string.nointernet_title);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void apiKeyErrorAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.apikeyerror_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });
        dialogBuilder.setMessage(R.string.apikeyerror_content).setTitle(R.string.apikeyerror_title);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void noApiKeyAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.noapikey_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });
        dialogBuilder.setMessage(R.string.noapikey_content).setTitle(R.string.noapikey_title);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void cityNotFoundAlert(String cityName) {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.citynotfound_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent search = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(search);
            }
        });
        String citynotfound = getResources().getString(R.string.citynotfound_content, cityName);
        dialogBuilder.setMessage(citynotfound).setTitle(R.string.citynotfound_title);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }
}
