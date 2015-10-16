package com.pollytronics.pollydac_test;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.pollytronics.pollydac_test.pollydac.DbEntry;
import com.pollytronics.pollydac_test.database.MyDAC;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyService extends Service {
    private static final String TAG = "MyService";
    Handler handler;
    WebLoop webLoop;
    DecreaseLoop decreaseLoop;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        handler = new Handler();
        webLoop = new WebLoop();
        decreaseLoop = new DecreaseLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        handler.removeCallbacks(webLoop);
        handler.removeCallbacks(decreaseLoop);
        handler.post(webLoop);
        handler.post(decreaseLoop);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        handler.removeCallbacks(webLoop);
        handler.removeCallbacks(decreaseLoop);
    }

    class WebLoop implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "webloop");
            new SyncTask().executeOnExecutor(SyncTask.THREAD_POOL_EXECUTOR);
            handler.postDelayed(this, 4000);
        }
    }

    class DecreaseLoop implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "decreaseLoop");
            decreaseAllNumbers();
            handler.postDelayed(this, 500);
        }
    }

    private void decreaseAllNumbers() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MyDAC dao = MyDAC.get(MyService.this);
                dao.keepOpen();
                List<DbEntry<MyNumber>> allNumbers = dao.numbers().getAllEntries();
                for (DbEntry<MyNumber> n : allNumbers) {
                    int val = n.getObject().getValue();
                    n.getObject().setValue(val > 0 ? val - 1 : 0);
                    //dao.numbers().update(n);
                    n.update();
                }
                dao.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                MyDAC.get(MyService.this).dispatchChange(false, null);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void syncWithServer() throws IOException, JSONException {
        Log.i(TAG, "syncWithServer");

        final int MAXNRS = 15;

        String reply = getNumbersFromServer();

        JSONArray jsonArray = new JSONArray(reply);
        List<MyNumber> numbers = new ArrayList<>();
        for (int i = 0, size = jsonArray.length(); i < size; i++) {
            numbers.add(new MyNumber(jsonArray.getInt(i)));
        }

        MyDAC dao = MyDAC.get(MyService.this);
        dao.keepOpen();
        for (MyNumber number : numbers) {
            dao.numbers().insert(number);
        }
        List<DbEntry<MyNumber>> DbMyNumbers = dao.numbers().getAllEntries();
        Collections.sort(DbMyNumbers, new Comparator<DbEntry<MyNumber>>() {
            @Override
            public int compare(DbEntry<MyNumber> lhs, DbEntry<MyNumber> rhs) {
                return lhs.getObject().compareTo(rhs.getObject());
            }
        });
        int nEntries = DbMyNumbers.size();
        if (nEntries > MAXNRS) {
            for (DbEntry<MyNumber> overTen : DbMyNumbers.subList(0, DbMyNumbers.size()-MAXNRS)) {
                dao.numbers().delete(overTen);
            }
        }
        dao.close();
    }

    private class SyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "SyncTask.doInBackground");
            try {
                syncWithServer();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            MyDAC.get(MyService.this).dispatchChange(false, null);
        }
    }

    private static String getNumbersFromServer() throws IOException {
        URL url = new URL("http://192.168.0.2:8080/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream is = conn.getInputStream();
        String response = readInputStream(is);
        is.close();
        conn.disconnect();
        Log.i(TAG, response);
        return response;
    }

    private static String readInputStream(InputStream is) throws IOException {
        final int bufferSize = 16 * 1024;
        Reader reader = new InputStreamReader(is, "UTF-8");
        char[] buffer = new char[bufferSize];
        int charsRead = reader.read(buffer,0,bufferSize);
        int length = charsRead;
        Log.i(TAG, "charsRead = " + charsRead + "   length = " + length);
        while((charsRead != -1) && (length < bufferSize)) {
            charsRead = reader.read(buffer, length, charsRead);
            if (charsRead > 0) length += charsRead;
            Log.i(TAG, "charsRead = " + charsRead + "   length = " + length);
        }
        if (charsRead != -1) { // this happens when the response size was longer or equal to the buffer size)
            Log.i(TAG, "stream = " + new String(buffer, 0, length));
            throw new IOException(String.format("api response longer than expected (buffersize = %d)", bufferSize));
        }
        return new String(buffer, 0, length);
    }
}
