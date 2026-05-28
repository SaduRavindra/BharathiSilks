package com.bharathisilks.web;

import com.bharathisilks.service.CategoryService;
import com.bharathisilks.web.dto.CategoryRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<String> all() {
        return categories.names();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<String> add(@RequestBody CategoryRequest req) {
        categories.add(req.name());
        return categories.names();
    }

    @DeleteMapping("/{name}")
    public List<String> delete(@PathVariable String name) {
        categories.delete(name);
        return categories.names();
    }
}
