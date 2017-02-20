package com.example.konstantinos.mobilecontroller;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ListView devicelist;
    Button paired;
    private BluetoothAdapter mBluetooth;
    private Set<BluetoothDevice> pairedDevices;
    private String address;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ArrayAdapter mArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        devicelist = (ListView)findViewById(R.id.deviceList);


        if(mBluetooth == null){
            Toast.makeText(MainActivity.this, "Bluetooth Device Not Available!", Toast.LENGTH_LONG).show();
            //finish th app
            finish();
        }
        else {
            if(mBluetooth.isEnabled()){
                Toast.makeText(MainActivity.this,"Bluetooth Enabled",Toast.LENGTH_LONG).show();
            }
            else{
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }
        }


    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.v("discover","started");
            }

            // When discovery finds a device
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.v("START","receiver");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.v("device",device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_connect){
            Toast.makeText(MainActivity.this,"Try to connect",Toast.LENGTH_LONG).show();
            pairedDevices = mBluetooth.getBondedDevices();
            final ArrayList list = new ArrayList();

            if(pairedDevices.size()>0){
                for(BluetoothDevice bt : pairedDevices)
                {
                    list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
                }
                ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
                devicelist.setAdapter(adapter);
                devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    public void onItemClick (AdapterView av, View v, int arg2, long arg3)
                    {
                        // Get the device MAC address, the last 17 chars in the View
                        String info = ((TextView) v).getText().toString();
                        String address = info.substring(info.length() - 17);
                    }
                }); //Method called when the device from the list is clicked
            }
            else{
                mBluetooth.startDiscovery();

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                registerReceiver(mReceiver, filter);
                registerReceiver(mReceiver, filter2);

                mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1);
                devicelist.setAdapter(mArrayAdapter);
                devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    public void onItemClick (AdapterView av, View v, int arg2, long arg3)
                    {
                        // Get the device MAC address, the last 17 chars in the View
                        String info = ((TextView) v).getText().toString();
                        String address = info.substring(info.length() - 17);
                    }
                }); //Method called when the device from the list is clicked
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected



        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    mBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = mBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(),"Connection Failed. Is it a SPP Bluetooth? Try again.",Toast.LENGTH_LONG).show();
                finish();
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Connected.",Toast.LENGTH_LONG).show();
                isBtConnected = true;
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
