package cn.liuyang.desk;

import java.util.*;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledge;private final Actor actor;
    public KnowledgeController(KnowledgeService knowledge,Actor actor){this.knowledge=knowledge;this.actor=actor;}
    @GetMapping Object list(){return knowledge.list();}
    record Document(@NotBlank @Size(max=120)String title,@Size(max=40)String productId,@NotBlank @Size(max=30000)String content){}
    @PostMapping Object create(@Valid @RequestBody Document d){return knowledge.add(d.title(),d.productId(),d.content(),actor.id());}
    @PostMapping("/upload") Object upload(@RequestParam String title,@RequestParam(required=false)String productId,@RequestParam MultipartFile file)throws Exception{
        String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
        if(title.isBlank()||title.length()>120||!(name.endsWith(".txt")||name.endsWith(".md")))throw new ApiException(400,"请选择 UTF-8 编码的 .txt 或 .md 文件，并填写标题");
        String content;
        try{content=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString();}catch(CharacterCodingException e){throw new ApiException(400,"文件必须使用 UTF-8 编码");}
        return knowledge.add(title,productId,content,actor.id());
    }
    record Active(boolean active){}
    @PutMapping("/{id}") Object active(@PathVariable String id,@RequestBody Active r){return knowledge.setActive(id,r.active(),actor.id());}
    @PostMapping("/reindex") Object index(){return knowledge.index(null,actor.id());}
    @PostMapping("/{id}/reindex") Object indexOne(@PathVariable String id){return knowledge.index(id,actor.id());}
}
