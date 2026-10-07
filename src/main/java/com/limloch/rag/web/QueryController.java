package com.limloch.rag.web;

import com.limloch.rag.query.QueryRequest;
import com.limloch.rag.query.QueryResponse;
import com.limloch.rag.query.QueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    public record QueryRequestDto(
            @NotBlank String question,
            Integer topK
    ) {}

    @PostMapping
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequestDto dto) {
        QueryResponse response = queryService.query(new QueryRequest(
                dto.question(),
                dto.topK()));
        return ResponseEntity.ok(response);
    }
}