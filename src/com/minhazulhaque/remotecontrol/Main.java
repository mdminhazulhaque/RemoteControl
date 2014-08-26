/**
    Remote Control Application
    File: Main.java

    Copyright (c) 2014 Md. Minhazul Haque <mdminhazulhaque@gmail.com>
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. 
    */
    
package com.minhazulhaque.remotecontrol;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;

public class Main extends Activity {

	Socket mSocket = null;
	boolean mConnected = false;
	float mPosX;
	float mPosY;

	float mLastTouchX;
	float mLastTouchY;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final View touchpad = (View) findViewById(R.id.touchpad);
		touchpad.setOnTouchListener(new OnTouchListener() {

			private static final int MAX_CLICK_DURATION = 200;
			private long startClickTime;
			private boolean twoFingers = false;

			@Override
			public boolean onTouch(View view, MotionEvent event) {

				if (event.getPointerCount() == 2) {
					twoFingers = true;
				}

				final int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN: {

					startClickTime = Calendar.getInstance().getTimeInMillis();

					final float x = event.getX();
					final float y = event.getY();

					mLastTouchX = x;
					mLastTouchY = y;
					break;
				}

				case MotionEvent.ACTION_MOVE: {
					final float x = event.getX();
					final float y = event.getY();

					final float dx = x - mLastTouchX;
					final float dy = y - mLastTouchY;

					mPosX += dx;
					mPosY += dy;
					mLastTouchX = x;
					mLastTouchY = y;

					sendData(dx + "," + dy);

					break;
				}

				case MotionEvent.ACTION_UP: {
					long clickDuration = (Calendar.getInstance().getTimeInMillis() - startClickTime);
					if (clickDuration < MAX_CLICK_DURATION) {
						if (twoFingers) {
							sendData("menu");
							twoFingers = false;
						} else {
							touchpad.performClick();
							sendData("click");
						}
					}
				}
					break;
				}
				return true;
			}
		});

		EditText server = (EditText) findViewById(R.id.hostaddress);
		server.setSelection(server.getText().length());

	} // onCreate

	public void onPause() {
		super.onPause();
		try {
			if (mConnected) {
				sendData("disconnect");
				mSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		finish();
	}

	public void click(View view) {
		sendData(this.getResources().getResourceEntryName(view.getId()));
	}

	private void sendData(String message) {

		if (mConnected) {
			OutputStream out;
			try {
				String eMessage = message + "|";
				out = mSocket.getOutputStream();
				out.write(eMessage.getBytes());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void connect(View view) {

		if (mConnected) {
			sendData("disconnect");
			try {
				mSocket.close();
				mConnected = false;
				Button connectButton = (Button) findViewById(R.id.connect);
				connectButton.setText(R.string.connect);
				EditText serverAddress = (EditText) findViewById(R.id.hostaddress);
				serverAddress.setEnabled(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {

			EditText serverAddress = (EditText) findViewById(R.id.hostaddress);
			String hostAddress = serverAddress.getText().toString();

			try {
				mSocket = new Socket();
				mSocket.connect(
						(new InetSocketAddress(InetAddress
								.getByName(hostAddress), 7777)), 1000);

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (mSocket.isConnected()) {
				mConnected = true;
				sendData("connect");
				Button connectButton = (Button) findViewById(R.id.connect);
				connectButton.setText(R.string.disconnect);
				serverAddress.setEnabled(false);

				showAlertDialog("Success", "Connected to server");
			} else {
				showAlertDialog("Fail", "Could not connect to server");
			}
		}
	}

	private void showAlertDialog(String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false).setTitle(title)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
