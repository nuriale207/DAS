package com.example.das;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class ChatsFragment extends Fragment {
    String[] ids = {};
    String[] nombres = {};
    String[] tokens = {};
    byte[][] imagenes = {};
    Handler handler;
    AdaptadorChats adaptador;
    //ImageView inexistente=new ImageView(getContext());

    public static boolean running;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rellenarListas();

        ListView lista=getView().findViewById(R.id.listaChats);
        adaptador = new AdaptadorChats(getActivity(), ids,nombres,tokens, imagenes);
        lista.setAdapter(adaptador);
        GestorChats.getGestorListas().asignarAdaptadorChats(adaptador);
        GestorChats.getGestorListas().actualizarChats();
        handler = new Handler();
        Runnable actualizadorChat = new Runnable() {
            @Override
            public void run() {
                if(GestorChats.getGestorListas().comprobarNuevoChat()){
                    String id=GestorChats.getGestorListas().getIdNuevoChat();
                    String mensaje=GestorChats.getGestorListas().getMensajeNuevo();
                    anadirUsuarioABDLocal(id,mensaje);
                }
                handler.postDelayed(this,2000);
            }
        };
        handler.postDelayed(actualizadorChat, 2000);
    }

    public void actualizarListaChats(){
        rellenarListas();
        ListView lista=getView().findViewById(R.id.listaChats);
        adaptador = new AdaptadorChats(getActivity(), ids,nombres,tokens, imagenes);
        lista.setAdapter(adaptador);
        lista.setSelection(0);
        GestorChats.getGestorListas().actualizarChats();
    }

    //https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
    @Override
    public void onStart() {
        super.onStart();
        running = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    public void rellenarListas(){
        BDLocal gestorDB = new BDLocal (getContext(), "DAS", null, 1);
        SQLiteDatabase bd = gestorDB.getWritableDatabase();

        String[] campos = new String[] {"Id", "Nombre", "Token", "Imagen"};
        Cursor cu = bd.query("Usuarios",campos,null,null,null,null,null);

        if(cu.getCount() != 0){
            cu.moveToFirst();
            ArrayList<String> idsAux = new ArrayList<>();
            ArrayList<String> nombresAux = new ArrayList<>();
            ArrayList<String> tokensAux = new ArrayList<>();
            ArrayList<byte[]> imagenesAux = new ArrayList<>();
            for(int i = 0; i < cu.getCount(); i++){

                String[] campos2 = new String[] {"Mensaje"};
                String[] argumentos = new String[] {cu.getString(0)};
                Cursor cu2 = bd.query("Mensajes",campos2,"IdUsuario=?",argumentos,null,null,null);
                if (cu2.getCount() != 0){
                    idsAux.add(cu.getString(0));
                    nombresAux.add(cu.getString(1));
                    tokensAux.add(cu.getString(2));
                    imagenesAux.add(cu.getBlob(3));
                }
                cu.moveToNext();
            }
            cu.close();
            ids = new String[idsAux.size()];
            nombres = new String[nombresAux.size()];
            tokens = new String[tokensAux.size()];
            imagenes = new byte[imagenesAux.size()][];
            for (int j=0; j<idsAux.size();j++){
                ids[j] = idsAux.get(j);
                nombres[j] = nombresAux.get(j);
                tokens[j] = tokensAux.get(j);
                imagenes[j] = imagenesAux.get(j);
            }
        }
    }

    private void anadirUsuarioABDLocal(String id_remitente,String mensaje) {
        //Metodo que carga la imagen de Firebase Storage
        //Metodo que carga la imagen de Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child("images/" + id_remitente + ".jpg");
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getContext())
                        .asBitmap()
                        .load(uri)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {

                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] imageInByte = stream.toByteArray();
                                anadirUsuarioABDLocal2(id_remitente,imageInByte,mensaje);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
        });
    }


    private void anadirUsuarioABDLocal2(String id_remitente,byte[] imagen,String mensaje) {

        Data datos = new Data.Builder()
                .putString("fichero", "DAS_users.php")
                .putString("parametros", "funcion=datosUsuario&id=" + id_remitente)
                .build();
        OneTimeWorkRequest requesContrasena = new OneTimeWorkRequest.Builder(ConexionBDWorker.class).setInputData(datos).addTag("getDatosUsuario"+id_remitente).build();
        WorkManager.getInstance(this.getContext()).getWorkInfoByIdLiveData(requesContrasena.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String resultado = workInfo.getOutputData().getString("resultado");
                            Log.i("MYAPP", "inicio realizado");

                            Log.i("MYAPP", resultado);
                            try {

                                JSONObject jsonObject = new JSONObject(resultado);
                                String nombre = jsonObject.getString("nombre");
                                String token = jsonObject.getString("id_FCM");

//                                Drawable drawable = inexistente.getDrawable();
//                                BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
//                                Bitmap bitmap = bitmapDrawable .getBitmap();
//                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                                byte[] imageInByte = stream.toByteArray();

                                BDLocal gestorDB = new BDLocal (getContext(), "DAS", null, 1);
                                SQLiteDatabase bd = gestorDB.getWritableDatabase();
                                ContentValues nuevo = new ContentValues();
                                nuevo.put("Id", id_remitente);
                                nuevo.put("Nombre", nombre);
                                nuevo.put("Token", token);
                                nuevo.put("Imagen", imagen);
                                bd.insert("Usuarios", null, nuevo);
                                gestorDB.guardarMensaje(id_remitente,mensaje, 0);

                                actualizarListaChats();
//                                Firebase.getFirebase().recibirMensajeFCM(mensaje,id_remitente,nombre,token,imagen);
                              /*  adaptador = new AdaptadorMensajes((Activity) getBaseContext(), mensajes,mios);
                                lista.setAdapter(adaptador);

                                lista.setSelection(adaptador.getCount() - 1);

                                actualizarListaMensajes();*/



                                //https://stackoverflow.com/questions/13854742/byte-array-of-image-into-imageview
//                                Bitmap bmp = BitmapFactory.decodeByteArray(imagenOtro, 0, imagenOtro.length);
//                                imagenOtroChat.setImageBitmap(Bitmap.createScaledBitmap(bmp, 150, 150, false));


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        //WorkManager.getInstance(getApplication().getBaseContext()).enqueue(requesContrasena);
        WorkManager.getInstance(getContext()).enqueueUniqueWork("getDatosUsuario"+id_remitente, ExistingWorkPolicy.REPLACE, requesContrasena);
    }

}