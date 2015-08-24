package com.example.testocr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;

public class ImgPretreatment {
	
	private static Bitmap img;
	private static int imgWidth;
	private static int imgHeight;
	private static int[] imgPixels;
	private static void setImgInfo(Bitmap image) {
		img = image;
		imgWidth = img.getWidth();
		imgHeight = img.getHeight();
		imgPixels = new int[imgWidth * imgHeight];
		img.getPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);
	}
	
	private static Bitmap getGrayImg() {

		int alpha = 0xFF << 24;
		double rsum=0,gsum=0,bsum=0,total=imgHeight*imgWidth;
		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int grey = imgPixels[imgWidth * i + j];
				int red = ((grey & 0x00FF0000) >> 16);
				int green = ((grey & 0x0000FF00) >> 8);
				int blue = (grey & 0x000000FF);
				rsum+=red;
				gsum+=green;
				bsum+=blue;
			}
		}
		int rmeans=(int) (rsum/total);
		int gmeans=(int) (gsum/total);
		int bmeans=(int) (bsum/total);	
		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int grey = imgPixels[imgWidth * i + j];

				int red = ((grey & 0x00FF0000) >> 16);
				int green = ((grey & 0x0000FF00) >> 8);
				int blue = (grey & 0x000000FF);
				red=(int) ((red-rmeans)*2+rmeans);
				green=(int) ((green-gmeans)*2+gmeans);
				blue=(int) ((blue-bmeans)*2+bmeans);
				grey = ( red * 299 + green * 587 + blue * 114+500)/1000;
				grey=setMinMaxValue(grey);
				grey = alpha | (grey << 16) | (grey << 8) | grey;
				imgPixels[imgWidth * i + j] = grey;
			}
		}
		Bitmap result = Bitmap
				.createBitmap(imgWidth, imgHeight, Config.RGB_565);
		result.setPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);
		return result;
	}
	
	private static void getMinMaxGrayValue(int[] p) {
		int minGrayValue = 255;
		int maxGrayValue = 0;
		for (int i = 0; i < imgHeight - 1; i++) {
			for (int j = 0; j < imgWidth - 1; j++) {
				int gray = imgPixels[i * imgWidth + imgHeight];
				if (((gray)&(0x000000FF)) < minGrayValue)
					minGrayValue = ((gray)&(0x000000FF));
				if (((gray)&(0x000000FF)) > maxGrayValue)
					maxGrayValue = ((gray)&(0x000000FF));
			}
		}
		p[0] = minGrayValue;
		p[1] = maxGrayValue;
	}
	
	private static int getIterationHresholdValue(int minGrayValue,
			int maxGrayValue) {
		int T1;
		int T2 = (maxGrayValue + minGrayValue) / 2;
		do {
			T1 = T2;
			double s = 0, l = 0, cs = 0, cl = 0;
			for (int i = 0; i < imgHeight; i++) {
				for (int j = 0; j < imgWidth; j++) {
					int gray = imgPixels[imgWidth * i + j];
					if (((gray)&(0x000000FF)) < T1) {
						s += ((gray)&(0x000000FF));
						cs++;
					}
					if (((gray)&(0x000000FF)) > T1) {
						l += ((gray)&(0x000000FF));
						cl++;
					}
				}
			}
			T2 = (int) (s / cs + l / cl) / 2;
		} while (T1 != T2);
		return T1;
	}
	
	private static Bitmap binarization(int T) {
		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int gray = imgPixels[i * imgWidth + j];
				if (((gray)&(0x000000FF)) < T) {
					imgPixels[i * imgWidth + j] = Color.rgb(0, 0, 0);
				} else {
					imgPixels[i * imgWidth + j] = Color.rgb(255, 255, 255);
				}
			}
		}

		Bitmap result = Bitmap
				.createBitmap(imgWidth, imgHeight, Config.RGB_565);
		result.setPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);

		return result;
	}
	
	private static int setMinMaxValue(int value){
		 return value > 255 ? 255 :(value < 0 ? 0 : value);
	}
	
	public static Bitmap converyToGrayImg(Bitmap img) {

		setImgInfo(img);

		return getGrayImg();
	}
	
	public static Bitmap doPretreatment(Bitmap img) {

		setImgInfo(img);

		Bitmap grayImg = getGrayImg();

		int[] p = new int[2];
		int maxGrayValue = 0, minGrayValue = 255;
		getMinMaxGrayValue(p);
		minGrayValue = p[0];
		maxGrayValue = p[1];
		int T1 = getIterationHresholdValue(minGrayValue, maxGrayValue);
		Bitmap result = binarization(T1);

		return result;
	}
}
