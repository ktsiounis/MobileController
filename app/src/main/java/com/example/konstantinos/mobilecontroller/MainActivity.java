package com.example.konstantinos.mobilecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static ListView devicelist;
    Button paired;
    private static BluetoothAdapter mBluetooth;
    private Set<BluetoothDevice> pairedDevices;
    public String address;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static ArrayAdapter mArrayAdapter;
    private static ArrayList list = new ArrayList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBluetooth = BluetoothAdapter.getDefaultAdapter();


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
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private static final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                list.add(device.getName() + "\n" + device.getAddress());
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
            pairedDevices = mBluetooth.getBondedDevices();

            if(pairedDevices.size()>0){
                for(BluetoothDevice bt : pairedDevices)
                {
                    list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
                }
                String[] listArr = new String[list.size()];
                list.toArray(listArr);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Device");
                builder.setItems(listArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        address = (list.get(i).toString()).substring((list.get(i).toString()).length() - 17);
                        Log.v("DIALOG", address);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else {
                mBluetooth.startDiscovery();

                String[] listArr = new String[list.size()];
                list.toArray(listArr);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Device");
                if(list.size() == 0){
                    builder.setMessage("There is no available devices");
                }
                else {
                    builder.setItems(listArr, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            address = (list.get(i).toString()).substring((list.get(i).toString()).length()-17);
                            Log.v("DIALOG", address);
                            ConnectBT connection = new ConnectBT(address);
                            connection.run();
                        }
                    });
                }
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }


        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectBT(String address) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = mBluetooth.getRemoteDevice(address);;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                Log.e("FAIL", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() //while the progress dialog is shown, the connection is done in background
        {
            mBluetooth.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.v("Connected", "connected");
                Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_LONG);
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Toast.makeText(getApplicationContext(),"Connection Failed",Toast.LENGTH_LONG);
                try {
                    mmSocket.close();
                    Log.v("Connected", "socket closed");
                } catch (IOException closeException) {
                    Log.e("FAIL", "Could not close the client socket", closeException);
                }
                //return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
