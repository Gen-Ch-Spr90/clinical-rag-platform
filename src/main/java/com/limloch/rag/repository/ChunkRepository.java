package com.limloch.rag.repository;

import com.limloch.rag.domain.Chunk;
import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ChunkRepository {

    private final JdbcTemplate jdbc;

    public ChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Payload for a chunk write. */
    public record ChunkRow(int chunkIndex, String content, int tokenCount, float[] embedding) {}

    public void batchInsert(UUID documentId, List<ChunkRow> rows) {
        String sql = """
                INSERT INTO chunks (document_id, chunk_index, content, token_count, embedding)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbc.batchUpdate(sql, rows, 100, (ps, row) -> {
            ps.setObject(1, documentId);
            ps.setInt(2, row.chunkIndex());
            ps.setString(3, row.content());
            ps.setInt(4, row.tokenCount());
            ps.setObject(5, new PGvector(row.embedding()));
        });
    }

    public List<Chunk> findByDocumentId(UUID documentId) {
        String sql = """
                SELECT id, document_id, chunk_index, content, token_count,
                       embedding::text AS embedding_text, created_at
                FROM chunks
                WHERE document_id = ?
                ORDER BY chunk_index
                """;
        return jdbc.query(sql, new ChunkRowMapper(), documentId);
    }

    public int countByDocumentId(UUID documentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM chunks WHERE document_id = ?",
                Integer.class, documentId);
        return count == null ? 0 : count;
    }

    private static class ChunkRowMapper implements RowMapper<Chunk> {
        @Override
        public Chunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Chunk(
                    rs.getLong("id"),
                    rs.getObject("document_id", UUID.class),
                    rs.getInt("chunk_index"),
                    rs.getString("content"),
                    rs.getInt("token_count"),
                    parseVector(rs.getString("embedding_text")),
                    rs.getObject("created_at", OffsetDateTime.class));
        }

        /** Parses the pgvector text form '[0.1,0.2,...]' into a float array. */
        private static float[] parseVector(String text) {
            if (text == null) return new float[0];
            String trimmed = text.substring(1, text.length() - 1);
            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }
    }
}