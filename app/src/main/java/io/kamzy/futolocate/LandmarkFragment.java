package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.prepGetRequestWithoutBody;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;

import io.kamzy.futolocate.Adapter.EventAdapter;
import io.kamzy.futolocate.Adapter.LandmarkAdapter;
import io.kamzy.futolocate.Models.Landmarks;
import io.kamzy.futolocate.Tools.GsonHelper;
import io.kamzy.futolocate.Tools.TokenSharedViewModel;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LandmarkFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LandmarkFragment extends Fragment {
    RecyclerView recyclerView;
    OkHttpClient client;
    List<Landmarks> landmarksList;
    LandmarkAdapter landmarkAdapter;
    TokenSharedViewModel tokenSharedViewModel;
    GsonHelper gsonHelper;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LandmarkFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LandmarkFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LandmarkFragment newInstance(String param1, String param2) {
        LandmarkFragment fragment = new LandmarkFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_landmark, container, false);
        recyclerView = view.findViewById(R.id.landmarkRecyclerView);
        gsonHelper = new GsonHelper();
        client = new OkHttpClient();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenSharedViewModel = new ViewModelProvider(requireActivity()).get(TokenSharedViewModel.class);
        tokenSharedViewModel.getData().observe(getViewLifecycleOwner(), value ->{
            try {
                getLandmarkList("api/landmark", value);
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void getLandmarkList (String endpoint, String authToken) throws IOException, JSONException {
        new Thread(()->{
            try(Response response = client.newCall(prepGetRequestWithoutBody(endpoint, authToken)).execute()){
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                    JSONArray responseBody = new JSONArray(response.body().string());
                    List<Landmarks> allLandmarks = gsonHelper.parseJSONArrayUsingGson(String.valueOf(responseBody));
                    getActivity().runOnUiThread(()->{
                        // Set up RecyclerView
                        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
                        recyclerView.setLayoutManager(layoutManager);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        landmarkAdapter = new LandmarkAdapter(allLandmarks, getContext(), getParentFragmentManager());
                        recyclerView.setAdapter(landmarkAdapter);
                        //       Add a DividerItemDecoration to control spacing
                        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
                        recyclerView.addItemDecoration(divider);
                    });
                }
            }catch (IOException | JSONException e){
                throw new RuntimeException(e);
            }
        }).start();
    }

}