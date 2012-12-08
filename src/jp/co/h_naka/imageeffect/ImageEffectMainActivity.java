package jp.co.h_naka.imageeffect;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;

public class ImageEffectMainActivity
    extends Activity
    implements OnClickListener, OnItemSelectedListener {

    private final String PICTURES_DIRECTORY = "/sdcard/Pictures";
    private final String REGEXP = ".*jpg|.*JPG";
    private ArrayList<String> m_FileList;
    private int m_iImageCnt = 0;
    private int m_iImageMax = 0;
    private ProgressDialog m_Dialog;
    private Bitmap m_Bitmap = null;
    private Bitmap m_BitmapEffect = null;
    private int m_iFilterIndex = 0;
    private boolean m_isProcessing = false;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initWidget();
        m_FileList = searchFile(PICTURES_DIRECTORY,REGEXP,true);
        m_iImageMax = m_FileList.size();
		m_iImageCnt = getListPosition();
		if (m_iImageCnt == -1) {
			m_iImageCnt = 0;
		} else if (m_iImageCnt > m_iImageMax) {
			m_iImageCnt = 0;
		}
        changeButtonVisible();
    }

    private void initWidget() {
        final Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setOnClickListener(this);

        final Button backButton = (Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(this);

        final Button updateListButton = (Button)findViewById(R.id.updateListButton);
        updateListButton.setOnClickListener(this);

        final String [] filterArray = getResources().getStringArray(R.array.filter);
        final ArrayAdapter<String> adpter = new ArrayAdapter<String>(this,R.layout.item,filterArray);
        final Spinner spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setAdapter(adpter);
        spinner.setOnItemSelectedListener(this);
    }

    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.nextButton:
            if (m_isProcessing == false) {
                m_isProcessing = true;
                nextImage();
            }
            break;
        case R.id.backButton:
            if (m_isProcessing == false) {
                m_isProcessing = true;
                backImage();
            }
            break;
        case R.id.updateListButton:
            updateList();
            break;
        }
    }
    
    public void onItemSelected(AdapterView<?> parent,View v,int pos,long id) {
        if (m_isProcessing == false) {
            m_isProcessing = true;
            m_iFilterIndex = pos;
            drawImage();
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }
    
    private void nextImage() {
        m_iImageCnt++;
        if (m_iImageCnt >= m_iImageMax) {
            m_iImageCnt = m_iImageMax - 1;
        }
        
        changeButtonVisible();
    }

    private void backImage() {
        m_iImageCnt--;
        if (m_iImageCnt < 0) {
            m_iImageCnt = 0;
        }
        
        changeButtonVisible();
    }

    private void updateList() {
        m_FileList = searchFile(PICTURES_DIRECTORY,REGEXP,true);
        m_iImageMax = m_FileList.size();
        m_iImageCnt = 0;
		saveListPostion(m_iImageCnt);
        changeButtonVisible();
    }

    private void changeButtonVisible() {
        final Button nextButton = (Button)findViewById(R.id.nextButton);
        final Button backButton = (Button)findViewById(R.id.backButton);
        final Spinner spinner = (Spinner)findViewById(R.id.spinner);
        
        if (m_iImageMax == 0) {
            spinner.setVisibility(View.GONE);
        } else {
            spinner.setVisibility(View.VISIBLE);
        }

        if (m_iImageMax <= 1) {
            backButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
        } else if (m_iImageCnt == (m_iImageMax - 1)) {
            nextButton.setVisibility(View.GONE);
            backButton.setVisibility(View.VISIBLE);
        } else if (m_iImageCnt == 0) {
            backButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
        } else {
            nextButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }

        drawImage();
    }

    private ArrayList<String> searchFile(String dir_path,String expr,boolean search_subdir) {
        final File dir = new File(dir_path);
        ArrayList<String> fileList = new ArrayList<String>();
        
        final File[] files = dir.listFiles();
        if(null != files){
            for(int i = 0; i < files.length; ++i) {
                if(!files[i].isFile()){
                    if(search_subdir){
                        ArrayList<String> sub_files = searchFile(files[i].getPath(), expr, search_subdir);
                        fileList.addAll(sub_files);
                    }
                    continue;
                }
                
                final String filename = files[i].getName();
                if((null == expr) || filename.matches(expr)){
                    fileList.add(dir.getPath() + "/" + filename);
                }
            }
        }

        return fileList;
    }

    private void drawImage() {
        if (m_iImageMax == 0) {
            return;
        }

		saveListPostion(m_iImageCnt);
        String filepath = m_FileList.get(m_iImageCnt);
        File f = new File(filepath);
        BitmapFactory.Options bmpOp = new BitmapFactory.Options();
        m_Bitmap = null;
        m_Bitmap = BitmapFactory.decodeFile(f.getPath(), bmpOp);
        final Spinner spinner = (Spinner)findViewById(R.id.spinner);
        if (m_Bitmap == null) {
            spinner.setVisibility(View.GONE);
            String str = filepath + "のデコードに失敗しました。";
            Toast.makeText(this,str,Toast.LENGTH_LONG).show();
        } else {
            spinner.setVisibility(View.VISIBLE);
            final ImageView imageView = (ImageView)findViewById(R.id.imageView);
            imageView.setImageBitmap(m_Bitmap);
            if (m_iFilterIndex != 0) {
                effectImage();
            } else {
                m_isProcessing = false;
            }
        }
    }

    private void effectImage() {
        m_Dialog = new ProgressDialog(this);
        m_Dialog.setIndeterminate(true);
        m_Dialog.setCancelable(false);
        m_Dialog.setMessage("Processing effect....");
        m_Dialog.show();

        new EffectThread(this).start();
    }

    public Bitmap getBitmap() {
        m_BitmapEffect = null;
        m_BitmapEffect = m_Bitmap.copy(Bitmap.Config.ARGB_8888,true);
        return m_BitmapEffect;
    }

    public int getFilterIndex() {
        return m_iFilterIndex;
    }
    
    public void effectComplete() {
        runOnUiThread(new Runnable() {
            public void run() {
                m_Dialog.dismiss();
                final ImageView imageView = (ImageView)findViewById(R.id.imageView);
                imageView.setImageBitmap(m_BitmapEffect);
                m_isProcessing = false;
            }
        });
    }

	private int getListPosition() {
		SharedPreferences sp = getSharedPreferences("ImageEffect",MODE_PRIVATE);
		return sp.getInt("ListPosition",-1);
	}

	private void saveListPostion(int position) {
		SharedPreferences sp = getSharedPreferences("ImageEffect",MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt("ListPosition",position);
		editor.commit();
	}
}
