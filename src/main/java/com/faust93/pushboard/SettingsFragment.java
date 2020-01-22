package com.faust93.pushboard;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by faust93 on 23.04.2014.
 */
public class SettingsFragment extends Fragment implements Const, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private CheckBox serviceEnabled;
    private CheckBox enableLogging;
    private CheckBox enableMirroring;

    private TextView brokerAddress;
    private TextView brokerPort;
    private TextView broadcastGroup;
    private TextView mirrorDeviceId;

    private View rootView;

    public SettingsFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.settings_fragment, container, false);

        TextView devId = ((TextView) rootView.findViewById(R.id.dev_id));
        devId.setText(String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID)));

        brokerAddress = (TextView) rootView.findViewById(R.id.broker_address);
        brokerPort = (TextView) rootView.findViewById(R.id.broker_port);
        mirrorDeviceId = (TextView) rootView.findViewById(R.id.mirror_id);
        broadcastGroup = (TextView) rootView.findViewById(R.id.broadcast_group);
        serviceEnabled = (CheckBox) rootView.findViewById(R.id.srv_enable);
        enableLogging = (CheckBox) rootView.findViewById(R.id.enable_logging);

        enableMirroring = (CheckBox) rootView.findViewById(R.id.enable_mirroring);
        enableMirroring.setOnCheckedChangeListener(this);

        serviceEnabled.setChecked(MainActivity.getPreferences().getBoolean("srvEnabled",true));
        enableLogging.setChecked(MainActivity.getPreferences().getBoolean("enableLogging", false));
        enableMirroring.setChecked(MainActivity.getPreferences().getBoolean("enableMirroring", false));

        enableDisableView(rootView.findViewById(R.id._mirror_id), enableMirroring.isChecked());

        String brokerurl = MainActivity.getPreferences().getString("brokerURL","");
        if(!brokerurl.isEmpty())
            brokerAddress.setText(brokerurl);

        String brokerport = MainActivity.getPreferences().getString("brokerPort","");
        if(!brokerport.isEmpty())
            brokerPort.setText(brokerport);

        String broadcastgroup = MainActivity.getPreferences().getString("broadcastGroup","");
        if(!broadcastgroup.isEmpty())
            broadcastGroup.setText(broadcastgroup);

        String mirrordevicesid = MainActivity.getPreferences().getString("mirrorDevices","");
        if(!mirrordevicesid.isEmpty())
            mirrorDeviceId.setText(mirrordevicesid);

        rootView.findViewById(R.id.broker_url_click).setOnClickListener(this);
        rootView.findViewById(R.id.broker_port_click).setOnClickListener(this);
        rootView.findViewById(R.id.broadcast_click).setOnClickListener(this);
        rootView.findViewById(R.id._mirror_id).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.broker_url_click: setBroker(); break;
            case R.id.broker_port_click: setBrokerPort(); break;
            case R.id.broadcast_click: setBroadcastGroup(); break;
            case R.id._mirror_id: setMirroring(); break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton cbBox, boolean isChecked){
        switch (cbBox.getId()) {
            case R.id.enable_mirroring:
                if (isChecked){
                    enableDisableView(rootView.findViewById(R.id._mirror_id),true);
                } else {
                    enableDisableView(rootView.findViewById(R.id._mirror_id),false);
                }
                break;
        }
    }

    private void setBroker(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.broker_url);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 5) {
                    brokerAddress.setText(value);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.broker_url_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void setBrokerPort(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.broker_port);
        final EditText text_input = new EditText(getActivity());
        text_input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(text_input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 2) {
                    brokerPort.setText(value);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.broker_port_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void setBroadcastGroup(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.broadcast_group);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 1) {
                    broadcastGroup.setText(value);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.broadcast_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void setMirroring(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.mirror_dev_id);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 5) {
                    mirrorDeviceId.setText(value);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.mirror_dev_id_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void updatePrefs() {
        SharedPreferences.Editor editor = MainActivity.getPreferences().edit();
        editor.putString("brokerURL", brokerAddress.getText().toString());
        editor.putString("brokerPort", brokerPort.getText().toString());
        editor.putString("broadcastGroup", broadcastGroup.getText().toString());
        editor.putBoolean("srvEnabled", serviceEnabled.isChecked());
        editor.putBoolean("enableLogging", enableLogging.isChecked());
        editor.putBoolean("enableMirroring", enableMirroring.isChecked());
        editor.putString("mirrorDevices", mirrorDeviceId.getText().toString());
        editor.commit();
    }

    @Override
    public void onStop() {
        super.onStop();

        updatePrefs();

        if (MainActivity.getPreferences().getBoolean("srvEnabled",false)) {
                Log.d("pushboard","Restarting service");
                PushBoardService.actionRestart(getActivity());
        }
    }

    private void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        if ( view instanceof ViewGroup ) {
            ViewGroup group = (ViewGroup)view;
            for ( int idx = 0 ; idx < group.getChildCount() ; idx++ ) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }
}
