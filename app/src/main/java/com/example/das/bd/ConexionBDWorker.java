package com.example.das.bd;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConexionBDWorker extends Worker {

    //Worker encargado de realizar las conexiones con las BD remotas

    public ConexionBDWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i("MYAPP","en do work");

        String fichero=getInputData().getString("fichero");
        //La dirección se completa con la url al servidor y el nombre del fichero al que se realiza la consulta
        String direccion = "http://ec2-54-242-79-204.compute-1.amazonaws.com/nlebena001/WEB/"+fichero;
        Log.i("MYAPP",direccion);

        //Los parámetros de la consulta los recibe de los input data
        String parametros=getInputData().getString("parametros");
        Log.i("MYAPP",parametros);

        HttpURLConnection urlConnection = null;
        String result="";
        try {
            //Se realiza la conexión
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setDoOutput(true);

            //Se añaden los parámetros a la petición
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();
            Log.i("MYAPP", "vAMOS A HACER EL POST");


            //En caso de obtener los datos correctamente se almacenan en una variable result que se devuelve a la clase que hizo la petición
            int statusCode=urlConnection.getResponseCode();
            Log.i("MYAPP", String.valueOf(statusCode));
            Log.i("MYAPP",urlConnection.getResponseMessage());
            if (statusCode == 200) {
                Log.i("MYAPP","CONEXION ESTABLECIDA");

                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("MYAPP",result);
        Data resultados = new Data.Builder()
                .putString("resultado",result)
                .build();
        return Result.success(resultados);
    }
}
