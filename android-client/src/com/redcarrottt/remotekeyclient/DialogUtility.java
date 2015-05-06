package com.redcarrottt.remotekeyclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtility {
	public static void killApp() {
		System.exit(0);
	}
	
	public static void showDialogSimple(Context context,
			String contentsMessage, String buttonMessage) {
		DialogInterface.OnClickListener defaultOnClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		};
		_showDialog(context, contentsMessage, buttonMessage,
				defaultOnClickListener);
	}

	public static void showDialogKillApp(Context context,
			String contentsMessage, String buttonMessage) {
		DialogInterface.OnClickListener defaultOnClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				killApp();
			}
		};
		_showDialog(context, contentsMessage, buttonMessage,
				defaultOnClickListener);
	}

	private static void _showDialog(Context context, String contentsMessage,
			String buttonMessage,
			DialogInterface.OnClickListener onClickListener) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setMessage(contentsMessage);
		alert.setPositiveButton(buttonMessage, onClickListener);
		alert.show();
	}
}
