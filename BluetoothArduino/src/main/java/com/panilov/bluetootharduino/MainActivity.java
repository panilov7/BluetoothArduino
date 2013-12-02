package com.panilov.bluetootharduino;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MESSAGE_READ = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Handler mHandler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private BroadcastReceiver mReceiverConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PlaceholderFragment fragment = new PlaceholderFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your Phone does not support Bluetooth!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }

        final StringBuilder sb = new StringBuilder();

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIn = new String(readBuf, 0, msg.arg1);
                        sb.append(strIn);
                        int endOfLineIndex = sb.indexOf("\r\n");
                        if (endOfLineIndex > 0) {
                            String dataIn = sb.substring(0, endOfLineIndex);

                            String[] parts = dataIn.split(":");

                            sb.delete(0, sb.length());
                            if (parts[0].equals("Connected")) {
                                fragment.tvConnSetText(parts[0] + " to " + connectThread.mmDevice.getName());
                                fragment.enableOnConnect(parts[1].substring(1));
                            }
                            Log.d(TAG, "------------Data from Arduino: " + dataIn);

                        }
                        break;
                }
            }
        };

        mReceiverConnection = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d(TAG, "----------Bluetooth Device Connected!!!!");
                    //Toast.makeText(MainActivity.this, "Bluetooth Device Connected!", Toast.LENGTH_SHORT).show();
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.d(TAG, "----------Bluetooth Device Disconnected!!!!");
                    //Toast.makeText(MainActivity.this, "Bluetooth Device Disconnected!", Toast.LENGTH_SHORT).show();
                    fragment.disableOnDisconnect();
                }
            }
        };

        IntentFilter filter_connected = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter_disconnected = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(mReceiverConnection, filter_connected);
        registerReceiver(mReceiverConnection, filter_disconnected);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectThread != null) {
            connectThread.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiverConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                switch (resultCode) {
                    case RESULT_CANCELED:
                        finish();
                        break;
                    case RESULT_OK:

                        break;
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_connect:

                ConnectDialog connectDialog = new ConnectDialog();
                connectDialog.show(getSupportFragmentManager(), "dialog");

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {
        private static final String TAG = "PlaceholderFragment";
        public TextView tvConn;
        private CheckBox chkBoxBacklight;
        private SeekBar seekBarBacklight;
        private boolean chk = false, seek = false;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            Log.d(TAG, "savedInstanceState2: " + savedInstanceState);

            if (savedInstanceState != null) {
                chk = savedInstanceState.getBoolean("chkBoxBacklight");
                seek = (savedInstanceState.getBoolean("seekBarBacklight"));
            }

            return rootView;
        }

        @Override
        public void onStart() {
            super.onStart();

            tvConn = (TextView) this.getActivity().findViewById(R.id.tvConnection);
            chkBoxBacklight = (CheckBox) this.getActivity().findViewById(R.id.chkBoxBacklight);
            seekBarBacklight = (SeekBar) this.getActivity().findViewById(R.id.seekBarBacklight);

            chkBoxBacklight.setEnabled(false);
            chkBoxBacklight.setChecked(chk);
            seekBarBacklight.setEnabled(seek);

            tvConn.setText("Disconnected");
            chkBoxBacklight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        chkBoxBacklight.setText(R.string.chk_box_backlight_text_manual);
                        seekBarBacklight.setEnabled(true);
                        String s = "b" + seekBarBacklight.getProgress();
                        connectedThread.write(s.getBytes());
                    } else {
                        chkBoxBacklight.setText(R.string.chk_box_backlight_text_auto);
                        seekBarBacklight.setEnabled(false);
                        connectedThread.write("ba".getBytes());
                    }
                }
            });

            seekBarBacklight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    String s = "b" + progress;
                    connectedThread.write(s.getBytes());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        public void tvConnSetText(String text) {
            tvConn.setText(text);
        }

        public void enableOnConnect(String inVal) {
            if (inVal.equals("a")) {
                chk = false;
                seek = false;
                seekBarBacklight.setEnabled(seek);
                seekBarBacklight.setProgress(0);
                chkBoxBacklight.setEnabled(true);
                chkBoxBacklight.setText(R.string.chk_box_backlight_text_auto);
                chkBoxBacklight.setChecked(chk);
            } else {
                int seekBarPos = Integer.valueOf(inVal);
                chk = true;
                seek = true;
                seekBarBacklight.setEnabled(seek);
                seekBarBacklight.setProgress(seekBarPos);
                chkBoxBacklight.setEnabled(true);
                chkBoxBacklight.setChecked(chk);
            }
        }

        public void disableOnDisconnect() {
            chk = false;
            seek = false;
            seekBarBacklight.setEnabled(seek);
            seekBarBacklight.setProgress(0);
            chkBoxBacklight.setEnabled(false);
            chkBoxBacklight.setChecked(chk);
            tvConn.setText("Disconnected");
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("chkBoxBacklight", chkBoxBacklight.isChecked());
            outState.putBoolean("seekBarBacklight", seekBarBacklight.isEnabled());
        }
    }

    public class ConnectDialog extends DialogFragment {
        private IntentFilter filter, filter2, filter3, filter4, filter5;
        private BroadcastReceiver mReceiver;

        public ConnectDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final ArrayList<Item> mArrayListPaired = new ArrayList<Item>();
            final ArrayList<Item> mArrayListDiscovered = new ArrayList<Item>();

            // Get the paired BT devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayListPaired.add(new Item(device.getName(), device.getAddress()));
                }
            }

            mArrayListPaired.add(new Item("Search", "Click to search for Devices"));

            final ArrayAdapter<Item> adapterPaired = new MyListAdapter(getActivity(), R.layout.list_item, mArrayListPaired);
            final ArrayAdapter<Item> adapterDisc = new MyListAdapter(getActivity(), R.layout.list_item, mArrayListDiscovered);

            LayoutInflater inflater = (LayoutInflater) getActivity().getLayoutInflater();
            final LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.dialog_connect, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final ListView lvPaired = (ListView) linearLayout.findViewById(R.id.lvPaired);
            lvPaired.setAdapter(adapterPaired);

            final ListView lvDisc = (ListView) linearLayout.findViewById(R.id.lvDiscovered);
            lvDisc.setAdapter(adapterDisc);

            builder.setView(linearLayout)
                    .setTitle(R.string.alert_dialog_connect_title)
                    .setNegativeButton(R.string.alert_dialog_connect_negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            final ProgressBar progressBar = (ProgressBar) linearLayout.findViewById(R.id.progressBarSearch);
            // Get the Discovered BT devices
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        mArrayListDiscovered.add(new Item(device.getName(), device.getAddress()));
                        Toast.makeText(MainActivity.this, "Found: " + device.getName(), Toast.LENGTH_SHORT).show();
                        adapterDisc.notifyDataSetChanged();
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        progressBar.setVisibility(View.INVISIBLE);
                        adapterDisc.notifyDataSetChanged();
                        if (mBluetoothAdapter.isDiscovering())
                            mBluetoothAdapter.cancelDiscovery();
                        Toast.makeText(MainActivity.this, "Finished Discovery!", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            // Register the BroadcastReceiver
            filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
            registerReceiver(mReceiver, filter2);

            final AlertDialog alertDialog = builder.create();
            lvPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
                    TextView tvAddress = (TextView) view.findViewById(R.id.tvDescription);

                    if (tvTitle.getText().toString().equals("Search")) {
                        // Search for other available devices
                        mArrayListPaired.remove(position);
                        adapterPaired.notifyDataSetChanged();

                        TextView tvDesc = (TextView) linearLayout.findViewById(R.id.tvDiscovered);

                        boolean descStarted = mBluetoothAdapter.startDiscovery();
                        if (descStarted) {
                            progressBar.setVisibility(View.VISIBLE);
                            tvDesc.setVisibility(View.VISIBLE);
                            lvDisc.setVisibility(View.VISIBLE);
                        }

                    } else {

                        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(tvAddress.getText().toString());

                        //TODO connect to device here
                        connectThread = new ConnectThread(mBluetoothDevice);
                        connectThread.start();

                        alertDialog.dismiss();
                    }
                }
            });

            lvDisc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    TextView tvAddress = (TextView) view.findViewById(R.id.tvDescription);

                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(tvAddress.getText().toString());

                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(tvAddress.getText().toString());

                    connectThread = new ConnectThread(mBluetoothDevice);
                    connectThread.start();

                    alertDialog.dismiss();
                }
            });

            setRetainInstance(true);

            return alertDialog;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mReceiver);
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
                if (connectedThread != null)
                    connectedThread.cancel();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
