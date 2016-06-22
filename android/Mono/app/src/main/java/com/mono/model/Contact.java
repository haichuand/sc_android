package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

import java.util.HashMap;
import java.util.Map;

/**
 * This data structure is used to store information about a specific contact.
 *
 * @author Gary Ng
 */
public class Contact implements Parcelable {

    public static final int TYPE_CONTACT = 0;
    public static final int TYPE_USER = 1;

    public static final int DEFAULT_EMAIL_KEY = 0;
    public static final int DEFAULT_PHONE_KEY = 0;

    public static final int SUGGESTION_PENDING = 1;
    public static final int SUGGESTION_IGNORED = -1;

    public long id;
    public int type;
    public boolean visible;
    public String displayName;
    public String fullName;
    public String firstName;
    public String middleName;
    public String lastName;

    public byte[] photo;

    public Map<Integer, String> emails;
    public Map<Integer, String> phones;

    public boolean isFavorite;
    public boolean isFriend;
    public int isSuggested;

    public Contact(long id) {
        this.id = id;
    }

    public Contact(long id, int type) {
        this.id = id;
        this.type = type;
    }

    protected Contact(Parcel in) {
        id = in.readLong();
        type = in.readInt();
        visible = in.readByte() != 0;
        displayName = in.readString();
        fullName = in.readString();
        firstName = in.readString();
        middleName = in.readString();
        lastName = in.readString();
        photo = in.createByteArray();
        isFavorite = in.readByte() != 0;
        isFriend = in.readByte() != 0;
        isSuggested = in.readInt();
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Contact)) {
            return false;
        }

        Contact contact = (Contact) object;

        if (id > 0 && id == contact.id && type == contact.type) {
            return true;
        }

        if (emails != null && contact.emails != null) {
            for (String email : emails.values()) {
                if (contact.emails.containsValue(email)) {
                    return true;
                }
            }
        }

        if (phones != null && contact.phones != null) {
            for (String phone : phones.values()) {
                if (contact.phones.containsValue(phone)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(type);
        dest.writeByte((byte) (visible ? 1 : 0));
        dest.writeString(displayName);
        dest.writeString(fullName);
        dest.writeString(firstName);
        dest.writeString(middleName);
        dest.writeString(lastName);
        dest.writeByteArray(photo);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeByte((byte) (isFriend ? 1 : 0));
        dest.writeInt(isSuggested);
    }

    public String[] getEmails() {
        if (emails != null) {
            return emails.values().toArray(new String[emails.size()]);
        }

        return null;
    }

    public String getEmail() {
        if (emails != null && emails.containsKey(DEFAULT_EMAIL_KEY)) {
            return emails.get(DEFAULT_EMAIL_KEY);
        }

        return null;
    }

    public void setEmail(String email) {
        if (email != null) {
            emails = new HashMap<>();
            emails.put(DEFAULT_EMAIL_KEY, email);
        } else {
            emails = null;
        }
    }

    public boolean hasEmails() {
        return emails != null && !emails.isEmpty();
    }

    public boolean containsEmail(String email) {
        return hasEmails() && emails.containsValue(email);
    }

    public String[] getPhones() {
        if (phones != null) {
            return phones.values().toArray(new String[phones.size()]);
        }

        return null;
    }

    public String[] getFormattedPhones() {
        String[] phones = getPhones();
        if (phones != null) {
            for (int i = 0; i < phones.length; i++) {
                phones[i] = Common.formatPhone(phones[i]);
            }
        }

        return phones;
    }

    public String getPhone() {
        if (phones != null && phones.containsKey(DEFAULT_PHONE_KEY)) {
            return phones.get(DEFAULT_PHONE_KEY);
        }

        return null;
    }

    public String getFormattedPhone() {
        String phone = getPhone();
        if (phone != null) {
            phone = Common.formatPhone(phone);
        }

        return phone;
    }

    public void setPhone(String phone) {
        if (phone != null) {
            phones = new HashMap<>();
            phones.put(DEFAULT_PHONE_KEY, phone);
        } else {
            phones = null;
        }
    }

    public boolean hasPhones() {
        return phones != null && !phones.isEmpty();
    }

    public boolean containsPhone(String phone) {
        return hasPhones() && phones.containsValue(phone);
    }
}
