package com.example.testocr;

import java.io.File;
import java.io.FileNotFoundException;








import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	
	private static final int PHOTO_CAPTURE = 0x11;
	private static final int PHOTO_RESULT = 0x12;
	private static String LANGUAGE="eng";
	private static String PATH=getEsdPath()+"/ocrtest";
	
	private static EditText result;
	private static ImageView oiv;
	private static ImageView div;
	private static Button camera;
	private static Button select;
	private static Button okk;
	private static RadioGroup radiogroup;
	private static CheckBox check;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;

	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (textResult.equals(""))
					result.setText(R.string.p_result);
				else
					
					result.setText(textResult);
				break;
			case SHOWTREATEDIMG:
				showPicture(div, bitmapTreated);
				break;
			}
			super.handleMessage(msg);
		}

	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		File path = new File(PATH);
		if (!path.exists()) {
			path.mkdirs();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		result=(EditText) findViewById(R.id.result);
		oiv=(ImageView) findViewById(R.id.oiv);
		div=(ImageView) findViewById(R.id.div);
		camera=(Button) findViewById(R.id.camera);
		select=(Button) findViewById(R.id.select);
		okk=(Button) findViewById(R.id.okk);
		radiogroup=(RadioGroup) findViewById(R.id.radiogrop);
		check=(CheckBox) findViewById(R.id.check);
		
		camera.setOnClickListener(this);
		select.setOnClickListener(this);
		okk.setOnClickListener(this);
		radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			
			@Override
			public void onCheckedChanged(RadioGroup group,int checkId){
				switch(checkId){
				case R.id.english:
					LANGUAGE="eng";
					break;
				case R.id.chinese:
					LANGUAGE="chi_sim";
					break;
				}
			}
		});
		
		check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if ((bitmapSelected != null) && (bitmapTreated!= null)){
					picturePretreatment();
				}
			}
		});
		
			
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		bitmapSelected=null;
		bitmapTreated=null;
	}
	
	@Override
	public void onClick(View v){
		switch(v.getId()){
		case R.id.camera:
			Intent intent2=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent2.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(PATH,"temp.jpg")));
			startActivityForResult(intent2, PHOTO_CAPTURE);
			break;
		case R.id.select:
			Intent intent = new Intent(Intent.ACTION_PICK, null);  
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,  
                    "image/*");
			startActivityForResult(intent, 1);
			break;
		case R.id.okk:
			if ((bitmapSelected != null) && (bitmapTreated!= null)){
				result.setText(R.string.wait);
				new Thread(new Runnable() {
					@Override
					public void run() {
						textResult = doOcr(bitmapTreated, LANGUAGE);
						Message msg = new Message();
						msg.what = SHOWRESULT;
						myHandler.sendMessage(msg);
					}
				}).start();
			}
			else{
				result.setText(R.string.none);
			}
			break;
		}	
	}
			
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_CANCELED)
			return;

		if (requestCode == PHOTO_CAPTURE) {
			startPhotoCrop(Uri.fromFile(new File(PATH, "temp.jpg")));
		}
		
		if (requestCode == 1) {
			startPhotoCrop(data.getData());
		}
		
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(PATH,
					"temp_cropped.jpg")));
			showPicture(oiv, bitmapSelected);
			
			picturePretreatment();

		}

		super.onActivityResult(requestCode, resultCode, data);
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
	
	public static void showPicture(ImageView iv, Bitmap bmp){
		iv.setImageBitmap(bmp);
	}
	
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getEsdPath(), language);

		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}
	
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); 
		startActivityForResult(intent, PHOTO_RESULT);
	}
	
	public static String getEsdPath(){
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();
		}
		return sdDir.toString();
	}
	
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
	
	private void picturePretreatment(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (check.isChecked()) {
					bitmapTreated = ImgPretreatment
							.doPretreatment(bitmapSelected);
					Message msg = new Message();
					msg.what = SHOWTREATEDIMG;
					myHandler.sendMessage(msg);
				} else {
					bitmapTreated = ImgPretreatment
							.converyToGrayImg(bitmapSelected);
					Message msg = new Message();
					msg.what = SHOWTREATEDIMG;
					myHandler.sendMessage(msg);
				}
			}

		}).start();
	}
	
}
