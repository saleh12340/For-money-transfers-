package com.saleh.tahweelati;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "Tahweelati";
    private WebView webView;
    private static final int REQUEST_PICK_RECIPIENT_CONTACT = 101;
    private static final int REQUEST_PICK_SENDER_CONTACT = 102;
    private static final int PERMISSION_REQUEST_CONTACTS = 201;

    private String pendingTargetField = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#0f172a"));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        webView = new WebView(this);
        setContentView(webView);

        // Pre-create cache directories to prevent Chromium code cache warnings
        try {
            java.io.File codeCacheDir = new java.io.File(getCacheDir(), "WebView/Default/HTTP Cache/Code Cache/js");
            if (!codeCacheDir.exists()) {
                codeCacheDir.mkdirs();
            }
            java.io.File wasmCacheDir = new java.io.File(getCacheDir(), "WebView/Default/HTTP Cache/Code Cache/wasm");
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs();
            }
        } catch (Exception ignored) {}

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("whatsapp:") || url.contains("api.whatsapp.com") || url.contains("wa.me"))) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                if (url != null && (url.startsWith("tel:") || url.startsWith("mailto:"))) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void launchContactPicker(String targetField) {
        try {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            int requestCode = "recipient".equals(targetField) ? REQUEST_PICK_RECIPIENT_CONTACT : REQUEST_PICK_SENDER_CONTACT;
            startActivityForResult(pickIntent, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Error launching contact picker", e);
            Toast.makeText(MainActivity.this, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestContactPermission(String targetField) {
        pendingTargetField = targetField;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_CONTACTS);
                return;
            }
        }
        launchContactPicker(targetField);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingTargetField != null) {
                    launchContactPicker(pendingTargetField);
                    pendingTargetField = null;
                }
            } else {
                Toast.makeText(this, "يرجى منح إذن جهات الاتصال لاستيراد الأرقام", Toast.LENGTH_LONG).show();
                pendingTargetField = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri contactUri = data.getData();
            String name = "";
            String phone = "";

            Cursor cursor = null;
            try {
                String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                };
                cursor = getContentResolver().query(contactUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    if (phoneIndex != -1) {
                        phone = cursor.getString(phoneIndex);
                    }
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed reading with projection: " + contactUri, e);
            } finally {
                if (cursor != null) cursor.close();
            }

            // Fallback general query if phone not resolved
            if (phone == null || phone.trim().isEmpty()) {
                try {
                    cursor = getContentResolver().query(contactUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            String colName = cursor.getColumnName(i).toLowerCase();
                            if (colName.contains("data1") || colName.contains("number") || colName.contains("phone")) {
                                String val = cursor.getString(i);
                                if (val != null && val.matches(".*[0-9].*")) {
                                    phone = val;
                                    break;
                                }
                            }
                        }
                        if (name == null || name.trim().isEmpty()) {
                            int nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                            if (nameIdx != -1) {
                                name = cursor.getString(nameIdx);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Fallback reading failed", e);
                } finally {
                    if (cursor != null) cursor.close();
                }
            }

            final String targetField = (requestCode == REQUEST_PICK_RECIPIENT_CONTACT) ? "recipient" : "sender";
            final String safeName = (name != null) ? name.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").trim() : "";
            final String safePhone = (phone != null) ? phone.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").trim() : "";

            if (safePhone.isEmpty()) {
                Toast.makeText(this, "تعذر قراءة رقم الهاتف من جهة الاتصال", Toast.LENGTH_SHORT).show();
            }

            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("if(window.onContactSelected){ window.onContactSelected('" + targetField + "', '" + safeName + "', '" + safePhone + "'); }", null);
                }
            });
        }
    }

    public class WebAppInterface {
        Context context;

        WebAppInterface(Context c) {
            context = c;
        }

        @JavascriptInterface
        public void pickContact(final String targetField) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkAndRequestContactPermission(targetField);
                }
            });
        }

        @JavascriptInterface
        public String getClipboardText() {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                return text != null ? text.toString() : "";
            }
            return "";
        }

        @JavascriptInterface
        public void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("transfer", text);
                clipboard.setPrimaryClip(clip);
            }
        }

        @JavascriptInterface
        public void openWhatsApp(String phone, String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String cleanPhone = phone.replaceAll("[^0-9]", "");
                        String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        intent.setPackage("com.whatsapp");
                        try {
                            startActivity(intent);
                        } catch (Exception e1) {
                            try {
                                intent.setPackage("com.whatsapp.w4b");
                                startActivity(intent);
                            } catch (Exception e2) {
                                Intent genericIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(genericIntent);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
