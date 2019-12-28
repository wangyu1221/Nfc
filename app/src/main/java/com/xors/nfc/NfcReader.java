package com.xors.nfc;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcV;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static android.os.Build.ID;

public class NfcReader {

    public static String readMifareClassic(Tag tagFromIntent){
        MifareClassic mfc = MifareClassic.get(tagFromIntent);
        String CardId = byteArrayToHexString(tagFromIntent.getId());
        String metaInfo = "";
        metaInfo += "卡片ID:\t" + CardId;
        boolean auth = false;
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
//            editTextl.setInputType(InputType.TYPE_NULL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metaInfo;
    }

    public static String readNfcv(Tag tagFromIntent) {
        NfcV nfcV = NfcV.get(tagFromIntent);
        String CardId = byteArrayToHexString(tagFromIntent.getId());
        String metaInfo = "";
        metaInfo += "卡片ID:\t" + CardId;
        boolean auth = false;
        try {
            //Enable I/O operations to the tag from this TagTechnology object.
            metaInfo += "\n";
            nfcV.connect();
            List<Byte> data = new LinkedList<Byte>();
            for (int i = 0; ; i++){
                byte[] block = readBlock(nfcV, i);
                if (block == null){
                    break;
                }
                for (byte b : block){
                    data.add(b);
                }
            }
            byte[] bytes = new byte[data.size()];
            for (int i = 0; i < bytes.length; i++){
                bytes[i] = data.get(i);
            }
            metaInfo = new String(bytes, "UTF-8");
//            editTextl.setInputType(InputType.TYPE_NULL);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                nfcV.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return metaInfo;
    }

    private static String bytesToHexString(byte[] src) {
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

    private static String byteArrayToHexString(byte[] inarray) {
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

    private static byte[] readBlock(NfcV nfcV, int position) throws IOException {
        byte[] ID = nfcV.getTag().getId();
        byte cmd[] = new byte[11];
        cmd[0] = (byte) 0x22;
        cmd[1] = (byte) 0x20;
        System.arraycopy(ID, 0, cmd, 2, ID.length); // UID
        cmd[10] = (byte) position;
        byte res[] = nfcV.transceive(cmd);
        if(res[0] == 0x00){
            byte block[] = new byte[res.length - 1];
            System.arraycopy(res, 1, block, 0, res.length - 1);
            return block;
        }
        return null;
    }

    public static String readOneBlock(NfcV nfcV, int position) throws IOException {
        return new String(readBlock(nfcV, position), "UTF-8");
//        return bytesToHexString(readBlock(nfcV, position));
    }
}
