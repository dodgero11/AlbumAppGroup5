package com.example.albumappgroup5.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.activities.DatabaseHandler;
import com.example.albumappgroup5.models.ImageDetailsObject;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {
    private Context context;
    private List<ImageDetailsObject> imageList;
    private OnImageClickListener listener;

    // Listener to check for images clicks
    public interface OnImageClickListener {
        void onImageClick(int position);
        void onImageLongClick(int position); // dùng cho M4: Xóa ảnh
    }

    public GalleryAdapter(Context context, List<ImageDetailsObject> imageList, OnImageClickListener listener) {
        this.context = context;
        this.imageList = imageList;
        this.listener = listener;
    }

    // Returns a new view holder for each image
    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new GalleryViewHolder(view);
    }

    // Loading in images, add a placeholder for errors
    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {
        ImageDetailsObject image = imageList.get(position);
        // Check if image is password protected
        if (image.hasPassword() && image.isPasswordProtected()) {
            holder.imageView.setBackgroundColor(Color.BLACK);
            holder.imageView.setImageDrawable(null);
        } else {
            // Load actual image with Glide as usual
            Glide.with(context)
                    .load(image.getImageID())
                    .placeholder(R.mipmap.placeholder_image)
                    .into(holder.imageView);
        }

        // Set name and other data
        holder.imageView.setContentDescription(image.getImageName());
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // Class for each image view
    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView imageView;
        public GalleryViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.itemImageView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(listener != null){
                listener.onImageClick(getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if(listener != null){
                listener.onImageLongClick(getAdapterPosition());
            }
            return true;
        }
    }
}
