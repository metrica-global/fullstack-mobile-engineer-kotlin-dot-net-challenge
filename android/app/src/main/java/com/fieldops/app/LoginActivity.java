package com.fieldops.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fieldops.app.data.local.SessionManager;
import com.fieldops.app.ui.orders.OrdersActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    EditText txtUser;
    EditText txtPass;
    Button btnLogin;
    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SessionManager.INSTANCE.init(getApplicationContext());

        txtUser = findViewById(R.id.txtUser);
        txtPass = findViewById(R.id.txtPass);
        btnLogin = findViewById(R.id.btnLogin);

        // si ya hay sesion protegida en Keystore entra directo
        if (SessionManager.INSTANCE.hasActiveSession()) {
            startActivity(new Intent(this, OrdersActivity.class));
            finish();
            return;
        }

        // recordar solo el ultimo usuario ingresado (nunca la clave)
        txtUser.setText(SessionManager.INSTANCE.getLastUsername());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = txtUser.getText().toString().trim();
                String p = txtPass.getText().toString();
                if (u.isEmpty() || p.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Ingrese usuario y clave", Toast.LENGTH_SHORT).show();
                    return;
        }
                new LoginTask().execute(u, p);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        super.onDestroy();
    }

    class LoginTask extends AsyncTask<String, Void, String> {

        String user;
        String pass;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(LoginActivity.this, "", "Ingresando...");
        }

        @Override
        protected String doInBackground(String... params) {
            user = params[0];
            pass = params[1];
            HttpURLConnection con = null;
            try {
                URL url = new URL(Session.BASE_URL + "/api/auth/login");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("username", user);
                payload.put("password", pass);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                InputStream stream = con.getResponseCode() >= 400
                        ? con.getErrorStream()
                        : con.getInputStream();

                if (stream == null) {
                    return null;
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
            try {
                if (result == null) {
                    Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject json = new JSONObject(result);
                if (json.optBoolean("ok", false)) {
                    String token = json.getString("token");
                    String refreshToken = json.optString("refreshToken", "");
                    JSONObject u = json.getJSONArray("user").getJSONObject(0);

                    SessionManager.INSTANCE.saveSession(
                            token,
                            refreshToken,
                            u.getInt("id"),
                            u.getString("fullName"),
                            user
                    );

                    startActivity(new Intent(LoginActivity.this, OrdersActivity.class));
                    finish();
                } else {
                    Toast.makeText(
                            LoginActivity.this,
                            json.optString("message", "Usuario o clave incorrectos"),
                            Toast.LENGTH_LONG
                    ).show();
                }
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
