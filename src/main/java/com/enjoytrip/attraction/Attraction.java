package com.enjoytrip.attraction;
public record Attraction(String contentId, String title, String address, String areaCode, String contentTypeId, String image, double latitude, double longitude, String overview) {
    public String typeName() { return getTypeName(); }
    public boolean hasCoordinates() { return getHasCoordinates(); }
    public String mapUrl() { return getMapUrl(); }
    public String getContentId() { return contentId; }
    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public String getImage() { return image; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getOverview() { return overview; }
    public String getTypeName() { return TourClient.TYPES.getOrDefault(contentTypeId, "여행지"); }
    public boolean getHasCoordinates() { return latitude != 0 && longitude != 0; }
    public String getMapUrl() {
        return "https://www.openstreetmap.org/export/embed.html?bbox=" + (longitude-.012) + "%2C" + (latitude-.008) + "%2C" + (longitude+.012) + "%2C" + (latitude+.008) + "&layer=mapnik&marker=" + latitude + "%2C" + longitude;
    }
}
