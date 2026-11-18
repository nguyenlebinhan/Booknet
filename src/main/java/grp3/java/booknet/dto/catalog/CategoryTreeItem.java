package grp3.java.booknet.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryTreeItem {

    private final String name;
    private final int depth;
}
