package com.enjoytrip.attraction;
import com.enjoytrip.common.Web;
import jakarta.servlet.http.HttpServletRequest;
public record AttractionQuery(String area, String district, String type, String keyword, int page, int size, String sort) {
    public AttractionQuery {
        if (!area.matches("[0-9]{0,2}") || !district.matches("[0-9]{0,3}")) throw new IllegalArgumentException("지역 선택이 잘못되었습니다.");
        if (!district.isEmpty() && area.isEmpty()) throw new IllegalArgumentException("지역을 먼저 선택하세요.");
        if (!type.isEmpty() && !TourClient.TYPES.containsKey(type)) throw new IllegalArgumentException("콘텐츠 유형이 잘못되었습니다.");
        if (page < 1 || page > 1000 || size < 1 || size > 24 || keyword.length()>100 || !"ACD".contains(sort) || sort.length()!=1) throw new IllegalArgumentException("조회 조건이 잘못되었습니다.");
    }
    public static AttractionQuery of(HttpServletRequest req) {
        return new AttractionQuery(Web.text(req,"area",2),Web.text(req,"district",3),Web.text(req,"type",2),Web.text(req,"keyword",100),Web.number(req,"page",1,1,1000),12,req.getParameter("sort")==null?"A":Web.text(req,"sort",1));
    }
    public String link(int target) { return "?area="+area+"&district="+district+"&type="+type+"&keyword="+Web.encode(keyword)+"&sort="+sort+"&page="+target; }
    public String getPrevious() { return link(Math.max(1,page-1)); }
    public String getNext() { return link(page+1); }
}
