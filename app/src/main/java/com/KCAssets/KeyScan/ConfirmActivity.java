package com.KCAssets.KeyScan;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ConfirmActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    ListView listView;
    CustomAdapter customAdapter;
    ArrayList<String> scannedIDs;
    ArrayList<String> Descriptions;
    ArrayList<String> Manufacturers;
    ArrayList<String> Models;
    ArrayList<String> SerialNumbers;
    ArrayList<String> CalibrationDates;
    ArrayList<String> CalibrationDatesF;
    ArrayList<String> Category;
    ArrayList<String> Division;
    ArrayList<String> PastDue;
    EditText jobNumber;
    EditText testType;
    String testTypeValue;
    String jobNumberValue;
    Button submitBtn;
    ProgressDialog progressDialog;
    String personName;
    String formattedDate;
    String accessToken;
    String folderName;
    ImageView check;
    Button folderBtn;
    String PREF_FOLDER = "folder";
    private Drive driveService;
    String progressMessage;
    String toastMessage;


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_screen);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        listView = findViewById(R.id.listview_id);
        submitBtn = findViewById(R.id.submitBtn);

        scannedIDs = getIntent().getStringArrayListExtra("scannedIDs");
        Descriptions = getIntent().getStringArrayListExtra("Descriptions");
        Manufacturers = getIntent().getStringArrayListExtra("Manufacturers");
        Models = getIntent().getStringArrayListExtra("Models");
        SerialNumbers = getIntent().getStringArrayListExtra("SerialNumbers");
        CalibrationDates = getIntent().getStringArrayListExtra("CalibrationDates");
        CalibrationDatesF = getIntent().getStringArrayListExtra("CalibrationDatesF");
        Category = getIntent().getStringArrayListExtra("Category");
        Division = getIntent().getStringArrayListExtra("Division");
        PastDue = getIntent().getStringArrayListExtra("PastDue");
        check = findViewById(R.id.check);
        folderBtn = findViewById(R.id.folderBtn);
        updateListView(scannedIDs, Descriptions, PastDue);

        // Loading Popup Initialization
        progressDialog = new ProgressDialog(ConfirmActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        check.setVisibility(View.INVISIBLE);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        folderName = sharedPreferences.getString("folder", null);

        boolean hasPastDueAssets = checkPastDueAssets(PastDue);
        if (hasPastDueAssets) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setMessage("One or more Assets are past due for Calibration (marked in red)");
            builder.setPositiveButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            personName = account.getDisplayName();
        }

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        formattedDate = dateFormat.format(currentDate);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();


        /***********************************************************
         * Toolbar
         **********************************************************/
        ImageView backArrow = findViewById(R.id.backArrow);
        TextView title = findViewById(R.id.title);
        RelativeLayout bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setVisibility(View.GONE);
        ImageView keylogo = findViewById(R.id.keylogo);
        keylogo.setVisibility(View.GONE);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfirmActivity.this, TestingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        title.setText("Confirmation");


        /***********************************************************
         * EditTexts
         **********************************************************/
        testType = findViewById(R.id.test);
        testType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used in this case
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Store the updated input in the variable
                testTypeValue = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used in this case
            }
        });

        jobNumber = findViewById(R.id.job);
        jobNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used in this case
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Store the updated input in the variable
                jobNumberValue = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used in this case
            }
        });

        jobNumber.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Set the visibility of 'check' to GONE
                check.setVisibility(View.GONE);
                return false;
            }
        });

        if (folderName != null) {

            jobNumber.setText(folderName);

            // Perform the Google Drive API task
            progressMessage = "Searching for folder... ";
            toastMessage = "Folder found in 'My Drive'";
            DriveTask driveTask = new DriveTask(folderName, progressDialog, progressMessage, toastMessage);
            driveTask.execute();
        }


        /***********************************************************
         * Buttons
         **********************************************************/
        // Set click listener for the search/create button
        folderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check expiration time of access token
                AccessTokenManager accessTokenManager = new AccessTokenManager(ConfirmActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                // Get the folder name from the EditText
                folderName = jobNumber.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_FOLDER, folderName);
                editor.apply();

                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(jobNumber.getWindowToken(), 0);

                // Remove focus and cursor from the EditText
                jobNumber.clearFocus();

                // Perform the Google Drive API task
                progressMessage = "Searching for folder... ";
                toastMessage = "Folder found in 'My Drive'";
                DriveTask driveTask = new DriveTask(folderName, progressDialog, progressMessage, toastMessage);
                driveTask.execute();
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(ConfirmActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (!scannedIDs.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConfirmActivity.this);
                    builder.setTitle("Confirmation");
                    builder.setMessage("Are you sure you want to submit list?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (folderName != null && testTypeValue != null) {
                                createEquipmentList();
                            } else {
                                Toast.makeText(ConfirmActivity.this, "Error: Job Number or File Name inputs are empty", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.show();
                } else {
                    Toast.makeText(ConfirmActivity.this, "Error: Asset List is Empty", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    /***********************************************************
     * Adapter and Past Due Handling
     **********************************************************/
    private void updateListView(ArrayList<String> scannedIDs, ArrayList<String> Descriptions, ArrayList<String> PastDue) {
        customAdapter = new CustomAdapter(getApplicationContext(), scannedIDs, Descriptions, PastDue);
        listView.setAdapter(customAdapter);
    }

    private boolean checkPastDueAssets(ArrayList<String> pastDue) {
        for (String pastDueValue : pastDue) {
            if (pastDueValue.equals("1")) {
                return true;
            }
        }
        return false;
    }


    /***********************************************************
     * Equipment List Google Sheet
     **********************************************************/
    private void createEquipmentList() {
        // Disable the submit button to prevent multiple clicks
        submitBtn.setEnabled(false);

        // Show a progress dialog to indicate the operation is in progress
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating equipment list...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Start the AsyncTask to perform the network operations in the background
        new CreateEquipmentListTask().execute();
    }

    private class CreateEquipmentListTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private String databaseId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create the Google Sheets service
            sheetsService = createSheetsService();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            // Add data to the spreadsheet
            List<List<Object>> data = prepareDataForSheet();
            writeDataToSheet(sheetsService, data);

            databaseId = "1sdWT3yQUiqbyI6_k1YhBkHXMqrl4J0hRoAm0nqWZKgI"; // Replace with your existing spreadsheet ID
            List<List<Object>> existingData = prepareDataForDatabase();
            writeDataToDatabase(sheetsService, databaseId, existingData);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Dismiss the progress dialog
            progressDialog.dismiss();

            // Enable the submit button
            submitBtn.setEnabled(true);

            startActivity(new Intent(ConfirmActivity.this, FinishActivity.class));
        }
    }

    private Sheets createSheetsService() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("KeyScan")
                .build();
    }

    private Drive createDriveService() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
        JsonFactory jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();

        // Initialize Google Drive API client
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("KeyScan")
                .build();
    }

    private String getFolderIdByName(String folderName) throws IOException {
        FileList result = driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "'")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();

        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        } else {
            return null;
        }
    }

    private List<List<Object>> prepareDataForDatabase() {
        // Prepare the data to be added to the existing sheet
        List<List<Object>> addData = new ArrayList<>();

        // Add your data to the 'data' list
        for (int i = 0; i < scannedIDs.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(scannedIDs.get(i));
            row.add(Descriptions.get(i));
            row.add(Manufacturers.get(i));
            row.add(Models.get(i));
            row.add(SerialNumbers.get(i));
            row.add(CalibrationDates.get(i));
            row.add(CalibrationDatesF.get(i));
            row.add(Category.get(i));
            row.add(Division.get(i));
            row.add(personName);
            row.add("Testing");
            row.add(formattedDate);
            addData.add(row);
        }
        return addData;
    }

    private List<List<Object>> prepareDataForSheet() {
        List<List<Object>> data = new ArrayList<>();

        // Add the column headers
        List<Object> headers = new ArrayList<>();
        headers.add("Asset");
        headers.add("Description");
        headers.add("Manufacturer");
        headers.add("Model");
        headers.add("Serial #");
        headers.add("Calibration Due");
        // Add more headers for other data arrays if needed
        data.add(headers);

        // Add the data rows
        for (int i = 0; i < scannedIDs.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(scannedIDs.get(i));
            row.add(Descriptions.get(i));
            row.add(Manufacturers.get(i));
            row.add(Models.get(i));
            row.add(SerialNumbers.get(i));
            row.add(CalibrationDates.get(i));
            // Add more values for other data arrays if needed
            data.add(row);
        }
        return data;
    }

    private void writeDataToSheet(Sheets sheetsService, List<List<Object>> data) {
        WriteDataToSheetTask writeTask = new WriteDataToSheetTask(sheetsService, data);
        writeTask.execute();
    }

    private class WriteDataToSheetTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private List<List<Object>> values;

        public WriteDataToSheetTask(Sheets sheetsService, List<List<Object>> values) {
            this.sheetsService = sheetsService;
            this.values = values;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                driveService = createDriveService();

                // Prepare the data to be written to the sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(values);

                String folderId = getFolderIdByName(folderName);

                if (folderId != null) {
                    // Create file metadata
                    File fileMetadata = new File();
                    fileMetadata.setParents(Collections.singletonList(folderId)); // Set the folder ID
                    fileMetadata.setName(testTypeValue); // Set the file name
                    fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");

                    // Create the file in Google Drive
                    File createdFile = driveService.files().create(fileMetadata).execute();

                    String createdSpreadsheetId = createdFile.getId();

                    // Perform the write operation using the Google Sheets API
                    sheetsService.spreadsheets().values()
                            .update(createdSpreadsheetId, "Sheet1!A1", valueRange)
                            .setValueInputOption("RAW")
                            .execute();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI or perform any post-execution tasks
        }
    }

    private void writeDataToDatabase(Sheets sheetsService, String spreadsheetId, List<List<Object>> data) {
        WriteDataToDatabaseTask writeTask = new WriteDataToDatabaseTask(sheetsService, spreadsheetId, data);
        writeTask.execute();
    }

    private class WriteDataToDatabaseTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private String spreadsheetId;
        private List<List<Object>> values;

        public WriteDataToDatabaseTask(Sheets sheetsService, String spreadsheetId, List<List<Object>> values) {
            this.sheetsService = sheetsService;
            this.spreadsheetId = spreadsheetId;
            this.values = values;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Prepare the data to be written to the sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(values);

                // Get the current data range of the sheet
                String range = "Output!A:L";
                ValueRange result = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
                List<List<Object>> currentData = result.getValues();

                // Calculate the next empty row index
                int nextRowIndex = currentData != null ? currentData.size() + 1 : 1;

                // Append the data to the next empty row
                String appendRange = "Output!A" + nextRowIndex;
                sheetsService.spreadsheets().values()
                        .append(spreadsheetId, appendRange, valueRange)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI or perform any post-execution tasks
        }
    }


    /***********************************************************
     * Folder Check and Creation
     **********************************************************/
    public class DriveTask extends AsyncTask<String, Void, Boolean> {
        private String folderInput;
        private ProgressDialog progressDialog;
        private boolean createFolder;
        private String progressMessage;
        private String toastMessage;

        public DriveTask(String folderInput, ProgressDialog progressDialog, String progressMessage, String toastMessage) {
            this.folderInput = folderInput;
            this.progressMessage = progressMessage;
            this.toastMessage = toastMessage;
            this.progressDialog = progressDialog;
            this.createFolder = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage(progressMessage);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                driveService = createDriveService();

                // Search for the folder by name
                String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderInput + "'";
                FileList result = driveService.files().list().setQ(query).setSpaces("drive").execute();
                List<File> files = result.getFiles();

                // Check if the folder exists
                if (files != null && !files.isEmpty()) {
                    // Folder exists
                    File folder = files.get(0);

                } else {
                    // Folder does not exist
                    createFolder = true;
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (result) {
                if (!createFolder) {
                    check.setVisibility(View.VISIBLE);
                    Toast.makeText(ConfirmActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                }

                if (createFolder) {
                    showCreateFolderDialog();
                }
            } else {
                Toast.makeText(ConfirmActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
            }
        }

        private void showCreateFolderDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(progressDialog.getContext());
            builder.setMessage("This folder does not exist. Would you like to create it?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            createFolder();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            folderName = null;
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }


        /***********************************************************
         * Create Folder if it Does not Exist
         **********************************************************/
        private void createFolder() {
            ProgressDialog createFolderProgressDialog = new ProgressDialog(ConfirmActivity.this);
            createFolderProgressDialog.setCancelable(false);

            new CreateFolderTask(folderInput, createFolderProgressDialog).execute();
        }

        private class CreateFolderTask extends AsyncTask<Void, Void, Boolean> {
            private String folderInput;
            private ProgressDialog progressDialog;

            public CreateFolderTask(String folderInput, ProgressDialog progressDialog) {
                this.folderInput = folderInput;
                this.progressDialog = progressDialog;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Creating folder...");
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    driveService = createDriveService();

                    File folderMetadata = new File();
                    folderMetadata.setName(folderInput);
                    folderMetadata.setMimeType("application/vnd.google-apps.folder");

                    File folder = driveService.files().create(folderMetadata).setFields("id").execute();
                    System.out.println("Folder created: " + folder.getName() + " (ID: " + folder.getId() + ")");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                progressDialog.dismiss();

                if (result) {
                    check.setVisibility(View.VISIBLE);
                    Toast.makeText(ConfirmActivity.this, "Folder Created in 'My Drive'", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(ConfirmActivity.this, "Error: Network connection not found.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}