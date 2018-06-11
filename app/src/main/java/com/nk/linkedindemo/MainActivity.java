package com.nk.linkedindemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.DeepLinkHelper;
import com.linkedin.platform.LISession;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.errors.LIDeepLinkError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.listeners.DeepLinkListener;
import com.linkedin.platform.utils.Scope;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.login)
    Button login;
    @BindView(R.id.profileLayout)
    LinearLayout profileLayout;

    private UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getHashKey();
        userPref = new UserPref(this);
        LISession liSession = LISessionManager.getInstance(this).getSession();

        if (liSession != null) {
            login.setVisibility(View.GONE);
            profileLayout.setVisibility(View.VISIBLE);
            showUserProfile();
        }

    }

    @SuppressLint("CheckResult")
    private void showUserProfile() {

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.circleCrop();
        requestOptions.placeholder(R.drawable.ic_launcher);

        name.setText("Name : " + userPref.getName() + "\nHeadline : " + userPref.getHeadline() + "\nEmail Id : " + userPref.getEmail());
        Glide.with(this).load(userPref.getPhotoUrl())
                .apply(requestOptions)
                .into(image);
    }

    @OnClick(R.id.myProfile)
    public void openMyProfile() {
        DeepLinkHelper deepLinkHelper = DeepLinkHelper.getInstance();
        deepLinkHelper.openCurrentProfile(this, new DeepLinkListener() {
            @Override
            public void onDeepLinkSuccess() {
                Toast.makeText(MainActivity.this, "Current profile opened successfully.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeepLinkError(LIDeepLinkError error) {
                Toast.makeText(MainActivity.this, "Failed to open current profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.logout)
    protected void onLogout() {
        LISessionManager.getInstance(this).clearSession();
        profileLayout.setVisibility(View.GONE);
        login.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.login)
    public void setupLinkIn() {
        LISessionManager.getInstance(getApplicationContext()).init(this, buildScope()
                , new AuthListener() {
                    @Override
                    public void onAuthSuccess() {
                        Toast.makeText(MainActivity.this, "Successfully authenticated with LinkedIn.", Toast.LENGTH_SHORT).show();
                        fetchBasicProfileData();
                    }

                    @Override
                    public void onAuthError(LIAuthError error) {
                        Log.e(TAG, "Auth Error :" + error.toString());
                        Toast.makeText(MainActivity.this, "Failed to authenticate with LinkedIn. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }, true);
    }


    private void fetchBasicProfileData() {

        String url = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,headline,public-profile-url,picture-url,email-address,picture-urls::(original))";
        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
        apiHelper.getRequest(this, url, new ApiListener() {
            @Override
            public void onApiSuccess(ApiResponse apiResponse) {
                Log.d(TAG, "API Res : " + apiResponse.getResponseDataAsString() + "\n" + apiResponse.getResponseDataAsJson().toString());
                updateUI(apiResponse);
            }

            @Override
            public void onApiError(LIApiError liApiError) {
                Log.e(TAG, "Fetch profile Error   :" + liApiError.getLocalizedMessage());
            }
        });
    }

    @SuppressLint("CheckResult")
    private void updateUI(ApiResponse apiResponse) {
        try {

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.circleCrop();
            requestOptions.placeholder(R.drawable.ic_launcher);

            if (apiResponse != null) {
                JSONObject jsonObject = apiResponse.getResponseDataAsJson();
                String firstName = jsonObject.getString("firstName");
                String lastName = jsonObject.getString("lastName");
                String headline = jsonObject.getString("headline");
                String email = jsonObject.getString("emailAddress");
                name.setText("Name : " + firstName + " " + lastName + "\nHeadline : " + headline + "\nEmail Id : " + email);
                String smallPicture = jsonObject.getString("pictureUrl");

                userPref.storeData(firstName,lastName,email,headline,smallPicture);

                JSONObject pictureURLObject = jsonObject.getJSONObject("pictureUrls");
                if (pictureURLObject.getInt("_total") > 0) {
                    JSONArray profilePictureURLArray = pictureURLObject.getJSONArray("values");
                    if (profilePictureURLArray != null && profilePictureURLArray.length() > 0) {


                        Glide.with(this).load(profilePictureURLArray.getString(0))
                                .apply(requestOptions)
                                .into(image);

                        userPref.setPhotoUrl(profilePictureURLArray.getString(0));
                    }
                } else {
                    Glide.with(this).load(smallPicture)
                            .apply(requestOptions)
                            .into(image);

                }


                profileLayout.setVisibility(View.VISIBLE);
                login.setVisibility(View.GONE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnClick(R.id.share)
    public void onShare() {
        String url = "https://api.linkedin.com/v1/people/~/shares";

        JSONObject body = null;
        try {
            body = new JSONObject("{" + "\"comment\": \"Test Comment\"," +
                    "\"visibility\": { \"code\": \"anyone\" }," +
                    "\"content\": { " +
                    "\"title\": \"Test share\"," +
                    "\"description\": \"Testing the mobile SDK share feature!\"," +
                    "\"submitted-url\": \"http://www.technitab.in/\"," +
                    "\"submitted-image-url\": \"http://http://www.technitab.in\"" +
                    "}" + "}");
        } catch (JSONException e) {
            e.printStackTrace();

        }

        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
        apiHelper.postRequest(this, url, body, new ApiListener() {
            @Override
            public void onApiSuccess(ApiResponse apiResponse) {
                Toast.makeText(MainActivity.this, "Shared successfully.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onApiError(LIApiError liApiError) {
                Toast.makeText(MainActivity.this, "Failed to share. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);

        DeepLinkHelper deepLinkHelper = DeepLinkHelper.getInstance();
        deepLinkHelper.onActivityResult(this, requestCode, resultCode, data);
    }

    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE, Scope.R_EMAILADDRESS, Scope.W_SHARE);
    }


    @SuppressLint("PackageManagerGetSignatures")
    private void getHashKey() {
        try {

            PackageInfo info = getPackageManager().getPackageInfo(
                    "in.technitab.fitmode",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(TAG, "Hash  : " + Base64.encodeToString(md.digest(), Base64.NO_WRAP));//Key hash is printing in Log
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            //
        }
    }
}
