package com.broadstar.blecarddemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.broadstar.blecardsdk.BleReader;
import com.broadstar.blecardsdk.LogicCard;
import com.broadstar.blecardsdk.ServiceManager;
import com.broadstar.blecardsdk.exception.APDUException;
import com.broadstar.blecardsdk.exception.ReaderException;
import com.daimajia.numberprogressbar.NumberProgressBar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeviceActivity extends AppCompatActivity {

	// 界面控件
	private TextView dataView;

	/**
	 * 读4442卡
	 */
	private Button bt_read;
	private EditText et_password;

	/**
	 * 校验密码
	 */
	private Button bt_checkPW;
	private EditText et_data;
	private EditText et_address;
	/**
	 * 写主存储区
	 */
	private Button bt_write;

	/**
	 * 发送APDU指令
	 */
	private Button bt_apdu;

	/**
	 * CPU卡复位
	 */
	private Button bt_reset;
	private Button bt_clear;
	private EditText protectedBlock;

	/**
	 * 读保护存储区
	 */
	private Button readProtectedBlock;

	/**
	 * 写保护存储区
	 */
	private Button writeProtectedBlock;
	private EditText secureBlock;

	/**
	 * 读安全存储区
	 */
	private Button readSecureBlock;
	private EditText secureBlockAddress;

	/**
	 * 写安全存储区
	 */
	private Button writeSecureBlock;
	private EditText protectedBlockAddress;
	private EditText et_writeProtectedBlock;
	private AlertDialog timeOutDialog;

	private LinearLayout card4442Fun;
	private LinearLayout cardCpuFun;

	private RadioButton cardType4442;
	private RadioButton cardTypeCpu;

	private Switch connection;

	private static ProgressDialog connectDialog;

	// 蓝牙卡相关操作
	private ServiceManager serviceManager;
	private BleReader reader;
	private BluetoothDevice device;

	/**
	 * 4442卡读写方法
	 */
	private LogicCard card;

	/**
	 * 电量指示
	 */
	private NumberProgressBar pb_batteryLevel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		device = getIntent().getParcelableExtra("BluetoothDevice");

		serviceManager = new ServiceManager(this, device);
		serviceManager.setCallback(new ServiceManager.Callback() {
			@Override
			public void onServiceInited(final boolean isSuccess) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (isSuccess) {
							showMsg("服务初始化成功");
							setViewsEnabled(true);
							reader = serviceManager.getBleReader();
							/**
							 * 获取电量信息
							 */
							reader.setBatteryLevelListener(new BleReader.BatteryLevelListener() {
								@Override
								public void onBatteryLevelNotified(final int batteryLevel) {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											pb_batteryLevel.setProgress(batteryLevel);
										}
									});
								}
							});
							if (cardTypeCpu.isChecked()) {
								reader.setCardType(BleReader.CARD_TYPE_CPU);
							}
							card = new LogicCard(reader);

						} else {
							showMsg("透传服务初始化失败");
						}
					}
				});
			}

			@Override
			public void onDeviceConnected() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showMsg("设备已连接");
						connectDialog.dismiss();
						connection.setChecked(true);
					}
				});
			}

			@Override
			public void onDeviceDisconnected() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showMsg("设备断开");
						serviceManager.close();
						connection.setChecked(false);
						setViewsEnabled(false);
					}
				});
			}
		});
		initView();
		// 务必在主线程调用
		serviceManager.initService();
	}

	private void initView() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(device.getName());
		}

		pb_batteryLevel = (NumberProgressBar) findViewById(R.id.battery_level);

		cardCpuFun = (LinearLayout) findViewById(R.id.cardCpu);

		bt_clear = (Button) findViewById(R.id.bt_clear);
		bt_clear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clearDisplay();
			}
		});


		card4442Fun = (LinearLayout) findViewById(R.id.card4442);

		cardType4442 = (RadioButton) findViewById(R.id.cardType4442);
		cardTypeCpu = (RadioButton) findViewById(R.id.cardTypeCpu);

		cardType4442.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					reader.setCardType(BleReader.CARD_TYPE_4442);
					bt_read.setVisibility(View.VISIBLE);
					card4442Fun.setVisibility(View.VISIBLE);
					cardCpuFun.setVisibility(View.GONE);
					bt_reset.setVisibility(View.GONE);
				}
			}
		});

		cardTypeCpu.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					reader.setCardType(BleReader.CARD_TYPE_CPU);
					bt_reset.setVisibility(View.VISIBLE);
					bt_read.setVisibility(View.GONE);
					card4442Fun.setVisibility(View.GONE);
					cardCpuFun.setVisibility(View.VISIBLE);
				}
			}
		});

		connection = new Switch(this);
		connection.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.w("Switch", "" + connection.isChecked());
				if (connection.isChecked()) { // 连接
					connection.setChecked(false);
					connectDialog.setMessage("正在连接设备");
					connectDialog.show();
					serviceManager.initService();
				} else { // 断开连接
					serviceManager.close();
					setViewsEnabled(false);
				}
			}
		});

		dataView = (TextView) findViewById(R.id.dataView);
		bt_read = (Button) findViewById(R.id.bt_read);
		et_password = (EditText) findViewById(R.id.et_password);
		bt_checkPW = (Button) findViewById(R.id.bt_checkPW);
		et_data = (EditText) findViewById(R.id.et_data);
		et_address = (EditText) findViewById(R.id.et_address);
		bt_write = (Button) findViewById(R.id.bt_write);
		bt_apdu = (Button) findViewById(R.id.bt_apdu);
		bt_reset = (Button) findViewById(R.id.bt_reset);
		bt_reset.setVisibility(View.GONE);

		et_data.addTextChangedListener(new HexTextWatcher(et_data));
		et_address.addTextChangedListener(new HexTextWatcher(et_address));
		et_password.addTextChangedListener(new HexTextWatcher(et_password));

		protectedBlock = (EditText) findViewById(R.id.protectedBlock);
		readProtectedBlock = (Button) findViewById(R.id.readProtectedBlock);
		writeProtectedBlock = (Button) findViewById(R.id.writeProtectedBlock);
		protectedBlock.addTextChangedListener(new HexTextWatcher(protectedBlock));

		secureBlock = (EditText) findViewById(R.id.secureBlock);
		readSecureBlock = (Button) findViewById(R.id.readSecureBlock);
		secureBlockAddress = (EditText) findViewById(R.id.secureBlockAddress);
		secureBlockAddress.addTextChangedListener(new HexTextWatcher(secureBlockAddress));
		secureBlock.addTextChangedListener(new HexTextWatcher(secureBlock));

		writeSecureBlock = (Button) findViewById(R.id.writeSecureBlock);
		protectedBlockAddress = (EditText) findViewById(R.id.protectedBlockAddress);
		et_writeProtectedBlock = (EditText) findViewById(R.id.et_writeProtectedBlock);
		protectedBlockAddress.addTextChangedListener(new HexTextWatcher(protectedBlockAddress));
		et_writeProtectedBlock.addTextChangedListener(new HexTextWatcher(et_writeProtectedBlock));

		readSecureBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final byte[] result = card.readSecureBlock();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									secureBlock.setText(bytesToHexString(result));
									secureBlockAddress.setText("00");
									displayToast("读取成功");
								}
							});
						} catch (APDUException | ReaderException e) {
							e.printStackTrace();
							displayToast(e.getMessage());
						}
					}
				}).start();
			}
		});

		writeSecureBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String str = secureBlock.getText().toString().replaceAll(" ", "");
				final String address = secureBlockAddress.getText().toString().replaceAll(" ", "");
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (!str.equals("")) {
							byte[] data = hexStringToBytes(str);
							try {
								int add = Integer.parseInt(address, 16);
								card.writeSecureBlock(add, data.length, data);
								displayToast("写入成功");
							} catch (APDUException | ReaderException e) {
								e.printStackTrace();
								displayToast(e.getMessage());
							}
						}
					}
				}).start();
			}
		});

		readProtectedBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final byte[] result = card.readProtectedBlock();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									protectedBlock.setText(bytesToHexString(result));
									displayToast("读取成功");
								}
							});
						} catch (APDUException | ReaderException e) {
							e.printStackTrace();
							displayToast(e.getMessage());
						}
					}
				}).start();
			}
		});

		writeProtectedBlock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String str = et_writeProtectedBlock.getText().toString().replaceAll(" ", "");
				final String address = protectedBlockAddress.getText().toString().replaceAll(" ", "");
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (!str.equals("")) {
							byte[] data = hexStringToBytes(str);
							try {
								int add = Integer.parseInt(address, 16);
								card.writeProtectedBlock(add, data.length, data);
								displayToast("写入成功");
							} catch (APDUException | ReaderException e) {
								e.printStackTrace();
								displayToast(e.getMessage());
							}
						}
					}
				}).start();

			}
		});

		bt_reset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							byte[] atr = reader.reset();
							if (atr != null)
								showMsg(bytesToHexString(atr));
						} catch (ReaderException e) {
							e.printStackTrace();
							showMsg(e.getMessage());
						}
					}
				}).start();
			}
		});

		dataView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView textView = new TextView(DeviceActivity.this);
				textView.setText(dataView.getText());
				textView.setMovementMethod(new ScrollingMovementMethod());
				new AlertDialog.Builder(DeviceActivity.this).setView(textView).create().show();
			}
		});

		bt_read.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						readData();
					}
				}).start();
			}
		});

		bt_checkPW.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String password = et_password.getText().toString().replaceAll(" ", "");
				if(password.length() != 6) {
					displayToast("密码长度错误");
					return;
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							card.checkPW(hexStringToBytes(password));
							displayToast("密码校验成功");
						} catch (ReaderException e) {
							e.printStackTrace();
							displayToast("密码校验失败: 连接错误");
							showMsg(e.getMessage());
						} catch (APDUException e) {
							e.printStackTrace();
							String sw = e.getResponse();
							if (sw.substring(0,2).equals("63")) {
								displayToast("密码校验失败: 剩余次数" + sw.charAt(3));
							} else {
								displayToast("密码校验失败: " + e.getMessage());
							}
						}
					}
				}).start();
			}
		});

		bt_write.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String address = et_address.getText().toString().replaceAll(" ", "");
				int add = 0;
				if (address.length() != 0) {
					add = Integer.parseInt(address, 16);
					if (!(add <= 255 && add >= 0)) {
						displayToast("地址输入错误");
						return;
					}
				} else {
					displayToast("地址输入错误");
					return;
				}
				final String data = et_data.getText().toString().replaceAll(" ", "");
				if (data.length() == 0 || data.length() % 2 != 0) {
					displayToast("数据长度错误");
					return;
				}
				if (data.length() / 2 + add > LogicCard.MAX_LENGTH) {
					displayToast("地址或数据长度错误");
					return;
				}
				final int finalAdd = add;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							card.writeBlock(finalAdd, data.length() / 2, hexStringToBytes(data));
							displayToast("数据写入成功");
						} catch (ReaderException | APDUException e) {
							e.printStackTrace();
							displayToast("数据写入失败: " + e.getMessage());
							showMsg(e.getMessage());
						}
					}
				}).start();

			}
		});


		View lv_apdu = getLayoutInflater().inflate(R.layout.edit_text, null);
		final EditText editText2 = (EditText) lv_apdu.findViewById(R.id.data_send);
		editText2.addTextChangedListener(new HexTextWatcher(editText2));
		final AlertDialog apduDialog = new AlertDialog.Builder(DeviceActivity.this).setTitle("请输入apdu指令").setView(lv_apdu).setPositiveButton("发送", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String data = editText2.getText().toString().replaceAll(" ", "");
				if (data.length() % 2 != 0) {
					data = "0" + data;
				}
				final String finalData = data;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							showMsg("发送：" + finalData);
							byte[] result = reader.transmit(hexStringToBytes(finalData));
							showMsg("接收：" + bytesToHexString(result));
						} catch (ReaderException e) {
							e.printStackTrace();
							displayToast(e.getMessage());
						}
					}
				}).start();
			}
		}).create();

		bt_apdu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				apduDialog.show();
			}
		});

		// 设置超时
		View lv_timeout = getLayoutInflater().inflate(R.layout.time_out, null);
		final EditText et_timeout = (EditText) lv_timeout.findViewById(R.id.time_out);

		timeOutDialog = new AlertDialog.Builder(this).setTitle("请输入超时时间").setView(lv_timeout)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					reader.setTimeout(Long.parseLong(et_timeout.getText().toString()));
				}catch (NumberFormatException ignored) {
				}
			}
		}).create();


		connectDialog = new ProgressDialog(this);
		connectDialog.setMessage("正在连接设备");
		connectDialog.show();
	}

	private void clearDisplay() {
		dataView.setText(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_device, menu);
		menu.findItem(R.id.connection).setActionView(connection);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.action_timeout:
				timeOutDialog.show();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (connection.isChecked()) {
			serviceManager.close();
		}
	}

	/**
	 * 读取逻辑卡主存储区全部数据
	 * @return 数据
	 * @throws ReaderException
	 * @throws APDUException
	 */
	public synchronized byte[] readAllBlock() throws ReaderException, APDUException {
		//分两次读取，每次读取一半
		byte[] result = new byte[LogicCard.MAX_LENGTH];
		byte[] result1 = card.readBlock(0, LogicCard.MAX_LENGTH / 2);
		byte[] result2 = card.readBlock(LogicCard.MAX_LENGTH / 2, LogicCard.MAX_LENGTH / 2);
		System.arraycopy(result1, 0, result, 0, result1.length);
		System.arraycopy(result2, 0, result, LogicCard.MAX_LENGTH / 2, result2.length);
		return result;
	}
	
	private void readData() {
		try {
			final byte[] data = readAllBlock();
			showMsg("\n" + dealData(data));
		} catch (ReaderException e) {
			displayToast("连接错误");
			showMsg(e.getMessage());
		} catch (APDUException e) {
			displayToast("读取失败");
			showMsg(e.getMessage());
		}

	}

	String dealData(byte[] data) {
		String stringData = bytesToHexString(data);
		String result = stringData.substring(0, 32);
		for(int i=1; i<16; i++) {
			result = result + "\n" + stringData.substring(i * 32, (i + 1) * 32);
		}
		return result;
	}

	//显示提示信息
	public void displayToast(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast=Toast.makeText(DeviceActivity.this, str, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP,0,220);
				toast.show();
			}
		});
	}

	private void showMsg(final CharSequence msg) {
//		final DateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]: ");
		final DateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss]: ");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dataView.append(dateFormat.format(new Date()));
				dataView.append(msg);
				dataView.append("\n");
//                if (textView.getLineCount() > 25) {
//                    textView.scrollTo(0, (textView.getLineCount() - 25) * textView.getLineHeight());
//                }
				int offset = dataView.getLineCount() * dataView.getLineHeight();
				int textHeight = dataView.getHeight() - dataView.getPaddingBottom() - dataView.getPaddingTop();
//				Log.w("textView.getHeight() :", textView.getHeight() + "");
//				Log.w(getActivity().getPackageName(), offset - textView.getHeight() + "");
//                textView.scrollTo(0, textView.getMeasuredHeight() - textView.getHeight());
				if (offset > textHeight) {
					dataView.scrollTo(0, offset - textHeight);
				}
			}
		});
	}

	private void setViewsEnabled(boolean enabled) {
		bt_checkPW.setEnabled(enabled);
		bt_read.setEnabled(enabled);
		bt_write.setEnabled(enabled);
		bt_apdu.setEnabled(enabled);
		bt_reset.setEnabled(enabled);
		readProtectedBlock.setEnabled(enabled);
		writeProtectedBlock.setEnabled(enabled);
		readSecureBlock.setEnabled(enabled);
		writeSecureBlock.setEnabled(enabled);
	}

	public class HexTextWatcher implements TextWatcher {

		private static final String TAG = "HexTextWatcher";

		private boolean mFormat;
		private boolean mInvalid;
		private int mSelection;
		private String mLastText;

		/**
		 * The editText to edit text.
		 */
		private EditText mEditText;

		/**
		 * Creates an instance of <code>HexTextWatcher</code>.
		 *
		 * @param editText
		 *        the editText to edit text.
		 */
		public HexTextWatcher(EditText editText) {
			mFormat = false;
			mInvalid = false;
			mLastText = "";
			this.mEditText = editText;
		}

		@Override
		public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			try {
				String temp = s.toString();
				// Set selection.
				if (mLastText.equals(temp)) {
					if (mInvalid) {
						mSelection -= 1;
					} else {
						if ((mSelection >= 1) && (temp.length() > mSelection - 1)
								&& (temp.charAt(mSelection - 1)) == ' ') {
							mSelection += 1;
						}
					}
					int length = mLastText.length();
					if (mSelection > length) {
						mEditText.setSelection(length);
					} else {
						mEditText.setSelection(mSelection);
					}
					mFormat = false;
					mInvalid = false;
					return;
				}

				mFormat = true;
				mSelection = start;

				// Delete operation.
				if (count == 0) {
					if ((mSelection >= 1) && (temp.length() > mSelection - 1)
							&& (temp.charAt(mSelection - 1)) == ' ') {
						mSelection -= 1;
					}
					return;
				}

				// Input operation.
				mSelection += count;
				char[] lastChar = (temp.substring(start, start + count))
						.toCharArray();
				int mid = lastChar[0];
				if (mid >= 48 && mid <= 57) {
                /* 1-9. */
				} else if (mid >= 65 && mid <= 70) {
                /* A-F. */
				} else if (mid >= 97 && mid <= 102) {
                /* a-f. */
				} else {
                /* Invalid input. */
					mInvalid = true;
					temp = temp.substring(0, start)
							+ temp.substring(start + count, temp.length());
					mEditText.setText(temp);
				}
			} catch (Exception e) {
				Log.i(TAG, e.toString());
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
			try {
            /* Format input. */
				if (mFormat) {
					StringBuilder text = new StringBuilder();
					text.append(s.toString().replace(" ", ""));
					int length = text.length();
					int sum = (length % 2 == 0) ? (length / 2) - 1 : (length / 2);
					for (int offset = 2, index = 0; index < sum; offset += 3, index++) {
						text.insert(offset, " ");
					}
					mLastText = text.toString().toUpperCase();
					mEditText.setText(text.toString().toUpperCase());
				}
			} catch (Exception e) {
				Log.i(TAG, e.toString());
			}
		}
	}

	public static byte[] hexStringToBytes(String hexString) {
		byte digest[] = new byte[hexString.length() / 2];
		for (int i = 0; i < digest.length; i++) {
			String byteString = hexString.substring(2 * i, 2 * i + 2);
			int byteValue = Integer.parseInt(byteString, 16);
			digest[i] = (byte) byteValue;
		}
		return digest;
	}

	public static String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (byte aBArray : bArray) {
			sTemp = Integer.toHexString(0xFF & aBArray);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

}
