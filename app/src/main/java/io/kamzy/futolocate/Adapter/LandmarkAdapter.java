package io.kamzy.futolocate.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.kamzy.futolocate.Models.Landmarks;
import io.kamzy.futolocate.R;

public class LandmarkAdapter extends RecyclerView.Adapter<LandmarkAdapter.LandmarkviewHolder> {
    List<Landmarks> landmarksList;
    Context ctx;
    private FragmentManager fragmentManager;

    public LandmarkAdapter(List<Landmarks> landmarksList, Context ctx, FragmentManager fragmentManager) {
        this.landmarksList = landmarksList;
        this.ctx = ctx;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public LandmarkviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.landmark_list_item, parent, false);
        return  new LandmarkviewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LandmarkviewHolder holder, int position) {
        Landmarks landmarks = landmarksList.get(position);
        holder.landmarkName.setText(landmarks.getName());
        holder.landmarkDescription.setText(landmarks.getDescription());

        holder.landmarkLocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle the button click event here
            }
        });

    }

    @Override
    public int getItemCount() {
        return landmarksList.size();
    }

    static class LandmarkviewHolder extends RecyclerView.ViewHolder {
        TextView landmarkName, landmarkDescription;
        ImageButton landmarkLocateButton;
        public LandmarkviewHolder(@NonNull View itemView) {
            super(itemView);
            landmarkName = itemView.findViewById(R.id.listLandmarkName);
            landmarkLocateButton = itemView.findViewById(R.id.listSearchButton);
            landmarkDescription = itemView.findViewById(R.id.listLandmarkdescription);
        }
    }
}
