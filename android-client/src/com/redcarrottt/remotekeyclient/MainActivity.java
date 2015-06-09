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
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;






import android.widget.Toast;

import com.chilkatsoft.*;


public class MainActivity extends Activity implements OnClickListener {
	private BluetoothAdapter mBluetoothAdapter = null;
	private DeviceListViewAdapter mDeviceListViewAdapter;
	private ListView mDeviceListView;
	private TextView mStatusTextView;
	private TextView mConnectedDeviceTextView;
	
	private EditText mEditText;

	private Button mUpdateDeviceListButton;
	private Button mDisconnectDeviceButton;
	private Button mMenuButton;
	private Button mHomeButton;

	private BluetoothConnector mBluetoothConnector = null;
	private BluetoothInteractor mBluetoothInteractor = null;

	private BluetoothConnectionChecker mBluetoothConnectionChecker;

	private boolean mIsDeviceListEnabled = true;
	
	private CkDh dhBob;
    private CkDh dhAlice;
    private String eBob;
    private String eAlice;
    private String sendKey;

	
	private static final String TAG = "Chilkat";
	
	
	 static {
	      // Important: Make sure the name passed to loadLibrary matches the shared library
	      // found in your project's libs/armeabi directory.
	      //  for "libchilkat.so", pass "chilkat" to loadLibrary
	      //  for "libchilkatemail.so", pass "chilkatemail" to loadLibrary
	      //  etc.
	      // 
	      System.loadLibrary("chilkat");

	      // Note: If the incorrect library name is passed to System.loadLibrary,
	      // then you will see the following error message at application startup:
	      //"The application <your-application-name> has stopped unexpectedly. Please try again."
	  }
	 
	
	// Application Lifecycle
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dhBob = new CkDh();
	    dhAlice = new CkDh();

		
		
		keySHARE();
		
		
		
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
		this.mMenuButton = (Button) findViewById(R.id.menuButton_mainActivity);
		this.mHomeButton = (Button) findViewById(R.id.homeButton_mainActivity);
		this.mUpdateDeviceListButton.setOnClickListener(this);
		this.mDisconnectDeviceButton.setOnClickListener(this);
		this.mMenuButton.setOnClickListener(this);
		this.mHomeButton.setOnClickListener(this);
		
		// Edit Text
		this.mEditText = (EditText) findViewById(R.id.editText);

		this.updateTurnOnOffBluetoothButton();

		// Set ListView's adapter
		this.mDeviceListView = (ListView) findViewById(R.id.deviceListView);
		this.mDeviceListViewAdapter = new DeviceListViewAdapter(
				MainActivity.this, R.layout.device_listview_row);
		mDeviceListView.setAdapter(this.mDeviceListViewAdapter);

		// In default, update device list view
		this.updateDeviceList();
	}
	
	
	
	public void keyShare_send() {
		
	    boolean success;

	    //  Unlock the component once at program startup...
	    success = dhBob.UnlockComponent("Anything for 30-day trial");
	    if (success != true) {
	        Log.i(TAG, dhBob.lastErrorText());
	        return;
	    }

	    dhBob.UseKnownPrime(2);
	    String p;
	    int g;
	    //  Bob will now send P and G to Alice.
	    
	    // 1. P, 2. G 보내고,
	    p = dhBob.p();
	    this.pressButton(p);
	    
	    g = dhBob.get_G();
	    String gtemp = Integer.toString(g);
	    this.pressButton(gtemp);
	    
	    success = dhAlice.SetPG(p,g);
	    
	    if (success != true) {
	        Log.i(TAG, "P is not a safe prime");
	        return;
	    }

	    String eBob;
	    eBob = dhBob.createE(256);
	    
	    // 3. eBob 보내고
	    this.pressButton(eBob);   
	}
	
	public void keyShare_receive(String rev) {
		
		boolean success;
		
		// 4. eAlice 받고 KBob을 구한다.

	    //  Alice does the same:
//	    String eAlice;
//	    eAlice = dhAlice.createE(256);
		
		eAlice = rev;

	    //  The "E" values are sent over the insecure channel.
	    //  Bob sends his "E" to Alice, and Alice sends her "E" to Bob.

	    //  Each side computes the shared secret by calling FindK.
	    //  "K" is the shared-secret.

	    String kBob;
	    String kAlice;

	    //  Bob computes the shared secret from Alice's "E":
	    kBob = dhBob.findK(eAlice);

	    //  Alice computes the shared secret from Bob's "E":
	    kAlice = dhAlice.findK(eBob);

	    CkCrypt2 crypt = new CkCrypt2();
	    success = crypt.UnlockComponent("Anything for 30-day trial.");
	    if (success != true) {
	        Log.i(TAG, crypt.lastErrorText());
	        return;
	    }

	    crypt.put_EncodingMode("hex");
	    crypt.put_HashAlgorithm("md5");

	    String sessionKey;
	    sessionKey = crypt.hashStringENC(kBob);

	    Log.i(TAG, "128-bit Session Key:");
	    Log.i(TAG, sessionKey);

	    //  Encrypt something...
	    crypt.put_CryptAlgorithm("aes");
	    crypt.put_KeyLength(128);
	    crypt.put_CipherMode("cbc");

	    //  Use an IV that is the MD5 hash of the session key...
	    String iv;
	    iv = crypt.hashStringENC(sessionKey);

	    crypt.SetEncodedKey(sessionKey,"hex");
	    crypt.SetEncodedIV(iv,"hex");

	    //  Encrypt some text:
	    String cipherText64;

	    crypt.put_EncodingMode("base64");
	    cipherText64 = crypt.encryptStringENC(sendKey);

	    String plainText;
	    plainText = crypt.decryptStringENC(cipherText64);
	    
	    // Last Send
	    pressButton(cipherText64);
	    Toast.makeText(MainActivity.this, cipherText64, Toast.LENGTH_LONG).show();
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

			_disableUpdateDeviceListButton();

			_disableDeviceListView();
		}
	}

	//---------------------------------------------------------------------------------
	// send event: Test key
	private void pressButton(String buttonMessage) {
		if (this.mBluetoothInteractor == null) {
			DialogUtility.showDialogSimple(this,
					"You should connect to target before press it.", "OK");
			return;
		}

		BluetoothMessage msg = new BluetoothMessage(buttonMessage);
		boolean ret = this.mBluetoothInteractor.sendMessage(msg);
		if (ret == true) {
			this._updateStatusTextView("Message: " + buttonMessage);
		} else {
			this._updateStatusTextView("Failed to send the event");
		}
	}

	//----------------------------------------------------------------------
	// Test Key Send
	private void pressMenuButton() {
		// original key
		Editable etStr = mEditText.getText();
		this.pressButton(etStr.toString());
	}
	
	//----------------------------------------------------------------------
	// Security Key Send
	private void pressHomeButton() {
		Toast.makeText(MainActivity.this, "S-Key generate", Toast.LENGTH_LONG).show();
		keyShare_send();
		
		// original key
		Editable etTxt = mEditText.getText();
		sendKey = etTxt.toString();
//		this.pressButton(etStr.toString());
	}

	// OnClickListener of buttons
	@Override
	public void onClick(View view) {

		if (view == this.mUpdateDeviceListButton) {
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
				Toast.makeText(MainActivity.this, "Receive Data", Toast.LENGTH_LONG).show();
				_updateStatusTextView("The button is pressed!: "
						+ fMessage.toString());
				
				keyShare_receive(fMessage.toString());
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
	
	public void keySHARE() {
		 
	    //  Create two separate instances of the DH object.
	    CkDh dhBob = new CkDh();
	    CkDh dhAlice = new CkDh();

	    boolean success;

	    //  Unlock the component once at program startup...
	    success = dhBob.UnlockComponent("Anything for 30-day trial");
	    if (success != true) {
	        Log.i(TAG, dhBob.lastErrorText());
	        return;
	    }

	    //  The DH algorithm begins with a large prime, P, and a generator, G.
	    //  These don't have to be secret, and they may be transmitted over an insecure channel.
	    //  The generator is a small integer and typically has the value 2 or 5.

	    //  The Chilkat DH component provides the ability to use known
	    //  "safe" primes, as well as a method to generate new safe primes.

	    //  This example will use a known safe prime.  Generating
	    //  new safe primes is a time-consuming CPU intensive task
	    //  and is normally done offline.

	    //  Bob will choose to use the 2nd of our 8 pre-chosen safe primes.
	    //  It is the Prime for the 2nd Oakley Group (RFC 2409) --
	    //  1024-bit MODP Group.  Generator is 2.
	    //  The prime is: 2^1024 - 2^960 - 1 + 2^64 * { [2^894 pi] + 129093 }
	    dhBob.UseKnownPrime(2);
	    
	    //  The computed shared secret will be equal to the size of the prime (in bits).
	    //  In this case the prime is 1024 bits, so the shared secret will be 128 bytes (128 * 8 = 1024).
	    //  However, the result is returned as an SSH1-encoded bignum in hex string format.
	    //  The SSH1-encoding prepends a 2-byte count, so the result is going  to be 2 bytes
	    //  longer: 130 bytes.  This results in a hex string that is 260 characters long (two chars
	    //  per byte for the hex encoding).

	    String p;
	    int g;
	    //  Bob will now send P and G to Alice.
	    
	    // 1. P, 2. G 보내고,
	    p = dhBob.p();
	    g = dhBob.get_G();

	    Log.i(TAG, "p:" + p);
	    Log.i(TAG, "g:" + g);
	    
	    //  Alice calls SetPG to set P and G.  SetPG checks
	    //  the values to make sure it's a safe prime and will
	    //  return false if not.
	    success = dhAlice.SetPG(p,g);
	    if (success != true) {
	        Log.i(TAG, "P is not a safe prime");
	        return;
	    }

	    //  Each side begins by generating an "E"
	    //  value.  The CreateE method has one argument: numBits.
	    //  It should be set to twice the size of the number of bits
	    //  in the session key.

	    //  Let's say we want to generate a 128-bit session key
	    //  for AES encryption.  The shared secret generated by the Diffie-Hellman
	    //  algorithm will be longer, so we'll hash the result to arrive at the
	    //  desired session key length.  However, the length of the session
	    //  key we'll utlimately produce determines the value that should be
	    //  passed to the CreateE method.

	    //  In this case, we'll be creating a 128-bit session key, so pass 256 to CreateE.
	    //  This setting is for security purposes only -- the value
	    //  passed to CreateE does not change the length of the shared secret
	    //  that is produced by Diffie-Hellman.
	    //  Also, there is no need to pass in a value larger
	    //  than 2 times the expected session key length.  It suffices to
	    //  pass exactly 2 times the session key length.

	    //  Bob generates a random E (which has the mathematical
	    //  properties required for DH).
	    eBob = dhBob.createE(256);
	    
	    // 3. e.Bob 보내고
	    
	    // 4. eAlice 받고 KBob을 구한다.

	    //  Alice does the same:
	    String eAlice;
	    eAlice = dhAlice.createE(256);

	    //  The "E" values are sent over the insecure channel.
	    //  Bob sends his "E" to Alice, and Alice sends her "E" to Bob.

	    //  Each side computes the shared secret by calling FindK.
	    //  "K" is the shared-secret.

	    String kBob;
	    String kAlice;

	    //  Bob computes the shared secret from Alice's "E":
	    kBob = dhBob.findK(eAlice);

	    //  Alice computes the shared secret from Bob's "E":
	    kAlice = dhAlice.findK(eBob);

	    
	    Log.i(TAG, "eBob:" + eBob);
	    Log.i(TAG, "eAlice:" + eAlice);
	    
	    //  Amazingly, kBob and kAlice are identical and the expected
	    //  length (260 characters).  The strings contain the hex encoded bytes of
	    //  our shared secret:
	    Log.i(TAG, "Bob's shared secret:");
	    Log.i(TAG, kBob);
	    Log.i(TAG, "Alice's shared secret (should be equal to Bob's)");
	    Log.i(TAG, kAlice);

	    //  To arrive at a 128-bit session key for AES encryption, Bob and Alice should
	    //  both transform the raw shared secret using a hash algorithm that produces
	    //  the size of session key desired.   MD5 produces a 16-byte (128-bit) result, so
	    //  this is a good choice for 128-bit AES.

	    //  Here's how you would use Chilkat Crypt (a separate Chilkat component) to
	    //  produce the session key:
	    CkCrypt2 crypt = new CkCrypt2();
	    success = crypt.UnlockComponent("Anything for 30-day trial.");
	    if (success != true) {
	        Log.i(TAG, crypt.lastErrorText());
	        return;
	    }

	    crypt.put_EncodingMode("hex");
	    crypt.put_HashAlgorithm("md5");

	    String sessionKey;
	    sessionKey = crypt.hashStringENC(kBob);

	    Log.i(TAG, "128-bit Session Key:");
	    Log.i(TAG, sessionKey);

	    //  Encrypt something...
	    crypt.put_CryptAlgorithm("aes");
	    crypt.put_KeyLength(128);
	    crypt.put_CipherMode("cbc");

	    //  Use an IV that is the MD5 hash of the session key...
	    String iv;
	    iv = crypt.hashStringENC(sessionKey);

	    //  AES uses a 16-byte IV:
	    Log.i(TAG, "Initialization Vector:");
	    Log.i(TAG, iv);

	    crypt.SetEncodedKey(sessionKey,"hex");
	    crypt.SetEncodedIV(iv,"hex");

	    //  Encrypt some text:
	    String cipherText64;

	    crypt.put_EncodingMode("base64");
	    cipherText64 = crypt.encryptStringENC("home");
	    Log.i(TAG, cipherText64);

	    String plainText;
	    plainText = crypt.decryptStringENC(cipherText64);

	    Log.i(TAG, plainText);
	}
}
