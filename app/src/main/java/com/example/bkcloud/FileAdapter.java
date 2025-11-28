package com.example.bkcloud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private List<FileItem> files;
    List<FileItem> originalList;

    public static class FileItem {
        public String name;
        public long size;
        public String folder;

        public FileItem(String name, long size, String folder) {
            this.name = name;
            this.size = size;
            this.folder = folder;
        }
    }


    public FileAdapter(List<FileItem> files) {
        this.files = files;
        this.originalList = new ArrayList<>(files);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtFileName, txtFileSize;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFileName = itemView.findViewById(R.id.txtFileName);
            txtFileSize = itemView.findViewById(R.id.txtFileSize);
        }
    }

    @NonNull
    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileAdapter.ViewHolder holder, int position) {
        FileItem file = files.get(position);
        holder.txtFileName.setText(file.name);
        holder.txtFileSize.setText(formatSize(file.size));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
    }

    public void filter(String key) {
        files.clear();

        if (key.isEmpty()) {
            files.addAll(originalList);
        } else {
            for (FileItem f : originalList) {
                String nameNorm = stripAccent(f.name).toLowerCase();
                if (nameNorm.contains(key)) {
                    files.add(f);
                }
            }
        }

        notifyDataSetChanged();
    }

    public List<String> getVisibleFolders() {
        List<String> result = new ArrayList<>();
        for (FileItem f : files) {
            if (!result.contains(f.folder)) {
                result.add(f.folder);
            }
        }
        return result;
    }

    private String stripAccent(String s) {
        if (s == null) return "";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public void setData(List<FileItem> newData) {
        files.clear();
        files.addAll(newData);
        notifyDataSetChanged();
    }


}
