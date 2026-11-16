package grp3.java.booknet.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Checks database foreign-key references without coupling the Book module to
 * Loan, Reservation, Fine, or Notification Java repositories/entities.
 * Owned book-copy and many-to-many association rows are removable with Book.
 */
@Component
public class BookReferenceGuard {
    private static final Set<String> BOOK_OWNED_CHILD_TABLES = Set.of(
            "book_copies", "book_authors", "book_categories");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public BookReferenceGuard(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public List<String> findBlockingReferences(Long bookId) {
        return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<List<String>>) connection -> {
            List<String> blockers = new ArrayList<>();
            String catalog = connection.getCatalog();
            String schema = currentSchema(connection);
            String booksTable = resolveTableName(connection.getMetaData(), catalog, schema, "books");
            if (booksTable == null) {
                throw new IllegalStateException("Cannot inspect foreign keys: books table was not found.");
            }

            for (ForeignKeyReference reference : exportedReferences(connection.getMetaData(), catalog, schema, booksTable)) {
                if (BOOK_OWNED_CHILD_TABLES.contains(reference.childTable().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (reference.parentColumn().equalsIgnoreCase("id")
                        && countReference(connection, reference, bookId) > 0) {
                    blockers.add(reference.childTable() + "." + reference.childColumn());
                }
            }

            String copiesTable = resolveTableName(connection.getMetaData(), catalog, schema, "book_copies");
            if (copiesTable != null) {
                String quotedCopies = quote(connection.getMetaData(), copiesTable);
                List<Long> copyIds = jdbcTemplate.queryForList(
                        "select id from " + quotedCopies + " where book_id = ?", Long.class, bookId);
                for (Long copyId : copyIds) {
                    for (ForeignKeyReference reference : exportedReferences(
                            connection.getMetaData(), catalog, schema, copiesTable)) {
                        if (reference.parentColumn().equalsIgnoreCase("id")
                                && countReference(connection, reference, copyId) > 0) {
                            blockers.add(reference.childTable() + "." + reference.childColumn()
                                    + " references copy id " + copyId);
                        }
                    }
                }
            }
            return blockers.stream().distinct().toList();
        });
    }

    private long countReference(Connection connection, ForeignKeyReference reference, Long id) throws SQLException {
        String sql = "select count(*) from " + quote(connection.getMetaData(), reference.childTable())
                + " where " + quote(connection.getMetaData(), reference.childColumn()) + " = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, id);
    }

    private List<ForeignKeyReference> exportedReferences(DatabaseMetaData metadata, String catalog,
                                                           String schema, String parentTable) throws SQLException {
        List<ForeignKeyReference> references = new ArrayList<>();
        try (ResultSet rs = metadata.getExportedKeys(catalog, schema, parentTable)) {
            while (rs.next()) {
                references.add(new ForeignKeyReference(
                        rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKCOLUMN_NAME")));
            }
        }
        return references;
    }

    private String resolveTableName(DatabaseMetaData metadata, String catalog, String schema, String requested) throws SQLException {
        try (ResultSet rs = metadata.getTables(catalog, schema, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                if (table.equalsIgnoreCase(requested)) return table;
            }
        }
        return null;
    }

    private String currentSchema(Connection connection) {
        try { return connection.getSchema(); }
        catch (SQLException ignored) { return null; }
    }

    private String quote(DatabaseMetaData metadata, String identifier) throws SQLException {
        String quote = metadata.getIdentifierQuoteString();
        if (quote == null || quote.isBlank()) return identifier;
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private record ForeignKeyReference(String childTable, String childColumn, String parentColumn) {}
}
