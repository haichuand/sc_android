package com.mono.provider;

import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;

/**
 * This class stores all constants to be used in conjunction with the Contacts Provider.
 *
 * @author Gary Ng
 */
public class ContactsValues {

    public static class Contact {

        public static final String[] PROJECTION = {
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.IN_VISIBLE_GROUP,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5,
            ContactsContract.Data.DATA15,
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_MIME_TYPE = 1;
        public static final int INDEX_DISPLAY_NAME = 2;
        public static final int INDEX_VISIBLE = 3;
        public static final int INDEX_DATA1 = 4;
        public static final int INDEX_DATA2 = 5;
        public static final int INDEX_DATA3 = 6;
        public static final int INDEX_DATA4 = 7;
        public static final int INDEX_DATA5 = 8;
        public static final int INDEX_DATA15 = 9;

        public static final int INDEX_FULL_NAME = INDEX_DATA1;
        public static final int INDEX_FIRST_NAME = INDEX_DATA2;
        public static final int INDEX_MIDDLE_NAME = INDEX_DATA5;
        public static final int INDEX_LAST_NAME = INDEX_DATA3;

        public static final int INDEX_PHOTO = INDEX_DATA15;

        public static final int INDEX_EMAIL_TYPE = INDEX_DATA2;
        public static final int INDEX_EMAIL_ADDRESS = INDEX_DATA1;

        public static final int INDEX_PHONE_TYPE = INDEX_DATA2;
        public static final int INDEX_PHONE_NUMBER = INDEX_DATA1;
        public static final int INDEX_PHONE_NORMALIZED_NUMBER = INDEX_DATA4;
    }

    public static class Name {

        public static final String[] PROJECTION = {
            CommonDataKinds.StructuredName.CONTACT_ID,
            CommonDataKinds.StructuredName.IN_VISIBLE_GROUP,
            CommonDataKinds.StructuredName.DISPLAY_NAME_PRIMARY,
            CommonDataKinds.StructuredName.DISPLAY_NAME,
            CommonDataKinds.StructuredName.GIVEN_NAME,
            CommonDataKinds.StructuredName.MIDDLE_NAME,
            CommonDataKinds.StructuredName.FAMILY_NAME
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_VISIBLE = 1;
        public static final int INDEX_DISPLAY_NAME = 2;
        public static final int INDEX_FULL_NAME = 3;
        public static final int INDEX_FIRST_NAME = 4;
        public static final int INDEX_MIDDLE_NAME = 5;
        public static final int INDEX_LAST_NAME = 6;
    }

    public static class Email {

        public static final String[] PROJECTION = {
            CommonDataKinds.Email.TYPE,
            CommonDataKinds.Email.ADDRESS
        };

        public static final int INDEX_TYPE = 0;
        public static final int INDEX_EMAIL = 1;
    }

    public static class Phone {

        public static final String[] PROJECTION = {
            CommonDataKinds.Phone.TYPE,
            CommonDataKinds.Phone.NUMBER,
            CommonDataKinds.Phone.NORMALIZED_NUMBER
        };

        public static final int INDEX_TYPE = 0;
        public static final int INDEX_NUMBER = 1;
        public static final int INDEX_NORMALIZED_NUMBER = 2;
    }
}
