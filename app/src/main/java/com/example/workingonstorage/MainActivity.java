package com.example.workingonstorage;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private static final int RUNTIME_PERMISSION_REQUEST_CODE = 5;
    private Context context;
    private String internalStoragePath = null;
    private String externalStoragePath = null;
    private ArraySet<String> readPossibleExternalStoragePaths;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkMultiplePermissions())
            {
                init();
            }
            else
            {
                //Toast.makeText(context,"Need Permission To Work With This Application",Toast.LENGTH_LONG).show();
            }
        }
        else {
            init();
        }
    }

    private void init()
    {
        externalStoragePath = getExternalStoragePath();
        internalStoragePath = getInternalStoragePath();

        if(readPossibleExternalStoragePaths != null) {
            for (int i = 0; i < readPossibleExternalStoragePaths.size(); i++)
            {
                if (internalStoragePath.equals(readPossibleExternalStoragePaths.valueAt(i))) {

                } else {
                    externalStoragePath = readPossibleExternalStoragePaths.valueAt(i);
                }
            }
        }

        if(externalStoragePath != null)
        {
            File externalMemoryFile = new File(externalStoragePath);
            final long externalMemoryTotalSpace = externalMemoryFile.getTotalSpace();
            final long externalMemoryFreeSpace = externalMemoryFile.getFreeSpace();
            final long externalMemoryUsedSpace = (externalMemoryTotalSpace - externalMemoryFreeSpace);

            Log.i(TAG,"ExternalMemoryTotalSpace =  "+   Formatter.formatFileSize(context,externalMemoryTotalSpace));
            Log.i(TAG,"ExternalMemoryFreeSpace  =  "+   Formatter.formatFileSize(context,externalMemoryFreeSpace));
            Log.i(TAG,"ExternalMemoryUsedSpace  =  "+   Formatter.formatFileSize(context,externalMemoryUsedSpace));
        }

        if(internalStoragePath != null)
        {
            File internalMemoryFile = new File(internalStoragePath);
            final long internalMemoryTotalSpace = internalMemoryFile.getTotalSpace();
            final long internalMemoryFreeSpace  = internalMemoryFile.getFreeSpace();
            final long internalMemoryUsedSpace  = (internalMemoryTotalSpace - internalMemoryFreeSpace);

            Log.i(TAG,"InternalMemoryTotalSpace = "+    Formatter.formatFileSize(context,internalMemoryTotalSpace));
            Log.i(TAG,"InternalMemoryFreeSpace  = "+     Formatter.formatFileSize(context,internalMemoryFreeSpace));
            Log.i(TAG,"InternalMemoryUsedSpace  = "+     Formatter.formatFileSize(context,internalMemoryUsedSpace));
        }

        Log.i(TAG,"InternalStoragePath = "+internalStoragePath);
        Log.i(TAG,"ExternalStoragePath = "+externalStoragePath);


//        File directory = new File(internalStoragePath);
//        getDataFromDirectory(directory);

//        getDataFromUri(externalStoragePath);

        getCachedDataSize();
    }

    private String getExternalStoragePath()
    {
        String externalStoragePath = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            File[] files = context.getExternalFilesDirs(null);

            if(files != null && files.length >= 2)
            {
                if(files[1] != null)
                {
                    externalStoragePath = files[1].getAbsolutePath();
                    String[] split = externalStoragePath.split("/Android/data/"+getPackageName()+"/files");
                    for (String s:split)
                    {
                        externalStoragePath = s;
                    }
                }
            }
        }
        else
        {
            readPossibleExternalStoragePaths = readProcMountsFile();
        }
        return externalStoragePath;
    }

    private String getInternalStoragePath()
    {
        String internalStoragePath = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            File[] files = context.getExternalFilesDirs(null);

            if(files != null)
            {
                internalStoragePath = files[0].getAbsolutePath();
                String[] split = internalStoragePath.split("/Android/data/"+getPackageName()+"/files");
                for (String s:split)
                {
                    internalStoragePath = s;
                }
            }
        }
        else
        {
            internalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return internalStoragePath;
    }

    private android.support.v4.util.ArraySet<String> readProcMountsFile()
    {
        RandomAccessFile randomAccessFile = null;
        StringBuilder stringBuilder = new StringBuilder();
        android.support.v4.util.ArraySet<String> pathsArraySet = new android.support.v4.util.ArraySet<>();
        pathsArraySet.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        try
        {
            randomAccessFile = new RandomAccessFile("/proc/mounts","r");
            String line;

            while ((line = randomAccessFile.readLine()) != null)
            {
                if(line.contains("vfat") || line.contains("/mnt"))
                {
                    if(line.contains("/dev/block/vold/"))
                    {
                        if(!line.contains("/mnt/secure")
                                && !line.contains("/mnt/asec")
                                && !line.contains("/mnt/obb")
                                && !line.contains("/dev/mapper")
                                && !line.contains("tmpfs"))
                        {
                            stringBuilder.append(line).append("\n");
                            StringTokenizer stringTokenizer = new StringTokenizer(line," ");
                            stringTokenizer.nextToken();    // First Token
                            String secondToken = stringTokenizer.nextToken();
                            pathsArraySet.add(secondToken);
                        }
                    }
                }
            }

            return pathsArraySet;
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG,"FileNotFoundException \n"+e.getMessage());
        } catch (IOException e)
        {
            Log.e(TAG,"IOException \n"+e.getMessage());
        }
        finally
        {
            try
            {
                if (randomAccessFile != null)
                {
                    randomAccessFile.close();
                }
            }
            catch (IOException e)
            {
                Log.e(TAG,"IOException while closing RandomAccessFile \n"+e.getMessage());
            }
        }

        return null;
    }

    private void getDataFromUri(String path)
    {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;//Uri.parse("content://"+path);
        String[]    columns = {MediaStore.Images.Media.DATA,MediaStore.Images.Media.TITLE,MediaStore.Images.Media._ID};
        String      whereClause = null;
        String[]    whereArgs = null;
        String      sortingOrder = MediaStore.Images.Media.DATE_ADDED +" DESC";
        Cursor cursor = getContentResolver().query(uri,columns,whereClause,whereArgs,sortingOrder);
        if(cursor != null && cursor.moveToFirst())
        {
            do
            {
                Uri uri1 = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID)));

                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Log.i(TAG, "Data = " + uri1 + " = "+data);
                /*if(data.equals("/storage/C090-10FF/Naveen/IMG_20170331_083755.jpg"))
                {
                    Log.i(TAG, "Data = " + data);
                    getContentResolver().delete(Uri.parse("file://"+data),null,null);
                }*/
//                getContentResolver().delete(uri1,null,null);

                if(uri1.equals(Uri.parse("content://media/external/images/media/6532")))
                {
                    Log.i(TAG, "Data = " + uri1);
//                    getContentResolver().delete(uri1,null,null);
                }
            }while (cursor.moveToNext());
        }


    }

    private void getDataFromDirectory(File directory)
    {
        File listFiles[] = directory.listFiles();

        if(listFiles != null)
        {
            for (File file : listFiles)
            {
                if (file.isDirectory())
                {
                    Log.i(TAG,"Directory = "+file.getName());
                    getDataFromDirectory(file);
                }
                else
                {
//                    Log.i(TAG,"File Name = "+file.getName());
                }
            }
        }
    }

    private boolean checkMultiplePermissions()
    {
        int readPermission = ContextCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE);
        int wirtePermission = ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> permissionList = new ArrayList<>();
        if (readPermission != PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (wirtePermission != PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty())
        {
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]), RUNTIME_PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case RUNTIME_PERMISSION_REQUEST_CODE:
                if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                {
                    Toast.makeText(context,"Now You Got Permission To Work With This Application",Toast.LENGTH_LONG).show();
                    init();
                }
                else
                {
                    Toast.makeText(context,"Need Permission To Work With This Application",Toast.LENGTH_LONG).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }


    }

    private void getCachedDataSize()
    {


    }
}
