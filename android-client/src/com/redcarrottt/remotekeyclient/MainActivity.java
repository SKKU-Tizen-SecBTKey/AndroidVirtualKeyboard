package com.redcarrottt.remotekeyclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	private BluetoothAdapter mBluetoothAdapter = null;
	private DeviceListViewAdapter mDeviceListViewAdapter;
	private ListView mDeviceListView;
	private TextView mStatusTextView;
	private TextView mConnectedDeviceTextView;

	private Button mUpdateDeviceListButton;
	private Button mDisconnectDeviceButton;
	private Button mTurnOnOffBluetoothButton;
	private Button mMenuButton;
	private Button mHomeButton;
	private Button mBackButton;

	private BluetoothConnector mBluetoothConnector = null;
	private BluetoothInteractor mBluetoothInteractor = null;

	private BluetoothConnectionChecker mBluetoothConnectionChecker;

	private boolean mIsDeviceListEnabled = true;

	// Message strings
	private final String MSG_MENU_BUTTON = "menu";
	private final String MSG_HOME_BUTTON = "home";
	private final String MSG_BACK_BUTTON = "back";

	// Application Lifecycle
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Ensure bluetooth connection or quit app.
		boolean succeed = ensureBluetoothConnection();
		if (succeed == false) {
			return;
		}

		// Set broadcast receiver
		this.mBluetoothConnectionChecker = new BluetoothConnectionChecker();
		this.registerReceiver(this.mBluetoothConnectionChecker,
				new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		// Set status TextView
		this.mStatusTextView = (TextView) findViewById(R.id.statusTextView_mainActivity);

		// Set connected device TextView
		this.mConnectedDeviceTextView = (TextView) findViewById(R.id.connectedDeviceTextView_mainActivity);
		this.mConnectedDeviceTextView.setTextColor(Color.GRAY);
		this.mConnectedDeviceTextView.setText("No device connected");

		// Set Buttons
		this.mUpdateDeviceListButton = (Button) findViewById(R.id.updateDeviceListButton_mainActivity);
		this.mDisconnectDeviceButton = (Button) findViewById(R.id.disconnectDeviceButton_mainActivity);
		this.mTurnOnOffBluetoothButton = (Button) findViewById(R.id.turnOnOffBluetoothPButton_mainActivity);
		this.mMenuButton = (Button) findViewById(R.id.menuButton_mainActivity);
		this.mHomeButton = (Button) findViewById(R.id.homeButton_mainActivity);
		this.mBackButton = (Button) findViewById(R.id.backButton_mainActivity);
		this.mUpdateDeviceListButton.setOnClickListener(this);
		this.mDisconnectDeviceButton.setOnClickListener(this);
		this.mTurnOnOffBluetoothButton.setOnClickListener(this);
		this.mMenuButton.setOnClickListener(this);
		this.mHomeButton.setOnClickListener(this);
		this.mBackButton.setOnClickListener(this);

		this.updateTurnOnOffBluetoothButton();

		// Set ListView's adapter
		this.mDeviceListView = (ListView) findViewById(R.id.deviceListView);
		this.mDeviceListViewAdapter = new DeviceListViewAdapter(
				MainActivity.this, R.layout.device_listview_row);
		mDeviceListView.setAdapter(this.mDeviceListViewAdapter);

		// In default, update device list view
		this.updateDeviceList();
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
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// UI update methods

	private void _updateStatusTextView(String statusString) {
		this.mStatusTextView.setText(statusString);
	}

	private void _updateDeviceListView() {
		Set<BluetoothDevice> bondedDevices = this.mBluetoothAdapter
				.getBondedDevices();
		if (this.mDeviceListViewAdapter != null && bondedDevices != null)
			this.mDeviceListViewAdapter.onUpdateList(bondedDevices);
	}

	private void _updateConnectedDeviceTextView(String connectedDeviceName) {
		this.mConnectedDeviceTextView.setText(connectedDeviceName);
	}

	private void _enableDisconnectDeviceButton() {
		this.mDisconnectDeviceButton.setEnabled(true);
	}

	private void _disableDisconnectDeviceButton() {
		this.mDisconnectDeviceButton.setEnabled(false);
	}

	private void _disableDeviceListView() {
		this.mIsDeviceListEnabled = false;
		if (this.mDeviceListView != null)
			this.mDeviceListViewAdapter.notifyDataSetInvalidated();
	}

	private void _enableDeviceListView() {
		this.mIsDeviceListEnabled = true;
		if (this.mDeviceListView != null)
			this.mDeviceListViewAdapter.notifyDataSetInvalidated();
	}

	private void _enableUpdateDeviceListButton() {
		this.mUpdateDeviceListButton.setEnabled(true);
	}

	private void _disableUpdateDeviceListButton() {
		this.mUpdateDeviceListButton.setEnabled(false);
	}

	private void _setAsTurnOnDeviceButton() {
		this.mTurnOnOffBluetoothButton.setText("BT ON");
	}

	private void _setAsTurnOffDeviceButton() {
		this.mTurnOnOffBluetoothButton.setText("BT OFF");
	}

	// Functionalities of this activity
	private boolean ensureBluetoothConnection() {
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (this.mBluetoothAdapter == null) {
			DialogUtility.showDialogKillApp(this,
					"This device does not support Bluetooth!", "Quit App");
			return false;
		}

		if (this.mBluetoothAdapter.isEnabled() == false) {
			Intent enableBluetoothIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			final int REQUEST_ENABLE_BT = 2;
			startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
		}
		return true;
	}

	private void turnOnOrOffBluetooth() {
		if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON
				|| mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
			this.mBluetoothAdapter.disable();
		} else {
			this.mBluetoothAdapter.enable();
		}
	}

	private void tryToConnectDevice(BluetoothDevice device) {
		// Start to connect to the device
		this._updateStatusTextView("Connecting to " + device.getName() + "...");
		this._disableDeviceListView();

		this.mBluetoothConnector = new BluetoothConnector(device);
		this.mBluetoothConnector.start();
	}

	private void disconnectConnectedDevice() {
		if (this.mBluetoothInteractor == null) {
			DialogUtility.showDialogSimple(this,
					"There is no connection to disconnect.", "OK");
			return;
		}
		this._updateStatusTextView("Disconnecting...");

		this.mBluetoothInteractor.disconnect();
	}

	private void updateDeviceList() {
		this._updateDeviceListView();

		if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON
				|| mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
			this._updateStatusTextView("Device list is updated.");
		}
	}

	private void updateTurnOnOffBluetoothButton() {
		if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON
				|| mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
			final long fDelayMS = 3000;

			_updateStatusTextView("Bluetooth is turned on. Updating device list...");

			_setAsTurnOffDeviceButton();
			_enableUpdateDeviceListButton();
			_disableDisconnectDeviceButton();

			_enableDeviceListView();

			Runnable runnableRetryDelayed = new Runnable() {
				@Override
				public void run() {
					updateDeviceList();
				}
			};
			Handler delayHandler = new Handler();
			delayHandler.postDelayed(runnableRetryDelayed, fDelayMS);
		} else {
			updateDeviceList();
			_updateStatusTextView("Bluetooth is turned off");

			_setAsTurnOnDeviceButton();
			_disableUpdateDeviceListButton();

			_disableDeviceListView();
		}
	}

	private void pressButton(String buttonMessage) {
		if (this.mBluetoothInteractor == null) {
			DialogUtility.showDialogSimple(this,
					"You should connect to target before press it.", "OK");
			return;
		}

		BluetoothMessage msg = new BluetoothMessage(buttonMessage);
		boolean ret = this.mBluetoothInteractor.sendMessage(msg);
		if (ret == true) {
			this._updateStatusTextView("Sent an button event (" + buttonMessage + ") to the target");
		} else {
			this._updateStatusTextView("Failed to send the button event (" + buttonMessage + ") to the target");
		}
	}

	private void pressMenuButton() {
		this.pressButton(MSG_MENU_BUTTON);
	}

	private void pressHomeButton() {
		this.pressButton(MSG_HOME_BUTTON);
	}

	private void pressBackButton() {
		this.pressButton(MSG_BACK_BUTTON);
	}

	// OnClickListener of buttons
	@Override
	public void onClick(View view) {
		if (view == this.mTurnOnOffBluetoothButton) {
			// If turn on off Bluetooth is on click
			this.turnOnOrOffBluetooth();
		} else if (view == this.mUpdateDeviceListButton) {
			// If update device list button is on click
			this.updateDeviceList();
		}
		if (view == this.mDisconnectDeviceButton) {
			// If disconnect device button is on click
			this.disconnectConnectedDevice();
		} else if (view == this.mMenuButton) {
			// If menu button is on click
			this.pressMenuButton();
		} else if (view == this.mHomeButton) {
			// If home button is on click
			this.pressHomeButton();
		} else if (view == this.mBackButton) {
			// If back button is on click
			this.pressBackButton();
		}
	}

	// Bluetooth Handlers
	private void onEstablishBluetoothConnection(BluetoothSocket socket,
			BluetoothDevice device) {
		final BluetoothDevice fDevice = device;

		this.mBluetoothInteractor = new BluetoothInteractor(socket);
		this.mBluetoothInteractor.start();

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_enableDisconnectDeviceButton();
				_updateStatusTextView("Connected");
				_updateConnectedDeviceTextView(fDevice.getName() + "("
						+ fDevice.getAddress() + ")");
			}
		};
		this.runOnUiThread(runnableOnUIThread);
	}

	private void onFailureBluetoothConnection(String message) {
		final String fStatusMsg = "Connection Failure";
		final String fDialogMsg = fStatusMsg + ": " + message;
		final Context thisContext = this;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				DialogUtility.showDialogSimple(thisContext, fDialogMsg, "OK");
				_updateStatusTextView(fStatusMsg);
				_updateConnectedDeviceTextView("No device connected");

				_enableDeviceListView();
			}
		};
		runOnUiThread(runnableOnUIThread);
	}

	private void onFailureBluetoothInteraction(String message) {
		final String fStatusMsg = "Interaction Failure";
		final String fDialogMsg = fStatusMsg + ": " + message;
		final Context thisContext = this;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				DialogUtility.showDialogSimple(thisContext, fDialogMsg, "OK");
				_updateStatusTextView(fStatusMsg);
				_updateConnectedDeviceTextView("No device connected");

				_disableDisconnectDeviceButton();

				_enableDeviceListView();
			}
		};
		this.runOnUiThread(runnableOnUIThread);
	}

	private void onReceiveBluetoothMessage(BluetoothMessage message) {
		final BluetoothMessage fMessage = message;
		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_updateStatusTextView("The button is pressed!: "
						+ fMessage.toString());
			}
		};
		this.runOnUiThread(runnableOnUIThread);
	}

	private void onDidDisconnectBluetooth() {
		this.mBluetoothInteractor = null;
		this.mBluetoothConnector = null;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_updateStatusTextView("Successfully disconnected");
				_updateConnectedDeviceTextView("No device connected");

				_disableDisconnectDeviceButton();

				_enableDeviceListView();
			}
		};
		this.runOnUiThread(runnableOnUIThread);
	}

	private void onDidChangeBluetoothState() {
		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				updateTurnOnOffBluetoothButton();
			}
		};
		this.runOnUiThread(runnableOnUIThread);
	}

	// DeviceListViewAdapter
	private class DeviceListViewAdapter extends ArrayAdapter<BluetoothDevice> {
		private Context mContext;

		public DeviceListViewAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			this.mContext = context;
		}

		public void onUpdateList(Collection<BluetoothDevice> devices) {
			this.clear();
			this.addAll(devices);
			this.notifyDataSetChanged();
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				// If there is no row view in the cache, create a new row view.
				LayoutInflater inflater = (LayoutInflater) this.mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.device_listview_row, null);
			}
			// Fill the row view
			TextView deviceNameTextView = (TextView) rowView
					.findViewById(R.id.deviceListView_deviceNameTextView);
			TextView macAddressTextView = (TextView) rowView
					.findViewById(R.id.deviceListView_macAddressTextView);
			BluetoothDevice thisDevice = this.getItem(position);
			String nameText = thisDevice.getName();
			switch (thisDevice.getBondState()) {
			case BluetoothDevice.BOND_BONDED:
				deviceNameTextView.setTextColor(Color.BLACK);
				break;
			default:
				deviceNameTextView.setTextColor(Color.GRAY);
				break;
			}
			deviceNameTextView.setText(nameText);
			macAddressTextView.setTextColor(Color.GRAY);
			macAddressTextView.setText(thisDevice.getAddress());
			deviceNameTextView.setBackgroundColor(Color.TRANSPARENT);
			macAddressTextView.setBackgroundColor(Color.TRANSPARENT);

			final int fPosition = position;
			rowView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					if (mIsDeviceListEnabled == true) {
						BluetoothDevice device = getItem(fPosition);
						tryToConnectDevice(device);
					}
				}
			});

			if (mIsDeviceListEnabled == true) {
				rowView.setBackgroundColor(Color.parseColor("#FFFFFF"));
			} else {
				deviceNameTextView.setTextColor(Color.GRAY);
				rowView.setBackgroundColor(Color.parseColor("#D8D8D8"));
			}

			return rowView;
		}
	}

	private class BluetoothConnectionChecker extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			onDidChangeBluetoothState();
		}
	}

	private class BluetoothConnector extends Thread {
		private final UUID MY_UUID = UUID
				.fromString("00001101-0000-1000-8000-00805F9B34FB");
		private final BluetoothSocket mBluetoothSocket;
		private final BluetoothDevice mBluetoothDevice;

		public BluetoothConnector(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			this.mBluetoothSocket = tmp;
			this.mBluetoothDevice = device;
		}

		@Override
		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();
			try {
				// Connect the device through the socket. This will block until
				// it succeeds or throws an exception
				this.mBluetoothSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					this.mBluetoothSocket.close();
					onFailureBluetoothConnection("Connection failed ("
							+ connectException.getMessage() + ")");
				} catch (IOException closeException) {
					onFailureBluetoothConnection("Socket close failed ("
							+ closeException.getMessage() + ")");
				}
				return;
			}
			// Do work to manage the connection (in a separate thread)
			onEstablishBluetoothConnection(this.mBluetoothSocket,
					this.mBluetoothDevice);
		}
	}

	private class BluetoothInteractor extends Thread {
		private final long SLEEP_MILLISECONDS = 200;

		private BluetoothSocket mBluetoothSocket;

		private boolean mIsRunning;

		private InputStream mInputStream;
		private OutputStream mOutputStream;

		public BluetoothInteractor(BluetoothSocket socket) {
			this.mBluetoothSocket = socket;
			this.mIsRunning = false;
		}

		@Override
		public void run() {
			// Interaction routine
			final int fBufferSize = 1024;
			byte[] readBuffer = new byte[fBufferSize];

			try {
				this.mInputStream = this.mBluetoothSocket.getInputStream();
				this.mOutputStream = this.mBluetoothSocket.getOutputStream();
				int bytesRead = -1;
				BluetoothMessage btReceivedMsg = null;
				this.mIsRunning = true;
				while (this.mIsRunning == true) {
					// Critical section 1
					// Send a message
					btReceivedMsg = new BluetoothMessage();
					bytesRead = this.mInputStream.read(readBuffer);
					if (bytesRead != -1) {
						// Make Java string from received message
						btReceivedMsg.attachBytes(readBuffer, 0, bytesRead);
						onReceiveBluetoothMessage(btReceivedMsg);
					}
					// Sleep for prevention of busy-wait
					try {
						Thread.sleep(SLEEP_MILLISECONDS);
					} catch (InterruptedException e) {
					}
				}
				this.mInputStream.close();
				this.mOutputStream.close();
			} catch (IOException e) {
				onFailureBluetoothInteraction(e.getMessage());
			}
			try {
				if (this.mBluetoothSocket.isConnected() == true)
					this.mBluetoothSocket.close();
				onDidDisconnectBluetooth();
			} catch (IOException e) {
				onFailureBluetoothInteraction(e.getMessage());
			}
		}

		public boolean sendMessage(BluetoothMessage msg) {
			if (this.mIsRunning == false)
				return false;

			if (this.mBluetoothSocket != null
					&& this.mBluetoothSocket.isConnected() == true) {
				try {
					this.mOutputStream.write(msg.toByteArray());
					this.mOutputStream.flush();
				} catch (IOException e) {
					return false;
				}
			}
			return true;
		}

		public void disconnect() {
			this.mIsRunning = false;
			try {
				this.mBluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class BluetoothMessage {
		// Encoding: US-ASCII
		private final Charset charset = Charset.forName("US-ASCII");
		private String mMsg;

		public BluetoothMessage(String msg) {
			this.mMsg = msg;
		}

		public BluetoothMessage() {
			this.mMsg = "";
		}

		public void attachBytes(byte[] msg, int start, int bytesRead) {
			CharBuffer charBuffer = charset.decode(ByteBuffer.wrap(msg, start,
					bytesRead));
			this.mMsg = this.mMsg + new String(charBuffer.array());
		}

		public String toString() {
			return this.mMsg;
		}

		public byte[] toByteArray() {
			ByteBuffer byteBuffer = charset.encode(CharBuffer.wrap(this.mMsg
					.toCharArray()));
			return byteBuffer.array();
		}
	}
}
