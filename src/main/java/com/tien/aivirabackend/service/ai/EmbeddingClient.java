package com.tien.aivirabackend.service.ai;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embedDocument(String title, String content);
    List<Double> embedQuery(String content);
    String provider();
    String model();
    int dimension();
    boolean configured();
}

