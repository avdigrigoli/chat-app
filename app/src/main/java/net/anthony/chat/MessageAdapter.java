package net.whispwriting.mantischat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private FirebaseUser currentUser;
    private Context context;
    private RecyclerView messageView;
    private int width, height;

    public MessageAdapter(List<Message> messageList, Context context, RecyclerView messageView,
                          int width, int height){
        this.messageList = messageList;
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.context = context;
        this.messageView = messageView;
        this.width = width;
        this.height = height;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_single_layout, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder viewHolder, final int i){
        final Message message = messageList.get(i);

        if (message.getFrom().equals(currentUser.getUid())){
            viewHolder.messageText.setVisibility(View.INVISIBLE);
            viewHolder.image.setVisibility(View.INVISIBLE);
            FirebaseFirestore.getInstance().collection("Users").document(message.getFrom())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Picasso.get().load(document.getString("image"))
                                    .networkPolicy(NetworkPolicy.OFFLINE)
                                    .placeholder(R.drawable.avatar)
                                    .into(viewHolder.image);
                        }
                    }
                }
            });
            viewHolder.receivedImage.setVisibility(View.INVISIBLE);
            if (message.getType().equals("image")){
                viewHolder.sentMessageText.setVisibility(View.INVISIBLE);
                Picasso.get().load(message.getMessage()).into(viewHolder.sentImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.get().load(message.getMessage()).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                int imageWidth = bitmap.getWidth();
                                int imageHeight = bitmap.getHeight();
                                if (imageHeight > 600){
                                    int resize = imageHeight / 600;
                                    imageHeight = imageHeight / resize;
                                    imageWidth = imageWidth / resize;
                                    Picasso.get().load(message.getMessage()).resize(imageWidth, imageHeight)
                                            .into(viewHolder.sentImage);
                                }
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
                viewHolder.sentImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri uri =  Uri.parse(message.getMessage());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String mime = "%/*";
                        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                        if (mimeTypeMap.hasExtension(
                                MimeTypeMap.getFileExtensionFromUrl(uri.toString())))
                            mime = mimeTypeMap.getMimeTypeFromExtension(
                                    MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                        intent.setDataAndType(uri,mime);
                        try {
                            context.startActivity(intent);
                        }catch(ActivityNotFoundException e){
                            Intent chooser = Intent.createChooser(intent, "Select Image Viewer");
                            try{
                                context.startActivity(chooser);
                            }catch(ActivityNotFoundException f) {
                                Toast.makeText(context, "No default image viewer found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }else {
                viewHolder.sentImage.setVisibility(View.INVISIBLE);
                viewHolder.sentMessageText.setText(message.getMessage());
            }
        }else{
            viewHolder.sentMessageText.setVisibility(View.INVISIBLE);
            viewHolder.sentImage.setVisibility(View.INVISIBLE);
            FirebaseFirestore.getInstance().collection("Users").document(message.getFrom())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Picasso.get().load(document.getString("image"))
                                    .networkPolicy(NetworkPolicy.OFFLINE)
                                    .placeholder(R.drawable.avatar)
                                    .into(viewHolder.image);
                        }
                    }
                }
            });
            if (message.getType().equals("image")){
                viewHolder.messageText.setVisibility(View.INVISIBLE);
                Picasso.get().load(message.getMessage()).into(viewHolder.receivedImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.get().load(message.getMessage()).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                int imageWidth = bitmap.getWidth();
                                int imageHeight = bitmap.getHeight();
                                if (imageHeight > 600){
                                    int resize = imageHeight / 600;
                                    imageHeight = imageHeight / resize;
                                    imageWidth = imageWidth / resize;
                                    Picasso.get().load(message.getMessage()).resize(imageWidth, imageHeight)
                                            .into(viewHolder.receivedImage);
                                }
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
                viewHolder.receivedImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri uri =  Uri.parse(message.getMessage());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String mime = "%/*";
                        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                        if (mimeTypeMap.hasExtension(
                                mimeTypeMap.getFileExtensionFromUrl(uri.toString())))
                            mime = mimeTypeMap.getMimeTypeFromExtension(
                                    mimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                        intent.setDataAndType(uri,mime);
                        try {
                            context.startActivity(intent);
                        }catch(ActivityNotFoundException e){
                            Intent chooser = Intent.createChooser(intent, "Select Image Viewer");
                            try{
                                context.startActivity(chooser);
                            }catch(ActivityNotFoundException f) {
                                Toast.makeText(context, "No default image viewer found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }else {
                viewHolder.receivedImage.setVisibility(View.INVISIBLE);
                viewHolder.messageText.setText(message.getMessage());
            }
        }
    }

    @Override
    public int getItemCount(){
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{

        private TextView messageText, sentMessageText;
        private ImageView receivedImage, sentImage;
        private CircleImageView image;

        public MessageViewHolder(View view){
            super(view);

            messageText = (TextView) view.findViewById(R.id.messageText);
            sentMessageText = (TextView) view.findViewById(R.id.sentMessageText);
            receivedImage = (ImageView) view.findViewById(R.id.receivedImage);
            sentImage = (ImageView) view.findViewById(R.id.sentImage);
            image = (CircleImageView) view.findViewById(R.id.profile_image_message);
            image.setMaxHeight(50);
        }
    }
}
