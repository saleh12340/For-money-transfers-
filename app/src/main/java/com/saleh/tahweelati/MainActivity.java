package com.saleh.tahweelati;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    EditText amount, recipient, recipientPhone, sender, senderPhone, search;
    TextView preview, empty;
    LinearLayout historyBox;
    DB db;
    int blue = Color.rgb(13, 71, 161), green = Color.rgb(22, 163, 74), bg = Color.rgb(245, 247, 250);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(blue);
        db = new DB(this);
        build();
        renderHistory("");
    }

    int dp(float d) {
        return (int) (d * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView tv(String s, float z, int c) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(z);
        t.setTextColor(c);
        t.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    GradientDrawable gd(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        e.setBackground(gd(Color.WHITE, 12));
        return e;
    }

    Button btn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(gd(color, 12));
        b.setMinHeight(dp(48));
        return b;
    }

    LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackground(gd(Color.WHITE, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(12));
        l.setLayoutParams(p);
        return l;
    }

    void addField(LinearLayout c, String label, EditText e) {
        c.addView(tv(label, 13, Color.DKGRAY));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.setMargins(0, dp(4), 0, dp(10));
        c.addView(e, p);
    }

    void build() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(24));
        root.setBackgroundColor(bg);
        sv.addView(root);
        setContentView(sv);

        LinearLayout head = card();
        head.setPadding(dp(18), dp(18), dp(18), dp(18));
        head.setBackground(gd(blue, 18));
        TextView title = tv("تحويلاتي", 24, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        head.addView(title);
        head.addView(tv("إنشاء رسالة تحويل وحفظ كل عملية مع التاريخ والوقت", 13, 0xDDEBFFFF));
        root.addView(head);

        LinearLayout form = card();
        amount = field("مثال: 47,400");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        recipient = field("اسم المستلم");
        recipientPhone = field("رقم المستلم");
        recipientPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        sender = field("اسم المرسل");
        senderPhone = field("رقم المرسل");
        senderPhone.setInputType(InputType.TYPE_CLASS_PHONE);

        addField(form, "المبلغ الصافي", amount);
        addField(form, "اسم المستلم", recipient);
        addField(form, "رقم المستلم", recipientPhone);
        addField(form, "اسم المرسل", sender);
        addField(form, "رقم المرسل", senderPhone);
        root.addView(form);

        LinearLayout out = card();
        out.addView(tv("الرسالة الناتجة", 17, blue));
        preview = tv("أدخل البيانات لعرض الرسالة هنا.", 16, Color.rgb(25, 35, 40));
        preview.setGravity(Gravity.RIGHT);
        preview.setPadding(dp(14), dp(14), dp(14), dp(14));
        preview.setBackground(gd(0xFFF4F8FF, 12));
        preview.setTextIsSelectable(true);
        out.addView(preview, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);
        Button copy = btn("نسخ الرسالة", blue);
        Button save = btn("حفظ العملية", green);
        Button clear = btn("مسح", 0xFF64748B);

        row.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(0, dp(48), 1);
        q.setMargins(dp(8), 0, 0, 0);
        row.addView(save, q);
        q = new LinearLayout.LayoutParams(0, dp(48), 1);
        q.setMargins(dp(8), 0, 0, 0);
        row.addView(clear, q);
        out.addView(row);
        root.addView(out);

        copy.setOnClickListener(v -> copyMessage());
        save.setOnClickListener(v -> save());
        clear.setOnClickListener(v -> {
            amount.setText("");
            recipient.setText("");
            recipientPhone.setText("");
            sender.setText("");
            senderPhone.setText("");
            update();
        });

        TextWatcher w = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { update(); }
            public void afterTextChanged(Editable e) {}
        };
        amount.addTextChangedListener(w);
        recipient.addTextChangedListener(w);
        recipientPhone.addTextChangedListener(w);
        sender.addTextChangedListener(w);
        senderPhone.addTextChangedListener(w);
        sender.setOnFocusChangeListener((v, f) -> { if (f) suggestSender(); });

        LinearLayout hist = card();
        hist.addView(tv("سجل العمليات", 17, blue));
        search = field("ابحث بالاسم أو الرقم أو المبلغ");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(48));
        sp.setMargins(0, dp(6), 0, dp(6));
        hist.addView(search, sp);
        historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        hist.addView(historyBox);
        root.addView(hist);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { renderHistory(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
    }

    void update() {
        String a = amount.getText().toString().trim();
        String r = recipient.getText().toString().trim();
        String rp = recipientPhone.getText().toString().trim();
        String s = sender.getText().toString().trim();
        String sp = senderPhone.getText().toString().trim();
        if (a.isEmpty() && r.isEmpty() && rp.isEmpty() && s.isEmpty() && sp.isEmpty()) {
            preview.setText("أدخل البيانات لعرض الرسالة هنا.");
        } else {
            preview.setText(make(a, r, rp, s, sp));
        }
    }

    String make(String a, String r, String rp, String s, String sp) {
        return (a.isEmpty() ? "" : a) + " صافي\n\nالمستلم: " + r + "\n" + rp + "\n\nالمرسل: " + s + "\n" + sp;
    }

    void copyMessage() {
        String m = preview.getText().toString();
        if (m.startsWith("أدخل البيانات")) {
            toast("أدخل البيانات أولًا");
            return;
        }
        ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("رسالة التحويل", m));
        toast("تم نسخ الرسالة");
    }

    void save() {
        String a = amount.getText().toString().trim();
        String r = recipient.getText().toString().trim();
        if (a.isEmpty() || r.isEmpty()) {
            toast("أدخل المبلغ واسم المستلم");
            return;
        }
        db.add(a, r, recipientPhone.getText().toString().trim(),
                sender.getText().toString().trim(),
                senderPhone.getText().toString().trim(),
                make(a, r, recipientPhone.getText().toString().trim(),
                        sender.getText().toString().trim(),
                        senderPhone.getText().toString().trim()),
                System.currentTimeMillis());
        renderHistory(search == null ? "" : search.getText().toString());
        toast("تم حفظ العملية");
    }

    void suggestSender() {
        String[] x = db.lastSender();
        if (sender.getText().length() == 0 && x != null) {
            sender.setText(x[0]);
            senderPhone.setText(x[1]);
        }
    }

    void renderHistory(String q) {
        historyBox.removeAllViews();
        Cursor c = db.list(q);
        if (c.getCount() == 0) {
            TextView e = tv(q.isEmpty() ? "لا توجد عمليات محفوظة بعد." : "لا توجد نتائج مطابقة.", 14, 0xFF667085);
            e.setGravity(Gravity.CENTER);
            e.setPadding(dp(8), dp(24), dp(8), dp(24));
            historyBox.addView(e);
            c.close();
            return;
        }
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            String a = c.getString(1);
            String r = c.getString(2);
            String rp = c.getString(3);
            String s = c.getString(4);
            String sp = c.getString(5);
            String text = c.getString(6);
            long tm = c.getLong(7);

            LinearLayout item = card();
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView aa = tv(a + " صافي", 17, blue);
            aa.setTypeface(null, Typeface.BOLD);
            top.addView(aa, new LinearLayout.LayoutParams(0, -2, 1));
            Button del = btn("حذف", 0xFFDC2626);
            del.setTextSize(13);
            del.setMinHeight(dp(36));
            del.setMinWidth(dp(64));
            top.addView(del, new LinearLayout.LayoutParams(dp(72), dp(36)));
            item.addView(top);

            TextView names = tv("المستلم: " + r + "\n" + rp + "\nالمرسل: " + s + "\n" + sp, 14, 0xFF27313A);
            names.setPadding(0, dp(4), 0, dp(4));
            item.addView(names);
            item.addView(tv(new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss", Locale.US).format(new Date(tm)), 12, 0xFF667085));

            item.setOnClickListener(v -> openRecord(a, r, rp, s, sp));
            del.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("حذف العملية")
                        .setMessage("هل تريد حذف هذه العملية؟")
                        .setNegativeButton("إلغاء", null)
                        .setPositiveButton("حذف", (d, w) -> {
                            db.delete(id);
                            renderHistory(search.getText().toString());
                        }).show();
            });
            historyBox.addView(item);
        }
        c.close();
    }

    void openRecord(String a, String r, String rp, String s, String sp) {
        amount.setText(a);
        recipient.setText(r);
        recipientPhone.setText(rp);
        sender.setText(s);
        senderPhone.setText(sp);
        update();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(recipient.getWindowToken(), 0);
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    static class DB extends SQLiteOpenHelper {
        DB(Context c) {
            super(c, "tahweelati.db", null, 1);
        }

        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE transfers(id INTEGER PRIMARY KEY AUTOINCREMENT,amount TEXT,recipient TEXT,recipientPhone TEXT,sender TEXT,senderPhone TEXT,message TEXT,created INTEGER)");
        }

        public void onUpgrade(SQLiteDatabase d, int o, int n) {}

        void add(String a, String r, String rp, String s, String sp, String m, long t) {
            ContentValues v = new ContentValues();
            v.put("amount", a);
            v.put("recipient", r);
            v.put("recipientPhone", rp);
            v.put("sender", s);
            v.put("senderPhone", sp);
            v.put("message", m);
            v.put("created", t);
            getWritableDatabase().insert("transfers", null, v);
        }

        Cursor list(String q) {
            String w = "";
            String[] args = null;
            if (q != null && !q.trim().isEmpty()) {
                w = "amount LIKE ? OR recipient LIKE ? OR recipientPhone LIKE ? OR sender LIKE ? OR senderPhone LIKE ?";
                String z = "%" + q.trim() + "%";
                args = new String[]{z, z, z, z, z};
            }
            return getReadableDatabase().query("transfers",
                    new String[]{"id", "amount", "recipient", "recipientPhone", "sender", "senderPhone", "message", "created"},
                    w, args, null, null, "created DESC");
        }

        void delete(long id) {
            getWritableDatabase().delete("transfers", "id=?", new String[]{String.valueOf(id)});
        }

        String[] lastSender() {
            Cursor c = getReadableDatabase().query("transfers",
                    new String[]{"sender", "senderPhone"},
                    null, null, null, null, "created DESC", "1");
            try {
                if (c.moveToFirst()) return new String[]{c.getString(0), c.getString(1)};
                return null;
            } finally {
                c.close();
            }
        }
    }
}

