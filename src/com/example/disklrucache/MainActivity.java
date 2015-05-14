package com.example.disklrucache;

/*
 * DisLrucache��Ҫ��������DiskLrucache.java����ӵ��Լ�����İ���
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity implements OnClickListener {
	Button button;
	Button clearButton;
	TextView textView;
	ImageView imageView;
	DiskLruCache mDiskLruCache = null;
	HttpURLConnection urlConnection = null;
	BufferedOutputStream out = null;
	BufferedInputStream in = null;
	String imageUrl = "http://www.ituc.cn/uploads/allimg/140519/1-140519134UXX.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ��׼��Open����
		try {
			File cacheDir = getDiskCacheDir(this, "bitmap");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			/*
			 * open���ĸ�����Ϊ�����Ի�����ֽ�����
			 * ����DiskLruCache��ʵ��֮�����ǾͿ��ԶԻ�������ݽ��в����ˣ�����������Ҫ����д�롢���ʡ��Ƴ���
			 */
			mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(this), 1,
					10 * 1024 * 1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		initView();

	}

	private void initView() {
		// TODO Auto-generated method stub
		button = (Button) findViewById(R.id.btn);
		button.setOnClickListener(this);
		clearButton = (Button) findViewById(R.id.clearBtn);
		clearButton.setOnClickListener(this);
		imageView = (ImageView) findViewById(R.id.image);
		textView = (TextView)findViewById(R.id.cacheText);
		//��ʾ�������ֽ���,���ֽ�Ϊ��λ
		textView.setText(Long.toString(mDiskLruCache.size()/1024));
	}

	/*
	 * ����ͼƬ�����浽Disk���߳�
	 */
	public class CacheWriteToDisk implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				String key = hashKeyForDisk(imageUrl);
				/*
				 * DiskLruCache.Editor��������������д�롢���ʡ��Ƴ���
				 */
				DiskLruCache.Editor editor = mDiskLruCache.edit(key);
				if (editor != null) {
					OutputStream outputStream = editor.newOutputStream(0);
					if (downloadUrlToStream(imageUrl, outputStream)) {
						editor.commit();
					} else {
						// ������ֹ
						editor.abort();
					}
				}
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * ��ȡ�����ַ ��SD�����ڻ���SD�����Ƴ���ʱ��,����·����etExternalCacheDir() ����:
	 * /sdcard/Android/data/<application package>/cache ���򻺴�·����getCacheDir() ���磺
	 * /data/data/<application package>/cache
	 */
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}

		// ����ֵΪƴ�Ӻ�·����File����
		return new File(cachePath + File.separator + uniqueName);
	}

	/*
	 * ��ȡ��ǰ����汾�� ���汾�Ÿı��ʱ����Ҫ�������
	 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/*
	 * ����ͼƬ������
	 * urlString:http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg
	 */
	private boolean downloadUrlToStream(String urlString,
			OutputStream outputStream) {

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(),
					8 * 1024);
			out = new BufferedOutputStream(outputStream, 8 * 1024);
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			Log.v("myu", "Dowload is true");
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		Log.v("myu", "Dowload is false");
		return false;
	}

	/*
	 * ���ַ�������MD5����1 ������Key��ͼƬ��URLһһ��Ӧ��MD5����ΪKEY
	 */
	public String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	/*
	 * ���ַ�������MD5����2 ������Key��ͼƬ��URLһһ��Ӧ��MD5����ΪKEY
	 */
	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn:
			/*
			 * ��ȡ����
			 */
				try {
					String key = hashKeyForDisk(imageUrl);
					DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
					if (snapShot != null) {
						Log.v("myu", "Image in Disk!!!");
						InputStream is = snapShot.getInputStream(0);
						Bitmap bitmap = BitmapFactory.decodeStream(is);
						// ��ȡ�����bitmap�󣬾Ϳ��Ը���UI��
						imageView.setImageBitmap(bitmap);
					} else {
						Log.v("myu", "Image is Null!!!");
						// ���Disk��û�л����Ӧ��ͼƬ���������߳�����ͼƬ�����浽Disk
						new Thread(new CacheWriteToDisk()).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			break;
		case R.id.clearBtn:
			try {
				mDiskLruCache.delete();
				Log.v("myu", "Cache is Delete!!!");
				textView.setText(Long.toString(mDiskLruCache.size()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
	}

}
