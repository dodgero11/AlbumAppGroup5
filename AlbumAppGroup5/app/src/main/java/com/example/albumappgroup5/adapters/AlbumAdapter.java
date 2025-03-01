package com.example.albumappgroup5.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.albumappgroup5.R;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<String> albumList;
    private OnAlbumClickListener listener;

    // Listener to check for album clicks
    public interface OnAlbumClickListener {
        void onAlbumClick(String albumName);
    }

    public AlbumAdapter(List<String> albumList, OnAlbumClickListener listener) {
        this.albumList = albumList;
        this.listener = listener;
    }

    // Create item album for each album in the album section
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
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(albumName));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // Class for each album view in the album section
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView albumName;
        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
