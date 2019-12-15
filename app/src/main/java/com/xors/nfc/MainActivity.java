package com.xors.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        String CardId = byteArrayToHexString(tagFromIntent.getId());
        String metaInfo = "";
        metaInfo += "卡片ID:\t" + CardId;
        for (String tech : tagFromIntent.getTechList()) {
            System.out.println(tech);
        }
        boolean auth = false;
        //读取TAG
        MifareClassic mfc = MifareClassic.get(tagFromIntent);
//        MifareClassic mfc = MifareClassic.get(tagFromIntent);
        try {
            //Enable I/O operations to the tag from this TagTechnology object.
            mfc.connect();
            int type = mfc.getType();//获取TAG的类型
            int sectorCount = mfc.getSectorCount();//获取TAG中包含的扇区数
            String typeS = "";
            switch (type) {
                case MifareClassic.TYPE_CLASSIC:
                    typeS = "TYPE_CLASSIC";
                    break;
                case MifareClassic.TYPE_PLUS:
                    typeS = "TYPE_PLUS";
                    break;
                case MifareClassic.TYPE_PRO:
                    typeS = "TYPE_PRO";
                    break;
                case MifareClassic.TYPE_UNKNOWN:
                    typeS = "TYPE_UNKNOWN";
                    break;
            }
            metaInfo += "\n卡片类型：\t" + typeS + "\n共" + sectorCount + "个扇区\n共" + mfc.getBlockCount() + "个块\n存储空间: " + mfc.getSize() + "B\n";
            metaInfo += "\n卡片类型：\t" + intent.getAction() + "\n";
            metaInfo += "\n卡片类型：\t" + Arrays.toString(tagFromIntent.getTechList()) + "\n";
            for (int j = 0; j < sectorCount; j++) {
                //Authenticate a sector with key A.
                String keyA = null;
                auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
                keyA = "KEY_DEFAULT";
                if (!auth){
                    auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
                    keyA = "KEY_MIFARE_APPLICATION_DIRECTORY";
                }
                if (!auth){
                    auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_NFC_FORUM);
                    keyA = "KEY_NFC_FORUM";
                }
//                if (!auth){
//                    auth = mfc.authenticateSectorWithKeyB(j, MifareClassic.KEY_DEFAULT);
//                }
//                if (!auth){
//                    auth = mfc.authenticateSectorWithKeyB(j, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
//                }
//                if (!auth){
//                    auth = mfc.authenticateSectorWithKeyB(j, MifareClassic.KEY_NFC_FORUM);
//                }


                int bCount;
                int bIndex;
                if (auth) {
                    metaInfo += "Sector " + j + ":验证成功, key=" + keyA + "\n";
                    // 读取扇区中的块
                    bCount = mfc.getBlockCountInSector(j);
                    bIndex = mfc.sectorToBlock(j);
                    for (int i = 0; i < bCount; i++) {
                        byte[] data = mfc.readBlock(bIndex);
                        metaInfo += "Block " + bIndex + " : " + bytesToHexString(data) + "\n";
                        bIndex++;
                    }
                } else {
                    metaInfo += "Sector " + j + ":验证失败\n";
                }
            }
            editTextl.setText(metaInfo);
//            editTextl.setInputType(InputType.TYPE_NULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer).append(" ");
        }
        return stringBuilder.toString();
    }

    private String byteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
                "B", "C", "D", "E", "F"};
        String out = "";
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
            out += " ";
        }
        return out;
    }
}
