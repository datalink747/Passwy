/*
 * Copyright 2015 Jan-Lukas Else
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jlelse.passwy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.wang.avi.AVLoadingIndicatorView;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class Passwy extends AppCompatActivity implements View.OnClickListener {

    EditText phrase1, phrase2, pwLength;
    CheckBox specChars;
    AVLoadingIndicatorView loadingIndicator;
    Button btnGenerate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwy);

        phrase1 = (EditText) findViewById(R.id.pw);
        phrase2 = (EditText) findViewById(R.id.pwrep);
        pwLength = (EditText) findViewById(R.id.pwlen);
        specChars = (CheckBox) findViewById(R.id.checkbox_chars);
        loadingIndicator = (AVLoadingIndicatorView) findViewById(R.id.progressBar);
        btnGenerate = (Button) findViewById(R.id.button_show);
        btnGenerate.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_show:
                generate(v);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.licenses:
                Notices notices = new Notices();

                notices.addNotice(new Notice("AVLoadingIndicatorView", "https://github.com/81813780/AVLoadingIndicatorView", "Jack Wang", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("NineOldAndroids", "https://github.com/JakeWharton/NineOldAndroids", "Jake Wharton", new ApacheSoftwareLicense20()));

                new LicensesDialog.Builder(this)
                        .setNotices(notices)
                        .setIncludeOwnLicense(true)
                        .build()
                        .show();
                break;
            default:
                break;
        }
        return true;
    }

    private String hash(String password, int length, boolean characters) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String salt = password(password, 64, characters);

        int runs = 0;

        for (int i = 0; i < salt.length(); i++) {
            runs = runs + (int) salt.charAt(i);
        }

        runs += length;

        System.out.println(runs);

        byte[] hasher;

        for (int z = 0; z < runs; z++) {
            hasher = encrypt((salt + password).getBytes("UTF-8"), "SHA-512");

            StringBuilder sb = new StringBuilder(2 * hasher.length);
            for (byte b : hasher) {
                sb.append(String.format("%02x", b & 0xff));
            }
            password = sb.toString();
        }

        return password;
    }

    private String password(String str, int len, boolean chars) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] hasher = encrypt(str.getBytes("UTF-8"), "SHA-512");
        String hash = Base64.encodeToString(hasher, Base64.NO_WRAP);

        String password = hash.substring(0, len);

        if (chars) {
            byte[] replacer = encrypt(hash.getBytes("UTF-8"), "SHA-512");
            String replace = Base64.encodeToString(replacer, Base64.NO_WRAP);

            String[] replaceables = {"\\!", "\\@", "\\#", "\\$", "\\%", "\\^", "\\&", "\\*", "\\(", "\\)", "\\-", "\\_", "\\+", "\\=", "\\]", "\\[", "\\{", "\\}", "\\?", "\\<", "\\>"};

            List<Integer> nums = new LinkedList<>();
            Pattern p = Pattern.compile("\\d+");
            Matcher m = p.matcher(hash);
            while (m.find()) {
                nums.add(Integer.parseInt(m.group()));
            }

            int num = 0;

            for (int i = 0; i < len / 2; i++) {
                num = num + Integer.parseInt(nums.get(0).toString().substring(0, 1));

                if (num > 20) {
                    num = num - 20;
                }

                password = password.replaceFirst("[" + replace.charAt(i) + "]", replaceables[num]);
            }
        }

        return password;
    }

    private byte[] encrypt(byte[] target, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.update(target);
        return messageDigest.digest();
    }

    public void generate(final View view) {
        final String phrase = phrase1.getText().toString();
        String phraseRep = phrase2.getText().toString();
        final boolean characters = specChars.isChecked();
        String lengthStr = pwLength.getText().toString();

        if (!lengthStr.equals("")) {

            int newLength = Integer.parseInt(lengthStr);

            if (newLength > 64) {
                newLength = 64;
            } else if (newLength < 4) {
                newLength = 4;
            }

            pwLength.setText(String.valueOf(newLength));

            final int length = Integer.parseInt(pwLength.getText().toString());

            if (phraseRep.equals(phrase) || phraseRep.equals("")) {
                loadingIndicator.setVisibility(View.VISIBLE);
                btnGenerate.setEnabled(false);

                new Thread(new Runnable() {
                    public void run() {
                        String phrasehash;
                        try {
                            phrasehash = hash(phrase, length, characters);
                            final String pw = password(phrasehash, length, characters);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnGenerate.setEnabled(true);
                                    loadingIndicator.setVisibility(View.GONE);

                                    new AlertDialog.Builder(Passwy.this)
                                            .setTitle(R.string.app_name)
                                            .setMessage(pw)
                                            .setPositiveButton(R.string.hide, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                    ClipData clip = ClipData.newPlainText(getResources().getString(R.string.app_name), pw);
                                                    clipboard.setPrimaryClip(clip);

                                                    Snackbar.make(view, R.string.copied, Snackbar.LENGTH_SHORT).show();
                                                }
                                            })
                                            .show();
                                }
                            });
                        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                Snackbar.make(view, R.string.phr_doesnt_match, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Snackbar.make(view, R.string.enter_length, Snackbar.LENGTH_SHORT).show();
        }
    }


}

