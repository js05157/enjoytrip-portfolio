package com.enjoytrip.attraction;

import java.util.List;

public class AttractionService {
  protected final TourClient source;

  public AttractionService(TourClient source) {
    this.source = source;
  }

  public TourClient.Result search(AttractionQuery query) {
    return source.search(query);
  }

  public Attraction detail(String id) {
    return source.detail(id);
  }

  public List<TourClient.Area> districts(String area) {
    return source.districts(area);
  }

  public java.util.Map<String, String> regions() {
    return source.regions();
  }

  public List<TourClient.Photo> photos(String id) {
    return source.photos(id);
  }

  public TourClient.Result recommendedPhotos(String area, int page) {
    return source.recommendedPhotos(area, page);
  }
}
