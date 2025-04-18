package com.example.albumappgroup5.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.activities.DatabaseHandler;
import com.example.albumappgroup5.models.AlbumModel;
import com.example.albumappgroup5.models.AlbumObject;
import com.example.albumappgroup5.models.ImageDetailsObject;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private AlbumModel albumList;
    private OnAlbumClickListener listener;
    private AlbumModel albumModel; // Thêm model chứa ảnh để truy cập danh sách ảnh của mỗi album
    private DatabaseHandler database;

    public interface OnAlbumClickListener {
        void onAlbumClick(String albumObject);
        void onAlbumLongClick(String albumObject);
    }

    public AlbumAdapter(AlbumModel albums, OnAlbumClickListener listener) {
        this.albumModel = albums;
        this.listener = listener;
    }

    // Setter để truyền vào model chứa ảnh
    public void setAlbumModel(AlbumModel albumModel) {
        this.albumModel = albumModel;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumObject albumObject = albumModel.getAlbumByPosition(position);
        holder.albumObject.setText(albumObject.getAlbumName());

        String selectedThumbnail = albumModel.getAlbumThumbnail(albumObject.getAlbumName());

        if (selectedThumbnail != null) {
            // Người dùng đã chọn thumbnail
            Glide.with(holder.itemView.getContext())
                    .load(selectedThumbnail)
                    .placeholder(R.drawable.default_album_cover)
                    .into(holder.albumThumbnail);
        } else {
            // Nếu chưa có thumbnail → chọn ảnh đầu tiên nếu có
            List<ImageDetailsObject> images = albumModel.getAlbumImages().get(albumObject.getAlbumName());
            Log.d("AlbumAdapter", "Images for album " + albumModel.getAlbumImages().get(albumObject.getAlbumName()) + ": " + images);
            if (images != null && !images.isEmpty()) {
                String imagePath = images.get(0).getImageID();
                Glide.with(holder.itemView.getContext())
                        .load(imagePath)
                        .placeholder(R.drawable.default_album_cover)
                        .into(holder.albumThumbnail);
            } else {
                holder.albumThumbnail.setImageResource(R.drawable.default_album_cover);
            }
        }


        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(albumObject.getAlbumName()));
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAlbumLongClick(albumObject.getAlbumName());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return albumModel.getAlbumList().size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView albumObject;
        ImageView albumThumbnail;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumObject = itemView.findViewById(R.id.albumName);
            albumThumbnail = itemView.findViewById(R.id.albumThumbnail); // ImageView để hiển thị thumbnail
        }
    }
}
