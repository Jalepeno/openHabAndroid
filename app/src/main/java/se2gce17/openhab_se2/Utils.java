package se2gce17.openhab_se2;

import android.annotation.TargetApi;
import android.location.Location;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.realm.Realm;
import se2gce17.openhab_se2.models.OpenHABConfig;
import se2gce17.openhab_se2.models.OpenHABNotification;
import se2gce17.openhab_se2.models.RealmLocationWrapper;

import static android.util.Base64.DEFAULT;

/**
 * Created by Nicolaj Pedersen on 21-11-2017.
 */

public class Utils {

    public static OpenHABConfig config;

    /**
     *
     * @param loc1 current location
     * @param loc2 targeted location
     * @param distanceProx proimity in meters must be >= 0
     * @return returns 1 in locations are within proximity of eachotther, else return 0;
     */
    public static int calcLocationProximity(Location loc1, RealmLocationWrapper loc2, int distanceProx) {

        if(loc1 == null || loc2 == null){
            return -1;
        }

        Location lastLocation = new Location("");
        lastLocation.setLatitude(loc2.getLatitude());
        lastLocation.setLongitude(loc2.getLongitude());



        float distance = loc1.distanceTo(lastLocation);
        if ((int) distance <= distanceProx) {
            return 1;
        }
        return 0;
    }



//     private static final byte[] keyValue = "Beercalc12DTU123".getBytes();

    // Generates a key
    private static Key generateKey(String keyValue) {

        Key key = new SecretKeySpec(keyValue.getBytes(), "AES");
        return key;
    }


    /**
     * This function is used to encrypt data for communication with the openHAB service
     * by using a base54 encoding on a aes cipher based on the string key.
     * @param stringKey
     * @param plainText
     * @return returns encrypted string
     */
    @TargetApi(11)
    public static String encrypt(String stringKey,String plainText) {
        try {
            Cipher AesCipher = Cipher.getInstance("AES");
            AesCipher.init(Cipher.ENCRYPT_MODE, generateKey(stringKey));


            return new String(Base64.encode(AesCipher.doFinal(plainText.getBytes()), DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /**
     * This function is used to encrypt data for communication with the openHAB service
     * by using a base54 encoding on a aes cipher based on a key value available in the config file.
     * @param conf this config file contains the variables set by the user, will be used for encryption.
     * @param plainText this will be the message sent
     * @return returns encrypted string
     */
    public static String encrypt(OpenHABConfig conf,String plainText){
        return encrypt(conf.getEncryptionKey(),plainText);
    }

    /**
     * decrypts the cipher text received using the provided key value. The cipher text will be decrypted
     * first by base64 decoding followed by the eas cipher, using the provided key
     * @param keyValue
     * @param cipherText
     * @return returns decrypted string
     */
    @TargetApi(11)
    public static String decrypt(String keyValue, String cipherText) {
        if(cipherText == null || cipherText.isEmpty()){
            return "";
        }
        try {
            Cipher AesCipher;
            AesCipher = Cipher.getInstance("AES");
            AesCipher.init(Cipher.DECRYPT_MODE, generateKey(keyValue));
            System.out.println(AesCipher.doFinal(Base64.decode(cipherText.getBytes(),DEFAULT)).length);


            return new String(AesCipher.doFinal(Base64.decode(cipherText.getBytes(), DEFAULT)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /**
     * THe notifications added are compared by their timestamp. The timestamps are strings, that are
     * formatted as mySQL date format.
     * @param notif1
     * @param notif2
     * @return
     */
    public static int compareNotifications(OpenHABNotification notif1, OpenHABNotification notif2){
        if(notif1 == null && notif2 == null){
            return 0;
        }else if(notif1 == null){
            return -1;
        }else if(notif2 == null){
            return 1;
        }
        Timestamp ts1 = Timestamp.valueOf(notif1.getTimestamp());
        Timestamp ts2 = Timestamp.valueOf(notif2.getTimestamp());
        return ts1.compareTo(ts2);
    }
}
