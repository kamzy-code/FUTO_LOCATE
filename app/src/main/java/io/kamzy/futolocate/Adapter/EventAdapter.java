package io.kamzy.futolocate.Adapter;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.kamzy.futolocate.Dashboard;
import io.kamzy.futolocate.ExploreFragment;
import io.kamzy.futolocate.Models.Events;
import io.kamzy.futolocate.R;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewholder> {
    private List<Events> eventsList;
    private Context ctx;
    private FragmentManager fragmentManager;

    public EventAdapter(List<Events> eventsList, Context ctx, FragmentManager fragmentManager) {
        this.eventsList = eventsList;
        this.ctx = ctx;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public EventViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_item, parent, false);
        return new EventViewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewholder holder, int position) {
        Events events = eventsList.get(position);
        holder.eventName.setText(events.getName());
        holder.eventDescription.setText(events.getDescription());
        holder.eventLocation.setText(events.getLocation());

        holder.navigatebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ctx instanceof Dashboard) {
                    Dashboard activity = (Dashboard) ctx;
                  activity.navigateToExplore(events.getLatitude(), events.getLongitude(), events.getName(), events.getLocation());
                }

//                // Create a bundle with event details
//                Bundle bundle = new Bundle();
//                bundle.putDouble("latitude", events.getLatitude());
//                bundle.putDouble("longitude", events.getLongitude());
//                bundle.putString("eventName", events.getName());
//                bundle.putString("eventLocation", events.getLocation());
//
//                // Navigate to ExploreFragment
//                ExploreFragment exploreFragment = new ExploreFragment();
//                exploreFragment.setArguments(bundle);
//                fragmentManager.beginTransaction()
//                        .replace(R.id.fragment_container, exploreFragment) // Ensure correct container ID
//                        .addToBackStack(null)
//                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventsList.size();
    }

    static class EventViewholder extends RecyclerView.ViewHolder{
        TextView eventName, eventDescription, eventLocation;
        ImageButton navigatebutton;

        public EventViewholder(@NonNull View itemView) {
            super(itemView);

            eventName = itemView.findViewById(R.id.listEventName);
            eventDescription = itemView.findViewById(R.id.listEventdescription);
            eventLocation = itemView.findViewById(R.id.listEventLocation);
            navigatebutton = itemView.findViewById(R.id.listNavigateButton);

        }
    }
}
