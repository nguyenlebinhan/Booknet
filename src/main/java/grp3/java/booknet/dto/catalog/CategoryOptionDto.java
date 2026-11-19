package grp3.java.booknet.dto.catalog;

import grp3.java.booknet.entity.book.Category;

public record CategoryOptionDto(
        Integer id,
        String name,
        String label,
        Integer parentId,
        String parentName,
        String parentLabel,
        boolean hasChildren) {

    public static CategoryOptionDto from(Category category) {
        return from(category, false);
    }

    public static CategoryOptionDto from(Category category, boolean hasChildren) {
        Category parent = category.getParent();
        String parentName = parent == null ? null : parent.getName();
        String label = parentName == null ? category.getName() : parentName + " / " + category.getName();
        return new CategoryOptionDto(
                category.getId(),
                category.getName(),
                label,
                parent == null ? null : parent.getId(),
                parentName,
                parentName,
                hasChildren);
    }
}
