package com.redcarrottt.remotekeyserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private BluetoothAdapter mBluetoothAdapter = null;

	private TextView mStatusTextView;
	private TextView mConnectedDeviceTextView;
	private TextView mMessageTextView;

	private String mReceivedMessagesString = "";

	private BluetoothListener mBluetoothListener = null;
	private BluetoothInteractor mBluetoothInteractor = null;

	// Message strings
	private final String MSG_HOME_BUTTON = "home";

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

		// Set textviews
		this.mStatusTextView = (TextView) this
				.findViewById(R.id.statusTextView_mainActivity);
		this.mConnectedDeviceTextView = (TextView) this
				.findViewById(R.id.connectedDeviceTextView_mainActivity);
		this.mMessageTextView = (TextView) this
				.findViewById(R.id.messageTextView_mainActivity);

		// Start to listen a connection
		this.listenConnection();
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

	private void _updateConnectedDeviceTextView(String connectedDeviceName) {
		this.mConnectedDeviceTextView.setText(connectedDeviceName);
	}

	@SuppressLint("SimpleDateFormat")
	private void _addMessageToMessageTextView(String messageString) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String currentString = sdf.format(new Date());
		String fullMessageString = "[" + currentString + "]" + messageString;
		if (this.mReceivedMessagesString.isEmpty()) {
			this.mReceivedMessagesString = fullMessageString;
		} else {
			this.mReceivedMessagesString = fullMessageString + "\n"
					+ this.mReceivedMessagesString;
		}
		this.mMessageTextView.setText(this.mReceivedMessagesString);
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

	private void listenConnection() {
		this._updateStatusTextView("Listening...");

		this.mBluetoothListener = new BluetoothListener();
		this.mBluetoothListener.start();
	}

	// Bluetooth handlers
	private void onFailureBluetoothConnection(String message) {
		final long fRetryMS = 3000;
		final String fStatusMsg = "Connection Failure. Retrying to listen...";
		final String fDialogMsg = "Connection Failure" + ": " + message;
		final Context thisContext = this;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				DialogUtility.showDialogSimple(thisContext, fDialogMsg, "OK");
				_updateStatusTextView(fStatusMsg);
				_updateConnectedDeviceTextView("No device connected");

				Runnable runnableRetryDelayed = new Runnable() {
					@Override
					public void run() {
						listenConnection();
					}
				};
				Handler delayHandler = new Handler();
				delayHandler.postDelayed(runnableRetryDelayed, fRetryMS);
			}
		};
		runOnUiThread(runnableOnUIThread);
	}

	private void onFailureBluetoothInteraction(String message) {
		final long fRetryMS = 3000;
		final String fStatusMsg = "Interaction Failure. Retrying to listen...";
		final String fDialogMsg = fStatusMsg + ": " + message;
		final Context thisContext = this;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				DialogUtility.showDialogSimple(thisContext, fDialogMsg, "OK");
				_updateStatusTextView(fStatusMsg);
				_updateConnectedDeviceTextView("No device connected");

				Runnable runnableRetryDelayed = new Runnable() {
					@Override
					public void run() {
						listenConnection();
					}
				};
				Handler delayHandler = new Handler();
				delayHandler.postDelayed(runnableRetryDelayed, fRetryMS);
			}
		};
		runOnUiThread(runnableOnUIThread);
	}

	private void onAcceptBluetoothConnection(BluetoothSocket socket) {

		this.mBluetoothInteractor = new BluetoothInteractor(socket);
		this.mBluetoothInteractor.start();

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_updateStatusTextView("Connected");
				_updateConnectedDeviceTextView("Connected");
			}
		};
		runOnUiThread(runnableOnUIThread);// Ensure bluetooth connection or quit
											// app.
	}

	private void onReceiveBluetoothMessage(BluetoothMessage message) {
		final BluetoothMessage fMessage = message;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_addMessageToMessageTextView(fMessage.toString());
				BluetoothMessage sendMsg = new BluetoothMessage("ack "
						+ fMessage.toString());
				mBluetoothInteractor.sendMessage(sendMsg);
			}
		};
		runOnUiThread(runnableOnUIThread);
	}

	private void onDidDisconnectBluetooth() {
		final long fRetryMS = 3000;

		this.mBluetoothInteractor = null;
		this.mBluetoothListener = null;

		Runnable runnableOnUIThread = new Runnable() {
			@Override
			public void run() {
				_updateStatusTextView("Successfully disconnected. It will retry to listen after "
						+ fRetryMS / 1000 + "s...");
				_updateConnectedDeviceTextView("No device connected");

				Runnable runnableRetryDelayed = new Runnable() {
					@Override
					public void run() {
						listenConnection();
					}
				};
				Handler delayHandler = new Handler();
				delayHandler.postDelayed(runnableRetryDelayed, fRetryMS);
			}
		};
		runOnUiThread(runnableOnUIThread);
	}

	private class BluetoothListener extends Thread {
		private final UUID MY_UUID = UUID
				.fromString("00001101-0000-1000-8000-00805F9B34FB");
		private final BluetoothServerSocket mBluetoothServerSocket;

		public BluetoothListener() {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = mBluetoothAdapter
						.listenUsingInsecureRfcommWithServiceRecord(
								"TizenRemoteController", MY_UUID);
			} catch (IOException e) {
			}
			mBluetoothServerSocket = tmp;
		}

		@Override
		public void run() {
			BluetoothSocket socket = null;
			try {
				// Connect the device through the socket. This will block
				// until
				// it succeeds or throws an exception
				socket = this.mBluetoothServerSocket.accept();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					this.mBluetoothServerSocket.close();
					onFailureBluetoothConnection("Connection failed ("
							+ connectException.getMessage() + ")");
				} catch (IOException closeException) {
					onFailureBluetoothConnection("Socket close failed ("
							+ closeException.getMessage() + ")");
				}
				return;
			}

			if (this.mBluetoothServerSocket != null) {
				// Do work to manage the connection (in a separate thread)
				onAcceptBluetoothConnection(socket);
			}
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
		private final Charset charset = Charset.forName("ISO-8859-1");
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
