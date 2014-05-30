package jp.co.spookies.android.wifijanken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

public class WifiJankenActivity extends Activity {
	// 接続ポート
	private final int PORT = 50123;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;
	private Handler handler = new Handler();
	private SoundPool sound = null;
	private int soundId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.search);
	}

	@Override
	public void onResume() {
		super.onResume();
		sound = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		soundId = sound.load(this, R.raw.janken, 1);
	}

	@Override
	public void onPause() {
		super.onPause();
		sound.release();
		disconnect();
		finish();
	}

	public void onSearchButtonClicked(View view) {
		final EditText editText = new EditText(this);
		// デフォルト値として表示しますが、実際はホストのＩＰアドレスに書き換えて接続してもらいます。
		editText.setText("192.168.1.1");
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setView(editText);
		dialog.setPositiveButton(R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final ProgressDialog progressDialog = new ProgressDialog(
						WifiJankenActivity.this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.show();
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							String host = editText.getText().toString();
							Socket socket = new Socket(host, PORT);
							connected(socket);
							progressDialog.dismiss();
						} catch (Exception e) {
							progressDialog.cancel();
							showErrorDialogAsync(getString(R.string.not_found));
						}
					}
				};
				thread.start();
			}
		});
		dialog.setNegativeButton(R.string.cancel, null);
		dialog.show();
	}

	public void onServeButtonClicked(View view) {
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			showErrorDialog(getString(R.string.not_found));
			return;
		}
		// IPアドレスを取得
		String ip = getIpAddress();
		if (ip == null) {
			showErrorDialog(getString(R.string.wifi_not_connected));
			return;
		}
		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(ip);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					finish();
				}
			}
		});
		progressDialog.show();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					Socket socket = serverSocket.accept();
					connected(socket);
					serverSocket.close();
					progressDialog.dismiss();
				} catch (IOException e) {
					progressDialog.cancel();
				}
			}
		};
		thread.start();
	}

	public void onJankenGButtonClicked(View view) {
		janken(JankenType.G);
	}

	public void onJankenCButtonClicked(View view) {
		janken(JankenType.C);
	}

	public void onJankenPButtonClicked(View view) {
		janken(JankenType.P);
	}

	public void onResultOkButtonClicked(View view) {
		setContentView(R.layout.retry);
	}

	public void onRetryYesButtonClicked(View view) {
		setContentView(R.layout.play);
		sound.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
	}

	public void onRetryNoButtonClicked(View view) {
		this.finish();
	}

	private void connected(Socket socket) throws IOException {
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		handler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.play);
				sound.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
			}
		});
	}

	private void disconnect() {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
			}
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private void janken(final JankenType self) {
		final ProgressDialog progressDialog = new ProgressDialog(
				WifiJankenActivity.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		progressDialog.show();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					outputStream.write(self.code);
					int b = (byte) inputStream.read();
					final JankenType enemy = JankenType.getByCode(b);
					if (enemy == null) {
						throw new IOException();
					}
					handler.post(new Runnable() {
						@Override
						public void run() {
							setContentView(R.layout.result);
							findViewById(R.id.self_image)
									.setBackgroundResource(self.selfImage);
							findViewById(R.id.enemy_image)
									.setBackgroundResource(enemy.enemyImage);
							((ImageView) findViewById(R.id.result_text))
									.setImageResource(self.resultImage(enemy));
							findViewById(R.id.result_image)
									.setBackgroundResource(
											self.resultBackgroundImage(enemy));
							progressDialog.dismiss();
						}
					});
				} catch (IOException e) {
					progressDialog.cancel();
					disconnect();
					handler.post(new Runnable() {
						@Override
						public void run() {
							setContentView(R.layout.search);
							showErrorDialog(getString(R.string.disconnected));
						}
					});
				}
			}
		};
		thread.start();
	}

	private void showErrorDialog(String text) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(
				WifiJankenActivity.this);
		dialog.setTitle(getString(R.string.error));
		dialog.setMessage(text);
		dialog.setPositiveButton("OK", null);
		dialog.show();
	}

	private void showErrorDialogAsync(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				showErrorDialog(text);
			}
		});
	}

	private String getIpAddress() {
		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		int ip = manager.getConnectionInfo().getIpAddress();
		if (ip == 0) {
			return null;
		}
		return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "."
				+ ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
	}
}
