package com.example.albumappgroup5.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.albumappgroup5.R;
import com.example.albumappgroup5.models.AlbumModel;
import com.example.albumappgroup5.models.ImageModel;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<String> albumList;
    private OnAlbumClickListener listener;
    private AlbumModel albumModel; // Thêm model chứa ảnh để truy cập danh sách ảnh của mỗi album

    public interface OnAlbumClickListener {
        void onAlbumClick(String albumName);
        void onAlbumLongClick(String albumName);
    }

    public AlbumAdapter(List<String> albumList, OnAlbumClickListener listener) {
        this.albumList = albumList;
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
        String albumName = albumList.get(position);
        holder.albumName.setText(albumName);

        String selectedThumbnail = albumModel.getAlbumThumbnail(albumName);

        if (selectedThumbnail != null) {
            // Người dùng đã chọn thumbnail
            Glide.with(holder.itemView.getContext())
                    .load(selectedThumbnail)
                    .placeholder(R.drawable.default_album_cover)
                    .into(holder.albumThumbnail);
        } else {
            // Nếu chưa có thumbnail → chọn ảnh đầu tiên nếu có
            List<ImageModel> images = albumModel.getAlbumImages().get(albumName);
            if (images != null && !images.isEmpty()) {
                String imagePath = images.get(0).getImagePath();
                Glide.with(holder.itemView.getContext())
                        .load(imagePath)
                        .placeholder(R.drawable.default_album_cover)
                        .into(holder.albumThumbnail);
            } else {
                holder.albumThumbnail.setImageResource(R.drawable.default_album_cover);
            }
        }


        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(albumName));
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAlbumLongClick(albumName);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView albumName;
        ImageView albumThumbnail;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.albumName);
            albumThumbnail = itemView.findViewById(R.id.albumThumbnail); // ImageView để hiển thị thumbnail
        }
    }
}
