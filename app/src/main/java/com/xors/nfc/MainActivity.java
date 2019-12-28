package com.xors.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    private EditText editTextl;
    private TextView textView;
    private NfcUtils nfcUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NfcUtils.NfcCheck(this);
        NfcUtils.NfcInit(this);
        editTextl = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);
        nfcUtils = new NfcUtils(this);
    }

    //在onResume中开启前台调度
    @Override
    protected void onResume() {
        super.onResume();
        //设定intentfilter和tech-list。如果两个都为null就代表优先接收任何形式的TAG action。也就是说系统会主动发TAG intent。
        if (NfcUtils.mNfcAdapter != null) {
            NfcUtils.mNfcAdapter.enableForegroundDispatch(this, NfcUtils.mPendingIntent, NfcUtils.mIntentFilter, NfcUtils.mTechList);
        }
    }


    //在onNewIntent中处理由NFC设备传递过来的intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "--------------NFC-------------");
        processIntent(intent);
    }

    //  这块的processIntent() 就是处理卡中数据的方法
    private void processIntent(Intent intent) {
        //取出封装在intent中的TAG
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        for (String tech : tagFromIntent.getTechList()) {
            System.out.println(tech);
        }
        //读取TAG
        List<String> techList = Arrays.asList(tagFromIntent.getTechList());
        if (techList.contains(MifareClassic.class.getCanonicalName())){
            String metaInfo = NfcReader.readMifareClassic(tagFromIntent);
            editTextl.setText(metaInfo);
        }else if (techList.contains(NfcV.class.getCanonicalName())){
            String metaInfo = NfcReader.readNfcv(tagFromIntent);
            editTextl.setText(metaInfo);
        }
//        MifareClassic mfc = MifareClassic.get(tagFromIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (NfcUtils.mNfcAdapter != null) {
            NfcUtils.mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NfcUtils.mNfcAdapter = null;
    }



}
