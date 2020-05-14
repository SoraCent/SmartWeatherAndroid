package com.soracent.smartweather;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.soracent.smartweather.helper.WindDirection;
import com.squareup.picasso.Picasso;

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

public class MainActivity extends AppCompatActivity {

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

    SharedPreferences sharedPreferences;

    public static class Weather extends AsyncTask<String, Void,String> {

        @Override
        protected String doInBackground(String... address) {
            try {
                URL url = new URL(address[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int connectionResponseCode = connection.getResponseCode();

                Log.i("ADRESSE : ", address[0]);
                Log.i("RESPONSE: ", Integer.toString(connectionResponseCode));
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
                    Log.i("Content", content);
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        //mainCallFunction();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mainCallFunction();
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

            Log.i("API-KEYY: ", Api_key);

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
            content = weather.execute("https://api.openweathermap.org/data/2.5/weather?q=" + CityName + "&appid=" + Api_key + "&lang=De&units=metric").get();

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
                String main = "";
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
                    main = weatherPart.getString("main");
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

                Log.i("SunDifferenceHours: ", sunDifferenceHours.toString());
                Log.i("SunDifferenceMinutes: ", sunDifferenceMinutes.toString());

                // Convert Sunrise and Sunset and LastUpdate
                Date SunriseDate = new Date(SunriseMilli);
                Date SunsetDate = new Date(SunsetMilli);
                Date LastUpdateDate = new Date(lastUpdateMilli);
                DateFormat sunDateFormat = new SimpleDateFormat("HH:mm");
                DateFormat lastUpdateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String Sunrise = sunDateFormat.format(SunriseDate);
                String Sunset = sunDateFormat.format(SunsetDate);
                String lastUpdate = lastUpdateFormat.format(LastUpdateDate);

                //WeatherMain.setBackgroundResource(R.drawable.ash_background);
                WeatherTitle.setText("Wetter in " + cityName + ", " + countryCode);
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
                lastUpdateContent.setText("Zuletzt Aktualisiert: " + lastUpdate);

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
        }

        return super.onOptionsItemSelected(item);
    }

    private void noInternetAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton("Reload", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                        startActivity(getIntent());
                    }
                });
                dialogBuilder.setNegativeButton("Schliessen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        dialogBuilder.setMessage("Kein Internet\nOhne Internet können keine Daten geladen werden.\nBitte stelle eine Internetverbindung her.").setTitle("Kein Internet");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void apiKeyErrorAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton("Api-key Anpassen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });
        dialogBuilder.setMessage("Es gab einen Fehler mit Ihrem API-Key\nBitte den Eintrag anpassen.").setTitle("API-Key Fehler");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void noApiKeyAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton("Api-Key Eintragen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });
        dialogBuilder.setMessage("Sie haben noch keinen API-Key angegeben\nDer API-Key wird benötigt zum abfragen der Wetterdaten\nBitte einen API-Key Eintragen.").setTitle("API-Key nicht vorhanden");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void cityNotFoundAlert(String cityName) {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton("Neue Stadt eingeben", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent search = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(search);
            }
        });
        dialogBuilder.setMessage("Tut uns Leid\nDie Stadt " + cityName + " konnte nicht gefunden werden.\nBitte erneut eingeben").setTitle("Stadt nicht gefunden");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }
}
