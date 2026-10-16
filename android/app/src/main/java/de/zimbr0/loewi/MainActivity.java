package de.zimbr0.loewi;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(LoewiDateiPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
