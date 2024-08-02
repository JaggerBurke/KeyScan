package com.KCAssets.KeyScan;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestingActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    private Button scanBtn;
    private Button inputBtn;
    private ListView scannedListView;
    ScannedItemAdapter scannedListAdapter;
    List<String> scannedList;
    private Button finishBtn;
    Button clearBtn;
    RelativeLayout mainLayout;
    String sheetID = "1Sij2xp0U9_ZYevVqGgkHvat8GYO0N3prVsCJKmnCmdg";
    String accessToken;
    ArrayList<String> listID = new ArrayList<String>();
    ArrayList<String> listDescript = new ArrayList<String>();
    ArrayList<String> listManufact = new ArrayList<String>();
    ArrayList<String> listModel = new ArrayList<String>();
    ArrayList<String> listSerial = new ArrayList<String>();
    ArrayList<String> listCalDate = new ArrayList<String>();
    ArrayList<String> scannedIDs = new ArrayList<>();
    ArrayList<String> listCalDateF = new ArrayList<String>();
    ArrayList<String> listCategory = new ArrayList<String>();
    ArrayList<String> listDivision = new ArrayList<String>();
    ArrayList<String> listPastDue = new ArrayList<String>();
    ProgressDialog progressDialog;
    SharedPreferences sharedPreferences;


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();

        mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackgroundColor(0xFFFFFFFF);

        RelativeLayout bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setVisibility(View.GONE);

        ImageView keylogo = findViewById(R.id.keylogo);
        keylogo.setVisibility(View.GONE);

        Spinner spinnerCal = findViewById(R.id.spinnerCal);
        spinnerCal.setVisibility(View.GONE);

        TextView textCal = findViewById(R.id.textCal);
        textCal.setVisibility(View.GONE);

        ImageView backArrow = findViewById(R.id.backArrow);
        TextView title = findViewById(R.id.title);

        scanBtn = findViewById(R.id.scanBtn);
        finishBtn = findViewById(R.id.finishBtn);
        inputBtn = findViewById(R.id.inputBtn);
        clearBtn = findViewById(R.id.clearBtn);

        scannedList = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("IDs", Context.MODE_PRIVATE);

        scannedListView = findViewById(R.id.scannedList);
        scannedListAdapter = new ScannedItemAdapter(this, scannedList, prefs);
        scannedListView.setAdapter(scannedListAdapter);

        Set<String> scannedIDsSet = prefs.getStringSet("IDs", new HashSet<>());
        scannedList.addAll(scannedIDsSet);
        scannedListAdapter.notifyDataSetChanged();


        /***********************************************************
         * Button Clicks
         **********************************************************/
        scanBtn.setOnClickListener(v -> {
            accessTokenManager.checkAccessTokenExpiration();
            scanCode();
        });
        inputBtn.setOnClickListener(v -> {
            accessTokenManager.checkAccessTokenExpiration();
            showManualInputDialog();
        });
        clearBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(TestingActivity.this);
            builder.setTitle("Clear All");
            builder.setMessage("Are you sure you want to clear all Assets?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    scannedList.clear();
                    scannedListAdapter.notifyDataSetChanged();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("IDs");
                    editor.apply();
                }
            });
            builder.setNegativeButton("No", null);
            builder.show();
        });

        finishBtn.setOnClickListener(v -> {
            accessTokenManager.checkAccessTokenExpiration();
            // Pass the scanned IDs to the Excel file and check for matches
            performExcelLookup();
        });

        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(TestingActivity.this, WelcomeActivity.class));
            }
        });

        title.setText("Testing");
    }


    /***********************************************************
     * Barcode Handling
     **********************************************************/
    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    private void storeBarcode(String barcode) {
        int arrayId = getResources().getIdentifier(barcode, "array", getPackageName());
        if (arrayId != 0) {
            String[] idArray = getResources().getStringArray(arrayId);
            scannedList.addAll(Arrays.asList(idArray));
        } else {
            // Barcode doesn't match a predefined code word
            scannedList.add(barcode);
        }
        scannedListAdapter.notifyDataSetChanged();
        saveScannedIDs();
    }
 
    private void showManualInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input ID exactly how it appears in asset list");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Request focus and show the keyboard
        input.requestFocus();
        input.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 100);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String assetId = input.getText().toString().trim();
                if (!TextUtils.isEmpty(assetId)) {
                    // Check if the input matches a predefined array name
                    int arrayId = getResources().getIdentifier(assetId, "array", getPackageName());
                    if (arrayId != 0) {
                        // Input matches a predefined array name
                        String[] idArray = getResources().getStringArray(arrayId);
                        scannedList.addAll(Arrays.asList(idArray));
                    } else {
                        // Input doesn't match a predefined array name
                        storeBarcode(assetId);
                    }
                    scannedListAdapter.notifyDataSetChanged();
                    saveScannedIDs();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            String barcode = result.getContents();
            storeBarcode(barcode);
        }
    });


    /***********************************************************
     * Asset ID Session Storage
     **********************************************************/
    private void saveScannedIDs() {
        SharedPreferences prefs = getSharedPreferences("IDs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("IDs", new HashSet<>(scannedList));
        editor.apply();
    }


    /***********************************************************
     * Asset List Data Pull
     **********************************************************/
    private void performExcelLookup() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Retrieving Data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String range = "'Asset List'!A:AA";
        Sheets sheetsService = createSheetsService();
        new ReadDataFromSheetTask(sheetsService, range).execute();
    }

    private Sheets createSheetsService() {
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("KeyScan")
                .build();
    }

    private class ReadDataFromSheetTask extends AsyncTask<Void, Void, List<List<Object>>> {
        private Sheets sheetsService;
        private String range;

        public ReadDataFromSheetTask(Sheets sheetsService, String range) {
            this.sheetsService = sheetsService;
            this.range = range;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... voids) {
            try {
                // Read the data from the sheet
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(sheetID, range)
                        .execute();
                return response.getValues();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<List<Object>> values) {
            progressDialog.dismiss();

            if (values != null) {
                // Clear the existing lists before populating with new data
                listID.clear();
                listDescript.clear();
                listManufact.clear();
                listModel.clear();
                listSerial.clear();
                listCalDate.clear();
                listCalDateF.clear();
                listCategory.clear();
                listDivision.clear();
                listPastDue.clear();

                // Iterate through the rows and find matches for scanned IDs
                for (int i = 1; i < values.size(); i++) {
                    List<Object> row = values.get(i);
                    String id = row.get(0).toString();

                    // Check if the ID is present in the scannedList
                    if (scannedList.contains(id)) {
                        String descript = "";
                        String manufact = "";
                        String model = "";
                        String serial = "";
                        String caldate = "";
                        String caldatef = "";
                        String category = "";
                        String division = "";
                        String pastdue = "";

                        if (row.size() > 1) {
                            descript = row.get(1).toString();
                        }
                        if (row.size() > 2) {
                            manufact = row.get(2).toString();
                        }
                        if (row.size() > 3) {
                            model = row.get(3).toString();
                        }
                        if (row.size() > 4) {
                            serial = row.get(4).toString();
                        }
                        if (row.size() > 5) {
                            caldate = row.get(5).toString();
                        }
                        if (row.size() > 6) {
                            caldatef = row.get(6).toString();
                        }
                        if (row.size() > 7) {
                            category = row.get(7).toString();
                        }
                        if (row.size() > 15) {
                            division = row.get(15).toString();
                        }
                        if (row.size() > 26) {
                            pastdue = row.get(26).toString();
                        }

                        // Store the matched values in the lists
                        listID.add(id);
                        listDescript.add(descript);
                        listManufact.add(manufact);
                        listModel.add(model);
                        listSerial.add(serial);
                        listCalDate.add(caldate);
                        listCalDateF.add(caldatef);
                        listCategory.add(category);
                        listDivision.add(division);
                        listPastDue.add(pastdue);
                    }
                }

                Intent intent = new Intent(TestingActivity.this, ConfirmActivity.class);
                intent.putStringArrayListExtra("scannedIDs", listID);
                intent.putStringArrayListExtra("Descriptions", listDescript);
                intent.putStringArrayListExtra("Manufacturers", listManufact);
                intent.putStringArrayListExtra("Models", listModel);
                intent.putStringArrayListExtra("SerialNumbers", listSerial);
                intent.putStringArrayListExtra("CalibrationDates", listCalDate);
                intent.putStringArrayListExtra("CalibrationDatesF", listCalDateF);
                intent.putStringArrayListExtra("Category", listCategory);
                intent.putStringArrayListExtra("Division", listDivision);
                intent.putStringArrayListExtra("PastDue", listPastDue);
                startActivity(intent);
            } else {
                // Error occurred while reading the data from the sheet
                Toast.makeText(TestingActivity.this, "Error. Check network connection and try refreshing login", Toast.LENGTH_LONG).show();
            }
        }
    }
}
