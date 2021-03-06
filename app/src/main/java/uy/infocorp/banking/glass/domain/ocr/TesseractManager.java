package uy.infocorp.banking.glass.domain.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import uy.infocorp.banking.glass.R;
import uy.infocorp.banking.glass.util.resources.Resources;

public class TesseractManager {

    private static final String TESS_DATA_PATH = Environment.getExternalStorageDirectory() +
            Resources.getString(R.string.tess_data_path);
    private static final String TESS_DATA_LANG = Resources.getString(R.string.tess_data_lang);
    private static final String TAG = TesseractManager.class.getSimpleName();

    public static String optimizeAndRecognizeText(String path) {
        Bitmap optimizedPicture = getPreparedBitmap(path);
        return recognizeText(optimizedPicture);
    }

    public static String recognizeText(Bitmap picture) {
        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(TESS_DATA_PATH, TESS_DATA_LANG);
        tessBaseAPI.setImage(picture);

        String result = tessBaseAPI.getUTF8Text();
        Log.i(TAG, "Result: [" + result + "]");
        tessBaseAPI.end();

        return result;
    }

    private static Bitmap getPreparedBitmap(String path) {
        Bitmap picture = BitmapFactory.decodeFile(path);
        Bitmap preparedPicture = BitmapModifier.addBorder(BitmapModifier.byFour(picture), 10);

        return preparedPicture;
    }
}
