package com.soracent.smartweather;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class SearchActivity extends AppCompatActivity {

    EditText CityInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        CityInput = findViewById(R.id.CityInput);
        CityInput.setOnKeyListener((new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                    onSearchBtnClick(view);
                    return true;
                }
                return false;
            }
        }));
    }

    public void onSearchBtnClick(View view) {
        CityInput = findViewById(R.id.CityInput);
        String cityName = CityInput.getText().toString();

        if (cityName == null || cityName.isEmpty()) {
            noCityEnteredAlert();
        } else {
            Intent searchIntent = new Intent(SearchActivity.this, MainActivity.class);
            searchIntent.putExtra("cityName", cityName);
            SearchActivity.this.startActivity(searchIntent);
        }
    }

    public void getLocation() {
    }
    private void noCityEnteredAlert() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        dialogBuilder.setMessage("Keine Stadt angegeben\nBitte Stadt eingeben").setTitle("Keine Stadt");
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    // Custom button in Actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_menu, menu);
        return super.onCreateOptionsMenu((menu));
    }

    // Handle button Activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.SettingsButton) {
            Intent settings = new Intent(SearchActivity.this, SettingsActivity.class);
            startActivity(settings);
        }

        if (id == R.id.LocationButton) {
            getLocation();
        }

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}
