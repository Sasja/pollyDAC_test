package com.pollytronics.pollydac_test;

import android.app.ActivityManager;
import android.app.LoaderManager;
import android.app.Service;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.pollytronics.pollydac_test.pollydac.DbEntry;
import com.pollytronics.pollydac_test.database.MyDAC;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<MyNumber>> {
    private static final String TAG = "MainAcitivity";

    private ArrayAdapter<MyNumber> adapter;
    private ContentObserver dbObserver;

    private boolean isMyServiceRunning(Class<? extends Service> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<MyNumber>(this, R.layout.listview_item);

        getLoaderManager().initLoader(0, null, MainActivity.this);
        LoaderManager.enableDebugLogging(true);

        ListView myListView = (ListView) findViewById(R.id.my_listview);
        myListView.setAdapter(adapter);

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        Button myButton = (Button) findViewById(R.id.mybutton);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        MyDAC dao = MyDAC.get(MainActivity.this);
                        DbEntry<MyNumber> newNumber = dao.numbers().insert(new MyNumber(new Random().nextInt(1000)));
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        MyDAC.get(MainActivity.this).dispatchChange(false, null);
                    }
                }.execute();
            }
        });

        Switch mySwitch = (Switch) findViewById(R.id.my_switch);
        mySwitch.setChecked(isMyServiceRunning(MyService.class));
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, MyService.class);
                if (isChecked) {
                    Log.i(TAG, "starting service");
                    startService(intent);
                } else {
                    Log.i(TAG, "stopping service");
                    stopService(intent);
                }
            }
        });

        dbObserver = new ContentObserver(new Handler()) { //  passing a handler will make the onChange run asynchronously
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.i(TAG, "ContentObserver.onChange()");
                getLoaderManager().getLoader(0).onContentChanged();
            }
        };
    }

    @Override
    public Loader<List<MyNumber>> onCreateLoader(int id, Bundle args) {
       Log.i(TAG, "onCreateLoader()");
       return new AsyncTaskLoader<List<MyNumber>>(this) {
            @Override
            public List<MyNumber> loadInBackground() {
                Log.i(TAG, "AsyncTaskLoader.loadInBackGroud");
                List<MyNumber> allNumbers = MyDAC.get(MainActivity.this).numbers().getAllObjects();
                Collections.sort(allNumbers);
                return allNumbers;
            }
       };
    }

    @Override
    public void onLoadFinished(Loader<List<MyNumber>> loader, List<MyNumber> data) {
        Log.i(TAG, "onLoadFinished()");
        if (data != null) {
            adapter.clear();
            adapter.addAll(data);
        } else {
            Log.i(TAG, "ITS NULL!");
        }
    }

    @Override
    public void onLoaderReset(Loader<List<MyNumber>> loader) {
        adapter.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyDAC.get(this).registerObserver(dbObserver);
        MyDAC.get(this).dispatchChange(false, null);
    }

    @Override
    protected void onPause() {
        MyDAC.get(this).unregisterObserver(dbObserver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
