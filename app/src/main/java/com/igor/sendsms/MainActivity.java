package com.igor.sendsms;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.devspark.appmsg.AppMsg;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private static final int MY_PERMISSIONS_READ_CONTACTS = 1;
    private static final int PICK_CONTACT = 2;
    private static final String SMS_SENT = "smsSent";
    private static final String SMS_DELIVERED = "smsDelivered";
    private static final int MAX_SMS_MESSAGE_LENGTH = 140;
    Button sendBtn;
    EditText txtphoneNo;
    EditText txtMessage;
    String phoneNo;
    String mes;

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = null;
            String action = intent.getAction();

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = "Message sent!";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = "Error. Message not sent.";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "Error: No service.";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "Error: Null PDU.";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "Error: Radio off.";
                    break;
            }
            // Just replaced Toast.More information:http://johnkil.github.io/Android-AppMsg/

           /* AppMsg.makeText(SendMessagesWindow.this, message,
                    AppMsg.STYLE_CONFIRM).setLayoutGravity(Gravity.BOTTOM)
                    .show();*/
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(receiver, new IntentFilter(SMS_SENT));  // SMS_SENT is a constant

        sendBtn = (Button) findViewById(R.id.btnSendSMS);
        txtphoneNo = (EditText) findViewById(R.id.editText);
        txtMessage = (EditText) findViewById(R.id.editText2);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendSMSMessage();
            }
        });
        txtphoneNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readContacts();
            }
        });
    }
    private void shakeItBaby() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(250);
        }
    }

    protected void sendSMSMessage() {
        phoneNo = txtphoneNo.getText().toString();
        mes = txtMessage.getText().toString();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
        SmsManager manager = SmsManager.getDefault();

        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);

       /* if(isBinary)
        {
            byte[] data = new byte[mes.length()];

            for(int index=0; index<mes.length() && index < MAX_SMS_MESSAGE_LENGTH; ++index)
            {
                data[index] = (byte)mes.charAt(index);
            }

            manager.sendDataMessage(phoneNo, null, (short) SMS_PORT, data,piSend, piDelivered);
        }
        else
        {*/
            int length = mes.length();

            if(length > MAX_SMS_MESSAGE_LENGTH)
            {
                ArrayList<String> messagelist = manager.divideMessage(mes);

                manager.sendMultipartTextMessage(phoneNo, null, messagelist, null, null);
                shakeItBaby();
            }
            else
            {
                manager.sendTextMessage(phoneNo, null, mes, piSend, piDelivered);
                shakeItBaby();
            }
       // }
    }
    private void readContacts(){
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},MY_PERMISSIONS_READ_CONTACTS);
        }
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                String number = "";
                Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
                cursor.moveToFirst();
                String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                if (hasPhone.equals("1")) {
                    Cursor phones = getContentResolver().query
                            (ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                            + " = " + contactId, null, null);
                    while (phones.moveToNext()) {
                        number = phones.getString(phones.getColumnIndex
                                (ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[-() ]", "");
                        txtphoneNo.setText(number);
                    }
                    phones.close();
                    //Do something with number
                } else {
                    Toast.makeText(getApplicationContext(), "This contact has no phone number", Toast.LENGTH_LONG).show();
                }
                cursor.close();
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //SmsManager smsManager = SmsManager.getDefault();
                    //smsManager.sendTextMessage(phoneNo, null, mes, null, null);
                    sendSMSMessage();
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }

            }
            case MY_PERMISSIONS_READ_CONTACTS:  {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContacts();
                }
                else{
                    Toast.makeText(getApplicationContext(),
                            "Read contacts faild.", Toast.LENGTH_LONG).show();
                    return;
                }

            }
        }

    }
}