package com.example.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ContactHelper {

    public static class Contact {
        public String name;
        public String phone;

        public Contact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        @Override
        public String toString() {
            return name + " (" + phone + ")";
        }
    }

    public static boolean hasContactPermission(android.content.Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static List<Contact> readContacts(ContentResolver resolver) {
        List<Contact> contacts = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String phone = cursor.getString(1);
                    if (name != null && phone != null) {
                        contacts.add(new Contact(name, phone));
                    }
                }
            }
        }
        return contacts;
    }
}
