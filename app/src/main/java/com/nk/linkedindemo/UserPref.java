package com.nk.linkedindemo;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPref {
    private final String NAME = "name";
    private final String HEADLINE = "headline";
    private final String EMAIL = "email";
    private final String PHOTO_URL = "photo_url";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public UserPref(Context context) {
        String userFile = "userFile";
        preferences = context.getSharedPreferences(userFile,Context.MODE_PRIVATE);
    }

    public void storeData(String firstName,String lastName,String email,String headline,String photoUrl){
        editor = preferences.edit();
        editor.putString(NAME,firstName+" "+lastName);
        editor.putString(EMAIL,email);
        editor.putString(HEADLINE,headline);
        editor.putString(PHOTO_URL,photoUrl);
        editor.apply();
    }

    public String getName(){
        return preferences.getString(NAME,"");
    }

    public void setPhotoUrl(String url){
        editor = preferences.edit();
        editor.putString(PHOTO_URL,url);
        editor.apply();
    }

    public String getHeadline(){
        return preferences.getString(HEADLINE,"");
    }


    public String getEmail(){
        return preferences.getString(EMAIL,"");
    }


    public String getPhotoUrl(){
        return preferences.getString(PHOTO_URL,"");
    }

    public void resetLogin(){
        editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}
