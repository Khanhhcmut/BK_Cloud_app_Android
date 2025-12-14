package com.example.bkcloud;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;

import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean openManual = getIntent().getBooleanExtra("open_manual", false);

        if (openManual) {
            setContentView(R.layout.activity_help);

            findViewById(R.id.btnExitManual).setOnClickListener(v -> finish());
        }
        else {
            setContentView(R.layout.help_menu);

            findViewById(R.id.btnHelp1).setOnClickListener(v -> {
                getIntent().putExtra("open_manual", true);
                recreate();
            });

            findViewById(R.id.btnHelp2).setOnClickListener(v -> {
                showChangePasswordDialog();
            });
            findViewById(R.id.btnHelp3).setOnClickListener(v -> {
                showChangeQuotaDialog();
            });

            findViewById(R.id.btnExitHelp).setOnClickListener(v -> finish());
        }
    }

    private void showChangePasswordDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtMessage = view.findViewById(R.id.txtMessage);
        EditText edtOldPassword = view.findViewById(R.id.edtOldPassword);
        EditText edtNewPassword = view.findViewById(R.id.edtNewPassword);
        EditText edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        txtTitle.setText("Change Password");
        txtMessage.setText("Change user '" + HomeActivity.username + "' password:");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String oldPass = edtOldPassword.getText().toString().trim();
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(HelpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(HelpActivity.this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);

            new Thread(() -> {
                try {
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    String baseUrl = prefs.getString("swift_url", "http://192.168.1.106");

                    if (baseUrl == null || baseUrl.trim().isEmpty()) {
                        baseUrl = "http://192.168.1.106";
                    }

                    baseUrl = baseUrl.trim().replaceAll("/+$", "");
                    String authUrl = baseUrl + "/identity/v3";
                    String url = authUrl + "/users/" + HomeActivity.userId + "/password";

                    OkHttpClient client = new OkHttpClient();

                    JSONObject userObj = new JSONObject();
                    userObj.put("original_password", oldPass);
                    userObj.put("password", newPass);

                    JSONObject payload = new JSONObject();
                    payload.put("user", userObj);

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    RequestBody body = RequestBody.create(JSON, payload.toString());

                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("X-Auth-Token", HomeActivity.token)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    int code = response.code();
                    response.close();

                    if (code == 204) {
                        UserManager.updatePassword(
                                HelpActivity.this,
                                HomeActivity.username,
                                HomeActivity.project,
                                newPass
                        );

                        runOnUiThread(() -> {
                            Toast.makeText(HelpActivity.this, "Password changed successfully.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            startActivity(new Intent(HelpActivity.this, MainActivity.class));
                            finishAffinity();
                        });
                    } else if (code == 401) {
                        runOnUiThread(() -> {
                            Toast.makeText(HelpActivity.this, "Incorrect current password.", Toast.LENGTH_LONG).show();
                            btnSave.setEnabled(true);
                        });
                    } else if (code == 400) {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    HelpActivity.this,
                                    "Failed to change password.\nPossible causes:\n- Incorrect current password\n- New password was used recently and cannot be reused",
                                    Toast.LENGTH_LONG
                            ).show();
                            btnSave.setEnabled(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    HelpActivity.this,
                                    "Failed to change password: HTTP " + code,
                                    Toast.LENGTH_LONG
                            ).show();
                            btnSave.setEnabled(true);
                        });
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(HelpActivity.this, String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private void showChangeQuotaDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_change_quota, null);

        EditText edtAdminPass = view.findViewById(R.id.edtAdminPassword);
        EditText edtQuota = view.findViewById(R.id.edtQuotaGb);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {

            String adminPass = edtAdminPass.getText().toString().trim();
            String quotaStr = edtQuota.getText().toString().trim();

            if (adminPass.isEmpty() || quotaStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!adminPass.equals("change")) {
                Toast.makeText(this, "Admin password incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            double quotaGb;
            try {
                quotaGb = Double.parseDouble(quotaStr);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid quota value", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            updateQuotaToCloud(quotaGb);
        });

        dialog.show();
    }

    private byte[] encryptConfig(String json) throws Exception {

        byte[] plain = json.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = new byte[plain.length];
        for (int i = 0; i < plain.length; i++) {
            encrypted[i] = (byte) (plain[i] ^ HomeActivity.SECRET_KEY[i % HomeActivity.SECRET_KEY.length]);
        }

        return android.util.Base64.encode(encrypted, android.util.Base64.NO_WRAP);
    }

    private String decryptConfig(byte[] encryptedFile) throws Exception {

        byte[] encrypted = android.util.Base64.decode(
                encryptedFile,
                android.util.Base64.DEFAULT
        );

        byte[] decrypted = new byte[encrypted.length];
        for (int i = 0; i < encrypted.length; i++) {
            decrypted[i] = (byte) (encrypted[i] ^ HomeActivity.SECRET_KEY[i % HomeActivity.SECRET_KEY.length]);
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private void updateQuotaToCloud(double quotaGb) {

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                String url = HomeActivity.storageUrl + "/config/config.json";

                Request getReq = new Request.Builder()
                        .url(url)
                        .addHeader("X-Auth-Token", HomeActivity.token)
                        .get()
                        .build();

                Response getResp = client.newCall(getReq).execute();

                JSONObject config;

                if (getResp.code() == 404) {
                    config = new JSONObject();
                } else if (getResp.isSuccessful()) {

                    String encryptedText = getResp.body().string();
                    byte[] encryptedBytes = encryptedText.getBytes(StandardCharsets.UTF_8);

                    String json = decryptConfig(encryptedBytes);

                    config = json.trim().isEmpty()
                            ? new JSONObject()
                            : new JSONObject(json);
                } else {
                    throw new Exception("Cannot load config.json (HTTP " + getResp.code() + ")");
                }

                getResp.close();

                JSONObject users = config.optJSONObject("users");
                if (users == null) {
                    users = new JSONObject();
                    config.put("users", users);
                }

                JSONObject me = users.optJSONObject(HomeActivity.username);
                if (me == null) {
                    me = new JSONObject();
                    users.put(HomeActivity.username, me);
                }

                me.put("quota_gb", quotaGb);

                byte[] encryptedOut = encryptConfig(config.toString());

                RequestBody body = RequestBody.create(
                        MediaType.parse("text/plain"),
                        encryptedOut
                );

                Request putReq = new Request.Builder()
                        .url(url)
                        .addHeader("X-Auth-Token", HomeActivity.token)
                        .put(body)
                        .build();

                Response putResp = client.newCall(putReq).execute();
                boolean ok = putResp.isSuccessful();
                putResp.close();

                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(
                                HelpActivity.this,
                                "Storage limit updated. Please login again.",
                                Toast.LENGTH_LONG
                        ).show();

                        Intent intent = new Intent(HelpActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(
                                HelpActivity.this,
                                "Failed to update quota",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(
                                HelpActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

}
