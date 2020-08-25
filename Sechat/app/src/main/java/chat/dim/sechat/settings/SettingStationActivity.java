package chat.dim.sechat.settings;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import chat.dim.sechat.R;

public class SettingStationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_station_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, SettingStationFragment.newInstance())
                    .commitNow();
        }
    }
}
