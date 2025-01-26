package io.kamzy.futolocate.Tools;

import java.util.List;

import io.kamzy.futolocate.Models.Landmarks;

public interface LandmarkAPICallback <T>{
    void onSuccess(List<Landmarks> landmarksList);

    void onFailure(Throwable t);
}
