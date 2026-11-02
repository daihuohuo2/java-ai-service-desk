package cn.liuyang.desk;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge/jobs")
public class IndexJobController {
    private final IndexJobService jobs;
    private final Actor actor;

    public IndexJobController(IndexJobService jobs, Actor actor) { this.jobs = jobs; this.actor = actor; }
    public record NewJob(@Size(max = 40) String documentId) { }

    @GetMapping public List<IndexJob> list() { return jobs.list(); }
    @GetMapping("/{id}") public IndexJob get(@PathVariable String id) { return jobs.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IndexJob submit(@RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody NewJob input) {
        return jobs.submit(input.documentId(), actor.id(), key);
    }

    @PostMapping("/{id}/retry")
    public IndexJob retry(@PathVariable String id) { return jobs.retry(id, actor.id()); }
}
