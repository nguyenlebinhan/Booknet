package grp3.java.booknet.dto.catalog;

import java.util.ArrayList;
import java.util.List;

public class CatalogFilter {

    private String keyword;
    private List<Integer> categoryIds = new ArrayList<>();
    private List<String> availability = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private Integer publishYear;
    /** Publication year range (UC07). Either end may be empty. */
    private Integer yearFrom;
    private Integer yearTo;
    private String sort = "popular";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<Integer> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Integer> categoryIds) {
        this.categoryIds = categoryIds != null ? categoryIds : new ArrayList<>();
    }

    public List<String> getAvailability() {
        return availability;
    }

    public void setAvailability(List<String> availability) {
        this.availability = availability != null ? availability : new ArrayList<>();
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages != null ? languages : new ArrayList<>();
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
