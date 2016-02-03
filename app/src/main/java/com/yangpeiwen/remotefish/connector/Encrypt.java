package com.yangpeiwen.remotefish.connector;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

/**
 * Created by ypw
 * on 2016-01-25 上午10:40.
 */
public class Encrypt {
    public static int getCRC16(byte[] bytes) {
        int[] table = {0x0000, 0x8005, 0x800F, 0x000A, 0x801B, 0x001E, 0x0014,
                0x8011, 0x8033, 0x0036, 0x003C, 0x8039, 0x0028, 0x802D, 0x8027,
                0x0022, 0x8063, 0x0066, 0x006C, 0x8069, 0x0078, 0x807D, 0x8077,
                0x0072, 0x0050, 0x8055, 0x805F, 0x005A, 0x804B, 0x004E, 0x0044,
                0x8041, 0x80C3, 0x00C6, 0x00CC, 0x80C9, 0x00D8, 0x80DD, 0x80D7,
                0x00D2, 0x00F0, 0x80F5, 0x80FF, 0x00FA, 0x80EB, 0x00EE, 0x00E4,
                0x80E1, 0x00A0, 0x80A5, 0x80AF, 0x00AA, 0x80BB, 0x00BE, 0x00B4,
                0x80B1, 0x8093, 0x0096, 0x009C, 0x8099, 0x0088, 0x808D, 0x8087,
                0x0082, 0x8183, 0x0186, 0x018C, 0x8189, 0x0198, 0x819D, 0x8197,
                0x0192, 0x01B0, 0x81B5, 0x81BF, 0x01BA, 0x81AB, 0x01AE, 0x01A4,
                0x81A1, 0x01E0, 0x81E5, 0x81EF, 0x01EA, 0x81FB, 0x01FE, 0x01F4,
                0x81F1, 0x81D3, 0x01D6, 0x01DC, 0x81D9, 0x01C8, 0x81CD, 0x81C7,
                0x01C2, 0x0140, 0x8145, 0x814F, 0x014A, 0x815B, 0x015E, 0x0154,
                0x8151, 0x8173, 0x0176, 0x017C, 0x8179, 0x0168, 0x816D, 0x8167,
                0x0162, 0x8123, 0x0126, 0x012C, 0x8129, 0x0138, 0x813D, 0x8137,
                0x0132, 0x0110, 0x8115, 0x811F, 0x011A, 0x810B, 0x010E, 0x0104,
                0x8101, 0x8303, 0x0306, 0x030C, 0x8309, 0x0318, 0x831D, 0x8317,
                0x0312, 0x0330, 0x8335, 0x833F, 0x033A, 0x832B, 0x032E, 0x0324,
                0x8321, 0x0360, 0x8365, 0x836F, 0x036A, 0x837B, 0x037E, 0x0374,
                0x8371, 0x8353, 0x0356, 0x035C, 0x8359, 0x0348, 0x834D, 0x8347,
                0x0342, 0x03C0, 0x83C5, 0x83CF, 0x03CA, 0x83DB, 0x03DE, 0x03D4,
                0x83D1, 0x83F3, 0x03F6, 0x03FC, 0x83F9, 0x03E8, 0x83ED, 0x83E7,
                0x03E2, 0x83A3, 0x03A6, 0x03AC, 0x83A9, 0x03B8, 0x83BD, 0x83B7,
                0x03B2, 0x0390, 0x8395, 0x839F, 0x039A, 0x838B, 0x038E, 0x0384,
                0x8381, 0x0280, 0x8285, 0x828F, 0x028A, 0x829B, 0x029E, 0x0294,
                0x8291, 0x82B3, 0x02B6, 0x02BC, 0x82B9, 0x02A8, 0x82AD, 0x82A7,
                0x02A2, 0x82E3, 0x02E6, 0x02EC, 0x82E9, 0x02F8, 0x82FD, 0x82F7,
                0x02F2, 0x02D0, 0x82D5, 0x82DF, 0x02DA, 0x82CB, 0x02CE, 0x02C4,
                0x82C1, 0x8243, 0x0246, 0x024C, 0x8249, 0x0258, 0x825D, 0x8257,
                0x0252, 0x0270, 0x8275, 0x827F, 0x027A, 0x826B, 0x026E, 0x0264,
                0x8261, 0x0220, 0x8225, 0x822F, 0x022A, 0x823B, 0x023E, 0x0234,
                0x8231, 0x8213, 0x0216, 0x021C, 0x8219, 0x0208, 0x820D, 0x8207,
                0x0202};
        int i = 0;
        int len = bytes.length;
        int crc = 0;
        while (i < len) {
            int index = (crc >> 8) ^ bytes[i++];
            if (index < 0)
                index += 256;
            crc = ((crc & 0xFF) << 8) ^ table[index];
        }
        return crc;
    }

    public static void generateKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.genKeyPair();
            System.out.println(kp.getPublic());
            System.out.println(kp.getPrivate());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PublicKey getPublicKey() {
        PublicKey publicKey = null;
        String modulus = "B88E852FAB8C73E49FD49C2AFAF77C7C15934C591E963FCC6C4B926C7561D26F1D1B87355464C554AA36E329A3F28C2BAB72C4FE0EBF35AD09AAB921AFE89944A09FA30E27F1868CE882AA2E535C1D5F1E6B7FB313A9ACDAF72BDFBC6CB1438BAFCC6294C9A5DB334AC9251E37DDE397B03E70C737A3E1A5445791F6810A101D475BD10BE3A7211C72A39F0EE719682D60486BCE0DFB9F10FD435598FDD92D63F8E3155D0D2892203252B9DC130889EEC89175D0FA2830DB92CF0F07731E4EFAD17F5B59480EF6E637B87D0AC88B44A824A89D9B66CA03D7A548B5E1CAF2E9614281C0F27C87295FEA99EC243022367FED993A782692FD0B77875745E0816E9D";
        String publicExponent = "10001";
        BigInteger m = new BigInteger(modulus, 16);
        BigInteger e = new BigInteger(publicExponent, 16);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return publicKey;
    }

    static PublicKey publicKey = getPublicKey();

    public static byte[] encrypt(byte[] data) {
        Cipher cipher;
        byte[] buf = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            buf = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf;
    }

    public static byte[] decrypt(byte[] data) {
        Cipher cipher;
        byte[] buf = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            buf = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf;
    }

    public static byte[] escape(byte[] buf) {
        byte[] rec = new byte[1024];
        int i, j = 0;

        for (i = 0; i < buf.length; i++) {
            if ((buf[i] == 0xAA) | (buf[i] == 0xBB) | (buf[i] == 0x7D) & (i != 0)
                    & (i != buf.length - 1)) {
                rec[j++] = 0x7D;
                rec[j++] = (byte) (buf[i] ^ 0x40);
            } else {
                rec[j++] = buf[i];
            }
        }

        byte[] rec2 = new byte[j];
        for (i = 0; i < j; i++)
            rec2[i] = rec[i];
        return rec2;
    }

    public static byte[] unescape(byte[] buf) {
        byte[] rec = new byte[1024];
        int i, j = 0;

        for (i = 0; i < buf.length; i++) {
            if (buf[i] == 0x7D) {
                rec[j++] = (byte) (buf[++i] ^ 0x40);
            } else
                rec[j++] = buf[i];
        }

        byte[] rec2 = new byte[j];
        for (i = 0; i < j; i++)
            rec2[i] = rec[i];
        return rec2;
    }

    public static byte[] package_tcp(byte[] buf) {
        byte[] rec = new byte[buf.length + 2];
        rec[0] = (byte) 0xAA;
        System.arraycopy(buf, 0, rec, 1, buf.length);
        rec[buf.length + 1] = (byte) 0xBB;
        return rec;
    }

    public static byte[] unpackage (byte[] buf){
        byte[] rec = new byte[256];
        System.arraycopy(buf, 1, rec, 0, 256);
        return rec;
    }

}
